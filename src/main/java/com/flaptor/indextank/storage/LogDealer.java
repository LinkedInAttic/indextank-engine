package com.flaptor.indextank.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.indextank.util.IndexTankUtil;
import com.flaptor.util.Execute;
import com.google.common.collect.Maps;
import com.google.common.io.Files;


public class LogDealer extends Thread {

    private static final FormatLogger logger = new FormatLogger();
    private static final FormatLogger alertLogger = FormatLogger.getAlertsLogger();

    private LogRoot root;

    private int liveSegmentSize;
    private int indexSegmentSize;
    long nextTimestamp;
    boolean alreadyDealt;
    private final LogCleaner cleaner;

    /**
     * Constructor for tests only. Used to run cycles without actually starting the server.
     * @param cleaner 
     */
    LogDealer(LogCleaner cleaner, LogRoot root, int liveSegmentSize, int indexSegmentSize) {
        this.root = root;
        this.liveSegmentSize = liveSegmentSize;
        this.indexSegmentSize = indexSegmentSize;
        this.cleaner = cleaner;
    }
    
    LogDealer(LogCleaner cleaner) {
        this.root = new LogRoot(); 
        this.liveSegmentSize = RawLog.DEFAULT_SEGMENT_SIZE;
        this.indexSegmentSize = IndexLog.DEFAULT_SEGMENT_SIZE;
        this.cleaner = cleaner;
    }

    public void run() {
        root.getLiveLogPath().mkdirs();
        root.getPreviousPath().mkdirs();
        if (!root.getIndexesLogPath().exists()) {
            if (root.getMigratedIndexesLogPath().exists()) {
                // It looks like we died after moving the current version of indexes but
                // before replacing it by the last migration. Let's complete the process
                moveMigrableFiles(root.getMigratedPath(), root.getPath());
            }
        }
        try {
            if (!root.lockComponent("dealer")) {
                alertLogger.fatal("Unable to get lock, there's another dealer running for this log. Aborting process");
                System.exit(1);
            }
        } catch (Throwable t) {
            alertLogger.fatal(t, "Unable to get lock, there's another dealer running for this log. Aborting process");
            System.exit(1);
        }
        while (true) {
            try {
                runCycle();
            } catch (Exception e) {
                alertLogger.error(e, "Failed while running the dealer's cycle, waiting 1 second and trying again.");
                Execute.sleep(1000);
            }
        }
    }

    public void runCycle() throws IOException, TException, FileNotFoundException {
        if (IndexTankUtil.isMaster() && root.getSafeToReadFile().exists()) {
            nextTimestamp = getNextTimestamp();
            
            RawLog live = new RawLog(root, liveSegmentSize);
            
            boolean dealt = false;
            Segment current = null;
            for (Segment next : live.getLiveSegments()) {
                if (current != null) {
                    long newNextTimestamp = Math.max(nextTimestamp + 1, next.timestamp);
                    if (current.isLocked()) {
                        throw new RuntimeException("Ran into a locked segment. This shouldn't have happened.");
                    }
                    if (current.timestamp != nextTimestamp && alreadyDealt) {
                        throw new RuntimeException("Segment timestamp didn't match the expected. " + current.timestamp + " != " + nextTimestamp);
                    }
                    if (current.length() > 0) {
                        deal(current, newNextTimestamp);
                        dealt = true;
                    }
                    archive(current);
                    nextTimestamp = newNextTimestamp;
                    alreadyDealt = true;
                }
                current = next;
                root.saveInfo("dealer.next_timestamp", String.valueOf(nextTimestamp));
            }
            
            if (!dealt) {
                logger.debug("Nothing to deal. Waiting...");
                Execute.sleep(15000);
            }
        } else {
            if (root.getMigratedIndexesLogPath().exists()) {
                
                // delete previous files and move current files to previous
                logger.info("Found migrated data. Removing previous directory...");
                Files.deleteDirectoryContents(root.getPreviousPath().getCanonicalFile());
                logger.info("Moving current data to previous directory...");
                moveMigrableFiles(root.getPath(), root.getPreviousPath());

                logger.info("Moving migrated data to current directory...");
                moveMigrableFiles(root.getMigratedPath(), root.getPath());
                Files.deleteRecursively(root.getMigratedPath().getCanonicalFile());
            } else {
                logger.debug("Nothing new rsynced. Waiting...");
                Execute.sleep(30000);
                return;
            }
            long newNextTimestamp = getNextTimestamp();
            long masterDelta = Math.max(0, getMasterDelta() + 5000);
            logger.info("Timestamp has been updated from %d to %d. Master delta is now %d", nextTimestamp, newNextTimestamp, masterDelta);
            nextTimestamp = newNextTimestamp;
            
            RawLog live = new RawLog(root, liveSegmentSize);
            Segment lastSegment = null;
            for (Segment segment : live.getLiveSegments()) {
                if (segment.timestamp < newNextTimestamp - masterDelta) {
                    if (lastSegment != null) {
                        logger.debug("Removing segment %s", lastSegment);
                        lastSegment.delete();
                    }
                    lastSegment = segment;
                } else {
                    break;
                }
            }
            if (lastSegment != null) {
                if (root.getSafeToReadFile().createNewFile()) {
                    logger.info("It is now safe to use this dealer to read.");
                }
            }
        }
    }

    private void moveMigrableFiles(File from, File to) {
        for (File file : from.listFiles()) {
            boolean isSafeFlag = file.getName().equals("safe_to_read");
            boolean isMasterDelta = file.getName().equals("master_delta.info");
            boolean isIndexes = file.getName().equals("indexes");
            boolean isDealerInfo = file.getName().startsWith("dealer") && file.getName().endsWith(".info");
            if (isSafeFlag || isMasterDelta || isIndexes || isDealerInfo) {
                file.renameTo(new File(to, file.getName()));
            }
        }
    }

    private void archive(Segment current) {
        current.archive(root.getHistoryLogPath());
    }

    public long getLastDealtId() throws IOException {
        String lastIdInfo = root.loadInfo("dealer.last_id");
        long lastId = lastIdInfo == null ? 0L : Long.parseLong(lastIdInfo);
        return lastId;
    }
    public long getNextTimestamp() throws IOException {
        String lastIdInfo = root.loadInfo("dealer.next_timestamp");
        long lastId = lastIdInfo == null ? 0L : Long.parseLong(lastIdInfo);
        return lastId;
    }
    public long getMasterDelta() throws IOException {
        String masterDeltaInfo = root.loadInfo("master_delta");
        long masterDelta = masterDeltaInfo == null ? 0L : Long.parseLong(masterDeltaInfo) * 1000L;
        return masterDelta;
    }

    public void deal(Segment current, long newNextTimestamp) throws TException, IOException {
        long t = System.currentTimeMillis();
        Map<String, MemoryBuffer> m = Maps.newHashMap();
        for (LogRecord record : current.reader()) {
            if (record.is_set_docid()) {
                MemoryBuffer p = m.get(record.get_index_code());
                if (p == null) {
                    p = new MemoryBuffer();
                    m.put(record.get_index_code(), p);
                }
                record.set_index_code(null);
                record.write(p.protocol);
            } else {
                String message = "Trying to append a record with no docid index:" + record.get_index_code() + " | " + record.get_fields() + " | " + record.get_variables() + " | " + record.get_variables();
                // no docid found 
                if (record.is_set_fields() || record.is_set_variables() || record.is_set_categories()) {
                    throw new RuntimeException(message);
                } else {
                    alertLogger.warn(message);
                }
            }
        }
        for (Entry<String, MemoryBuffer> entry : m.entrySet()) {
            cleaner.lockIndexesForRead();
            try {
                IndexLog index = new IndexLog(entry.getKey(), root, indexSegmentSize);
                index.appendBuffer(current.timestamp, entry.getValue());
            } catch (RuntimeException e) {
                logger.error(e, "Failed to deal segment %s, when appending data for index %s", current, entry.getKey());
                throw e;
            } finally {
                cleaner.unlockIndexesForRead();
            }
        }
        logger.info("Dealt segment %s in %.2fs (%d indexes affected)", current, (System.currentTimeMillis() - t) / 1000.0, m.size());
    }

    public void sortNow(String indexCode) throws TException, IOException {
        long tlock = System.currentTimeMillis();
        synchronized (this) {
            cleaner.lockIndexesForRead();
            try {
                long t = System.currentTimeMillis();
                new IndexLog(indexCode, root, indexSegmentSize).sortNow();
                logger.info("Forced closing of %s in %.2fs (locked for %.2fs)", indexCode, (System.currentTimeMillis() - t) / 1000.0, (t - tlock) / 1000.0);
            } finally {
                cleaner.unlockIndexesForRead();
            }
        }
    }

}
