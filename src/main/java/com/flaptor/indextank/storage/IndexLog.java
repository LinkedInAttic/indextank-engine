package com.flaptor.indextank.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.util.FormatLogger;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

public class IndexLog {

    private static final FormatLogger alertLogger = FormatLogger.getAlertsLogger();
    private static final FormatLogger logger = new FormatLogger();
    
    public static final int DEFAULT_SEGMENT_SIZE = 30 * 1024 * 1024;

    String code;
    
    private final int segmentSize;
    private final LogRoot root;

    
    public IndexLog(String code) throws FileNotFoundException {
        this(code, new LogRoot(), DEFAULT_SEGMENT_SIZE);
    }

    public IndexLog(String code, LogRoot root, int segmentSize) {
        this.root = root;
        this.code = code;
        this.segmentSize = segmentSize;
        this.getOptimizedPath().mkdirs();
        this.getSegmentsPath().mkdirs();
    }

    public List<Segment> getSegments() {
        return Segment.getSegments(root, getSegmentsPath());
    }
    
    public List<Segment> getSortedSegments() {
        return Segment.getSegments(root, getSegmentsPath(), true);
    }

    public synchronized void appendBuffer(long initialTimestamp, MemoryBuffer buffer) throws TException, IOException {
        // filter out empty records but add a warning to the alert log
        Predicate<LogRecord> isNonEmpty = new Predicate<LogRecord>() {
            public boolean apply(LogRecord r) {
                if (!isValidRecord(r)) {
                    throw new RuntimeException(String.format("At least one of the records was missing a docid: code=%s | fields: %s | vars: %s | categories: %s", r.get_index_code(), r.get_fields(), r.get_variables(), r.get_categories()));
                }
                if (!r.is_set_docid()) {
                    alertLogger.warn("Tried to append an empty record for index %s", r.get_index_code());
                    return false;
                }
                return true;
            }
        };
        
        // write the actual segment
        Segment.createUnsortedSegment(root, getSegmentsPath(), initialTimestamp, Iterators.filter(new RecordIterator(null, buffer.protocol, "Buffer for " + this.code + " at " + initialTimestamp), isNonEmpty));
        
        sort(false);
    }
    
    public synchronized void sortNow() throws TException, IOException {
        sort(true);
    }
    
    synchronized void sort(boolean now) throws TException, IOException {
        List<Segment> unsortedSegments = Segment.getSegments(root, getSegmentsPath(), false);
        if (!unsortedSegments.isEmpty()) {
            now = now || unsortedSegments.size() > 30;
            now = now || totalSize(unsortedSegments) > segmentSize;
            
            if (now) {
                // we should sort now
                Segment sorted = Segment.createSortedSegment(root, getSegmentsPath(), unsortedSegments.get(0).timestamp, unsortedSegments);
                
                // and now delete the replaced segments
                for (Segment segment : unsortedSegments) {
                    segment.delete();
                }
                
                logger.info("Sorted %d unsorted segments into %s", unsortedSegments.size(), sorted);
            }
        }
    }

    private int totalSize(List<Segment> unsortedSegments) {
        int totalSize = 0;
        for (Segment segment : unsortedSegments) {
            totalSize += segment.length();
        }
        return totalSize;
    }

    public Segment getLargestOptimizedSegment() {
        return Iterables.getLast(Segment.iterateSegments(root, getOptimizedPath(), true), null);
    }

    public List<Segment> getOptimizedSegments() {
        return Segment.getSegments(root, getOptimizedPath());
    }
    
    public Segment getOptimizedSegment(long timestamp) {
        return Segment.getSegment(root, getOptimizedPath(), timestamp, true);
    }

    public File getSegmentsPath() {
        return new File(root.getIndexLogPath(code), "segments");
    }
    public File getOptimizedPath() {
        return new File(root.getIndexLogPath(code), "optimized");
    }

    public Segment getSortedSegment(long timestamp) {
        return Segment.getSegment(root, getSegmentsPath(), timestamp, true);
    }
    public Segment getSegment(long timestamp) {
        return Segment.getSegment(root, getSegmentsPath(), timestamp, null);
    }
    
    public Segment getFirstSortedSegment() {
        return Iterables.get(Segment.iterateSegments(root, getSegmentsPath(), true), 0, null);
    }
    public Segment getFirstSegment() {
        return Iterables.get(Segment.iterateSegments(root, getSegmentsPath()), 0, null);
    }
    public Segment getLastSegment() {
        return Iterables.getLast(Segment.iterateSegments(root, getSegmentsPath()), null);
    }

    public static boolean isLog(File file) {
        return new File(file, "segments").exists();
    }

    public void markReadNow() {
        try {
            File file = getLastReadFile();
            if (!file.createNewFile()) {
                file.setLastModified(System.currentTimeMillis());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public long getLastRead() {
        return getLastReadFile().lastModified();
    }

    private File getLastReadFile() {
        return new File(root.getIndexLogPath(code), "last_read");
    }
    
    public boolean isValidRecord(LogRecord r) {   
        return r.is_set_docid() | !(r.is_set_fields() && r.is_set_variables() && r.is_set_categories());
    }
       
}

