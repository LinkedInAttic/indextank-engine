/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
