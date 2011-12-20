/**
 * 
 */
package com.flaptor.indextank.storage;

import java.io.OutputStream;

import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransportException;

class CountingWriteTransport extends TIOStreamTransport {
    long bytesWritten = 0;
    public CountingWriteTransport(OutputStream os) {
        super(os);
    }
    public CountingWriteTransport(OutputStream os, long length) {
        super(os);
        this.bytesWritten = length;
    }
    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
        super.write(buf, off, len);
        bytesWritten += (len - off);
    }
    public long getBytesWritten() {
        return bytesWritten;
    }
}