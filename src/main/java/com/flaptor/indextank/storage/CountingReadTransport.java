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
