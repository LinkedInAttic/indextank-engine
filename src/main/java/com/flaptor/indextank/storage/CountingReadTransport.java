/**
 * 
 */
package com.flaptor.indextank.storage;

import java.io.InputStream;

import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransportException;

class CountingReadTransport extends TIOStreamTransport {
    long bytesRead = 0;
    public CountingReadTransport(InputStream is) {
        super(is);
    }
    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
        int r = super.read(buf, off, len);
        bytesRead += r;
        return r;
    }
    public long getBytesRead() {
        return bytesRead;
    }
}