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

import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogBatch;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.LogWriter;
import com.google.common.collect.ImmutableMap;

public class LogWriterClient {

    private final String host;
    private final int port;

    public LogWriterClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void sendBatch(LogBatch batch) {
        TSocket transport = new TSocket(host, port);
        TProtocol protocol = new TBinaryProtocol(transport);
        LogWriter.Client client = new LogWriter.Client(protocol);
        
        try {
            transport.open();
            client.send_batch(batch);
            transport.close();
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        LogWriterClient client = new LogWriterClient("localhost", 15000);
        
        Scanner in = new Scanner(System.in);
        while (true) {
            String line = in.nextLine();
            if (line.startsWith("i")) {
                IndexLog log = new IndexLog(line.substring(1));
                Segment segment = log.getLargestOptimizedSegment();
                String docid = null;
                /*do {
                    Pair<SegmentReader, String> page = segment.pageReader(docid);
                    docid = page.last();
                    List<LogRecord> list = Lists.newArrayList(page.first());
                    System.out.println(list.size());
                    System.out.println(list.get(0));
                } while (docid != null);*/
                /*SegmentIndexReader r = new SegmentIndexReader(log.getLargestOptimizedSegment());
                for (Pair<Long, String> pair : r) {
                    System.out.println(pair);
                }*/
            /*} else if (line.startsWith("?")) {
                long t = System.currentTimeMillis();
                LogPageToken token = new LogPageToken();
                int total = 0;
                while (token != null) {
                    System.out.println(token);
                    LogPage page = client.readPage(line.substring(1), token);
                    int size = page.get_batch().get_records_size();
                    if (size > 0) {
                        LogRecord first = page.get_batch().get_records().get(0);
                        LogRecord last = page.get_batch().get_records().get(size-1);
                        System.out.println(String.format("> %d ---------> %d - %d --------> %s - %s", size, first.get_id(), last.get_id(), first.get_docid(), last.get_docid()));
                    }
                    if (page.is_set_next_page_token()) {
                        token = page.get_next_page_token();
                    } else {
                        token = null;
                    }
                    total += size;
                }
                System.out.println("Total: " + total);
                System.out.println("Execution time = " + (System.currentTimeMillis() - t) / 1000.0);*/
            } else {
                String[] split = line.split(" ", 4);
                long start = Long.parseLong(split[0]);
                int count = Integer.parseInt(split[1]);
                String code = split[2];
                String text = null;
                if (split.length > 3) {
                    text = split[3];
                }
                LogBatch b = new LogBatch();
                for (long id = start; id - start < count; id++) {
                    if (((id - start) % 1000) == 0 && id != start) {
                        System.out.println("sending " + id);
                        client.sendBatch(b);
                        b.get_records().clear();
                    }
                    LogRecord record = new LogRecord();
                    record.set_docid("d" + id);
                    record.set_timestamp_ms(System.currentTimeMillis());
                    record.set_index_code(code);
                    if (text != null) {
                        record.set_fields(ImmutableMap.of("text", text));
                    } else {
                        record.set_deleted(true);
                    }
                    b.add_to_records(record);
                }
                if (!b.get_records().isEmpty())
                    client.sendBatch(b);
            }
        }
    }
    
}
