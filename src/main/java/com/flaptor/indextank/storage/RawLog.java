package com.flaptor.indextank.storage;

import java.io.IOException;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.storage.Segment.MissingSegmentException;
import com.flaptor.indextank.util.FormatLogger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RawLog {

    private static final FormatLogger logger = new FormatLogger();

    public static final int DEFAULT_SEGMENT_SIZE = 1024 * 1024 * 50;

    private SegmentWriter writingHead;

    private LogRoot root;

    private final int segmentSize;
    private int records;
    private int segmentRecords;

    private long lastTime;
    
    public RawLog() {
        this(new LogRoot(), DEFAULT_SEGMENT_SIZE);
    }
    
    public RawLog(LogRoot root, int segmentSize) {
        this.root = root;
        this.segmentSize = segmentSize;
        this.root.getLiveLogPath().mkdirs();
        this.root.getHistoryLogPath().mkdirs();
    }

    public synchronized void write(LogRecord record) throws TException, IOException {
        Preconditions.checkState(!record.is_set_id(), "Can't insert records to the live log with an id");
        Preconditions.checkState(record.is_set_index_code(), "Can't insert records to the live log without defining the index code");
        if (writingHead == null) {
            writingHead = Segment.createRawHead(root, root.getLiveLogPath(), System.currentTimeMillis());
        }
        record.set_timestamp_ms(System.currentTimeMillis());
        writingHead.write(record);
        records++;
        segmentRecords++;

        if (System.currentTimeMillis() - lastTime > 5000) {
            logger.debug("%d records so far (current segment: %d, bytes: %d / %d)", records, segmentRecords, writingHead.length(), segmentSize);
            lastTime = System.currentTimeMillis();
            
        }
        
        if (writingHead.length() > segmentSize) {
            logger.info("Closing a segment with %d records", segmentRecords);
            writingHead.release();
            writingHead = null;
            segmentRecords = 0;
        }
    }

    public synchronized void flush() throws TTransportException {
        if (writingHead != null) {
            writingHead.flush();
        }
    }

    public List<Segment> getLiveSegments() {
        List<Segment> live = Segment.getSegments(root, root.getLiveLogPath());
        for (Segment s : live) {
            s.addAlternativeParent(root.getHistoryLogPath());
        }
        return live;
    }
    
    public List<Segment> getAllSegments() {
        List<Segment> live = Segment.getSegments(root, root.getLiveLogPath());
        List<Segment> history = Segment.getSegments(root, root.getHistoryLogPath());
        for (Segment s : live) {
            s.addAlternativeParent(root.getHistoryLogPath());
        }
        return Lists.newArrayList(Iterables.concat(history, live));
    }

    public Segment getSegment(long timestamp) {
        Segment segment = Segment.getSegment(root, root.getLiveLogPath(), timestamp, false);
        if (segment == null) {
            segment = Segment.getSegment(root, root.getHistoryLogPath(), timestamp, false);
        } else {
            segment.addAlternativeParent(root.getHistoryLogPath());
        }
        return segment;
    }

    public Segment getFirstSegment() {
        Segment segment = Iterables.get(Segment.iterateSegments(root, root.getHistoryLogPath()), 0, null);
        if (segment == null) {
            segment = Iterables.get(Segment.iterateSegments(root, root.getLiveLogPath()), 0, null);
        }
        return segment;
    }
    
    public Segment findFollowingSegment(long timestamp) throws MissingSegmentException {
        Segment segment = Segment.findNextSegment(root, timestamp, root.getHistoryLogPath(), root.getLiveLogPath());
        if (segment != null && segment.parent.equals(root.getLiveLogPath())) {
            segment.addAlternativeParent(root.getHistoryLogPath());
        }
        return segment;
    }

    
}
