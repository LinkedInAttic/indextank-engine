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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogBatch;
import com.flaptor.indextank.rpc.LogPage;
import com.flaptor.indextank.rpc.LogPageToken;
import com.flaptor.indextank.rpc.LogReader;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.LogReader.Client;
import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

public class LogStorageIndexReader implements Iterable<LogRecord> {
    
    private static final LogBatch END_BATCH = new LogBatch(ImmutableList.<LogRecord>of());
    
    private static FormatLogger logger = new FormatLogger();
    
    private final String readerHost;
    private final int readerPort;
    private final int pagesBuffer;
    private final String indexCode;

    public LogStorageIndexReader(String readerHost, int readerPort, int pagesBuffer, String indexCode) {
        Preconditions.checkArgument(pagesBuffer > 0, "At least one page buffer required");
        this.readerHost = readerHost;
        this.readerPort = readerPort;
        this.pagesBuffer = pagesBuffer;
        this.indexCode = indexCode;
    }
    
    
    @Override
    public Iterator<LogRecord> iterator() {
        return new AbstractIterator<LogRecord>() {
            private TSocket transport;
            private TProtocol protocol;
            private Client client;
            private BlockingQueue<LogBatch> buffer = new LinkedBlockingQueue<LogBatch>(pagesBuffer);
            private boolean failed;
            private boolean finished;
            private List<LogRecord> current;
            private int currentPosition;
            
            private void reloadClient() {
                transport = new TSocket(readerHost, readerPort);
                protocol = new TBinaryProtocol(transport);
                client = new LogReader.Client(protocol);
                try {
                    transport.open();
                } catch (TTransportException e) {
                }
            }
            
            { 
                reloadClient(); 
                new Thread() {
                    private LogPageToken token = new LogPageToken();
                    
                    public void run() {
                        try {
                            while (true) {
                                LogPage page;
                                int retries = 0;
                                while (true) {
                                    try {
                                        page = client.read_page(indexCode, token);
                                        break;
                                    } catch (Throwable t) {
                                        if (retries++ >= 30) throw t;
                                        int wait = (int) Math.min(60, Math.pow(3, retries-1));
                                        logger.warn(t, "Failed to read a page for index %s. Retry #%d. Waiting %d seconds", indexCode, retries, wait);
                                        Execute.close(transport);
                                        reloadClient();
                                        Execute.sleep(1000 * wait);
                                    }
                                }
                                if (page.is_set_batch()) {
                                    addBatch(page.get_batch());
                                }
                                if (page.is_set_next_page_token()) {
                                    token = page.get_next_page_token();
                                    continue;
                                } else {
                                    addBatch(END_BATCH);
                                    break;
                                }
                            }
                        } catch (Throwable t) {
                            logger.fatal(t, "Aborting index read for code %s", indexCode);
                            failed = true;
                            addBatch(END_BATCH);
                        }
                        Execute.close(transport);
                    }

                }.start();
            }
            
            private void addBatch(LogBatch batch) {
                while (true) {
                    try {
                        buffer.put(batch);
                    } catch (InterruptedException e) {
                        continue;
                    }
                    break;
                }
            }
            
            @Override
            protected LogRecord computeNext() {
                while (!failed && !finished && (current == null || currentPosition >= current.size())) {
                    try {
                        LogBatch batch = buffer.take();
                        if (batch == END_BATCH) {
                            finished = true;
                            break;
                        }
                        current = batch.get_records();
                        currentPosition = 0;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                if (failed) {
                    throw new RuntimeException("Failed to read index " + indexCode);
                }
                if (finished) {
                    return endOfData();
                }
                return current.get(currentPosition++);
            }
        };
    }
    
    public static void main(String[] args) {
        LogStorageIndexReader reader = new LogStorageIndexReader(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        String format = "id:%d - ts:%d - docid:%s - delete:%s - fields:%s - fields[timestamp]:%s";
        for (LogRecord record : reader) {
            String line = String.format(
                    format, 
                    record.get_id(), 
                    record.get_timestamp_ms(), 
                    record.get_docid(), 
                    record.is_deleted(), 
                    record.is_set_fields(), 
                    record.is_set_fields() ? record.get_fields().get("timestamp") : null);
            System.out.println(line);
        }
    }

}
