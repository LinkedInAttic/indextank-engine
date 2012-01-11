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

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.rpc.LogBatch;
import com.flaptor.indextank.rpc.LogPage;
import com.flaptor.indextank.rpc.LogPageToken;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.storage.IndexesLogServer;
import com.flaptor.indextank.storage.LogRoot;
import com.flaptor.indextank.storage.RecordMerger;
import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.flaptor.util.FileUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class BaseLogStorageTest extends IndexTankTestCase {

    protected File dir;
    protected LogRoot root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dir = FileUtil.createTempDir("LOGTEST", ".log");
        root = new LogRoot(dir, LogRoot.DEFAULT_READING_PAGE_SIZE);
    }

    protected Map<String, LogRecord> readFully(IndexesLogServer storage, String code) throws TException {
        LogPageToken token = new LogPageToken();
        List<LogRecord> records = Lists.newArrayList();
        while (token != null) {
            LogPage page = storage.read_page(code, token);
            records.addAll(page.get_batch().get_records());
            if (page.is_set_next_page_token()) {
                token = page.get_next_page_token();
            } else {
                token = null;
            }
        }
        records = RecordMerger.compactAndSort(records.iterator());
        Map<String, LogRecord> map = Maps.newHashMap();
        Iterator<LogRecord> merged = RecordMerger.merge(ImmutableList.of(new PeekingIterator<LogRecord>(records.iterator())));
        while (merged.hasNext()) {
            LogRecord r = merged.next();
            map.put(r.get_docid(), r);
        }
        return map;
    }

    /*private void assertSegmentInfo(Segment segment, long start, Long end, boolean sorted) {
        assertEquals("Segment sorted didn't match", sorted, segment.sorted);
        assertEquals("Segment start didn't match", start, segment.start);
        assertEquals("Segment end didn't match", end, segment.end);
    }*/

    protected void assertRecordDocIds(List<LogRecord> records, String... docids) {
        assertEquals("Segment should have " + docids.length + " records", docids.length, records.size());
        for (int i = 0; i < docids.length; i++) {
            assertEquals("Record " + (i+1) + " should have docid=" + docids[i], docids[i], records.get(i).get_docid());
        }
    }

    protected void assertRecordIds(List<LogRecord> records, long... ids) {
        assertEquals("Segment should have " + ids.length + " records", ids.length, records.size());
        for (int i = 0; i < ids.length; i++) {
            assertEquals("Record " + (i+1) + " should have id=" + ids[i], ids[i], records.get(i).get_id());
        }
    }

    static LogBatch batch(LogRecord... records) {
        LogBatch b = new LogBatch();
        for (LogRecord r : records) {
            b.add_to_records(r);
        }
        return b;
    }
    static LogRecord addRecord(String docid, String code) {
        LogRecord r = new LogRecord();
        r.set_deleted(false);
        r.set_docid(docid);
        r.set_index_code(code);
        return r;
    }
    static LogRecord addRecord(String docid, long id) {
        LogRecord r = new LogRecord();
        r.set_deleted(false);
        r.set_docid(docid);
        r.set_id(id);
        return r;
    }
    
    static LogRecord deleteRecord(String docid) {
        LogRecord r = new LogRecord();
        r.set_deleted(true);
        r.set_docid(docid);
        return r;
    }
    
    static LogRecord text(LogRecord r, String text) {
        return field(r, "text", text);
    }
    
    static LogRecord field(LogRecord r, String field, String value) {
        r.put_to_fields(field, value);
        return r;
    }
    
    static LogRecord variable(LogRecord r, int var, double value) {
        r.put_to_variables(var, value);
        return r;
    }
    
    static LogRecord category(LogRecord r, String field, String value) {
        r.put_to_categories(field, value);
        return r;
    }
    
    static {
        String[] lines = new String[50];
        Arrays.fill(lines, "some text goes here some text goes here some text goes here");
        LONG_TEXT = Joiner.on("\n").join(lines); 
    }
    protected static String LONG_TEXT;

}
