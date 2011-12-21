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

package com.flaptor.indextank.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogRecord;
import com.google.common.collect.AbstractIterator;

public final class RecordIterator extends AbstractIterator<LogRecord> {

    private final TProtocol protocol;
    private final CountingReadTransport transport;
    private long safelyRead = 0;
    private long totalRead = 0;
    private long end = -1;
    private final String description;

    RecordIterator(CountingReadTransport transport, TProtocol protocol, String description) {
        this.transport = transport;
        this.protocol = protocol;
        this.description = description;
    }

    public RecordIterator(CountingReadTransport transport, TProtocol protocol, long end, String description) {
        this(transport, protocol, description);
        this.end = end;
    }

    @Override
    protected LogRecord computeNext() {
        if (end >= 0 && transport.getBytesRead() >= end) {
            if (transport != null) {
                totalRead = transport.getBytesRead();
                transport.close();
            }
            return endOfData();
        }
        LogRecord record = new LogRecord();
        try {
            ((TBinaryProtocol)protocol).setReadLength(10000000);
            record.read(protocol);
            if (transport != null) {
                safelyRead = transport.getBytesRead();
            }
        } catch (TTransportException e) {
            switch (e.getType()) {
                case TTransportException.END_OF_FILE:
                    if (transport != null) {
                        totalRead = transport.getBytesRead();
                        transport.close();
                    }
                    return endOfData();
                case TTransportException.UNKNOWN:
                    if (e.getMessage().startsWith("Cannot read. Remote side has closed")) {
                        if (transport != null) {
                            totalRead = transport.getBytesRead();
                            transport.close();
                        }
                        return endOfData();
                    }
                default:
                    transport.close();
                    throw new RuntimeException("Failed while iterating: " + description, e);
            }
        } catch (TException e) {
            transport.close();
            throw new RuntimeException("Failed while iterating: " + description, e);
        }
        return record;
    }
    
    public long getSafelyRead() {
        return safelyRead;
    }
    
    public long getTotalRead() {
        return totalRead;
    }
    
    public static RecordIterator forFiles(File... files) throws IOException {
        return forFiles(8192, files);
    }
    public static RecordIterator forFiles(int bufferingSize, File... files) throws IOException {
        return forFiles(-1, -1, bufferingSize, files);
    }
    public static RecordIterator forFiles(long start, long end, int bufferingSize, File... files) throws IOException {
        FileInputStream in; 
        int i = 0;
        long fileLength = 0;
        while (true) {
            try {
                fileLength = files[i].length();
                in = new FileInputStream(files[i++]);
                break;
            } catch (FileNotFoundException e) {
                if (i == files.length) {
                    throw e;
                }
            }
        }
        InputStream is = start >= 0 ? Channels.newInputStream(in.getChannel().position(start)) : in;
        BufferedInputStream bis = new BufferedInputStream(is, bufferingSize);
        CountingReadTransport transport = new CountingReadTransport(bis);
        TProtocol protocol = new TBinaryProtocol.Factory().getProtocol(transport);
        /*if (fileLength < (long)Integer.MAX_VALUE) {
            ((TBinaryProtocol)protocol).setReadLength((int) fileLength);
        }*/
        long length = -1;
        if (end >= 0) {
            if (start >= 0) {
                length = end - start;
            } else {
                length = end;
            }
        }
        return new RecordIterator(transport, protocol, length, files[i-1].getAbsolutePath());
    }
    
    public String getDescription() {
        return description;
    }
    
}
