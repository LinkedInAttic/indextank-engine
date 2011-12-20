package com.flaptor.indextank.storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogRecord;
import com.google.common.base.Preconditions;

public class SegmentWriter {
    
    File file;
    CountingWriteTransport transport;
    TProtocol protocol;
    long baseLength;
    private final boolean append;
    private long initialPosition = -1;
    
    SegmentWriter(File file) {
        this(file, true);
    }
    SegmentWriter(File file, boolean append) {
        this.file = file;
        this.append = append;
        this.baseLength = append ? file.length() : 0;
    }
    
    void setPosition(long position) {
        Preconditions.checkState(append);
        Preconditions.checkState(transport == null);
        this.initialPosition = position;
        this.baseLength = position;
    }
    
    long length() {
        long length = baseLength;
        if (transport != null) {
            length += transport.getBytesWritten();
        }
        return length;
    }
    
    void write(LogRecord record) throws TException, IOException {
        record.write(getProtocol());
    }    

    TProtocol getProtocol() throws IOException {
        if (protocol == null) {
            if (append && initialPosition >= 0) {
                RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                raf.setLength(initialPosition);
                raf.close();
            }
            
            FileOutputStream out = new FileOutputStream(file, append);
            out.getChannel().lock();
            transport = new CountingWriteTransport(new BufferedOutputStream(out));
            protocol = new TBinaryProtocol.Factory().getProtocol(transport);
        }
        return protocol;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    void release() throws IOException {
        getProtocol();
        transport.close();
    }
    public void flush() throws TTransportException {
        if (transport != null) {
            transport.flush();
        }
    }
    
    /*public static void main(String[] args) throws TException, IOException {
        File file = new File("/tmp/000003_1-inf");
        Segment segment = new Segment(file);
        SegmentReader r = segment.reader();
        RecordIterator iterator = r.iterator();
        while (iterator.hasNext()) {
            LogRecord l = (LogRecord) iterator.next();
            System.out.println(l);
        }
        System.out.println(iterator.getSafelyRead() + " " + iterator.getTotalRead());  
        segment.prepareForWriting();
        segment.write(new LogRecord(segment.lastId+1, 2L, "A", "B", false, ImmutableMap.of("x","y"), ImmutableMap.of(1, 2.0), ImmutableMap.<String,String>of()));
        segment.release();
    }*/

}
