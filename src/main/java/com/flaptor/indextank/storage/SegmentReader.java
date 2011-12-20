package com.flaptor.indextank.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.util.FormatLogger;

public class SegmentReader implements Iterable<LogRecord> {

    private static final FormatLogger alertLogger = FormatLogger.getAlertsLogger();
    
    private Segment segment;
    private int bufferingSize;
    private long start = -1;
    private long end = -1;

    SegmentReader(Segment segment) {
        this(segment, 64 * 1024);
    }
    SegmentReader(Segment segment, int bufferingSize) {
        this.segment = segment;
        this.bufferingSize = bufferingSize;
    }
    SegmentReader(Segment segment, long start, long end) {
        this(segment);
        this.start = start;
        this.end = end;
    }

    @Override
    public RecordIterator iterator() {
        try {
            return RecordIterator.forFiles(start, end, bufferingSize, segment.buildFileAlternatives());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println(arg);
            Segment s = new Segment(new LogRoot(), new File(arg));
            long count = 0;
            long max_ts = 0;
            for (LogRecord r : s.reader()) {
                max_ts = Math.max(max_ts, r.get_timestamp_ms());
                if (!r.is_set_docid()) count++;
            }
            if (count > 0) {
                alertLogger.warn("=============================================> " + count + " : " + max_ts);
                System.out.println("=============================================> " + count + " : " + max_ts);
            }
        }
    }
    
}
