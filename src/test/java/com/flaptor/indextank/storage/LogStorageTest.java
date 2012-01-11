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

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestInfo.TestType;
import com.google.common.collect.Lists;

public class LogStorageTest extends BaseLogStorageTest {

    /*@TestInfo(testType=UNIT)
    public void testThreeSegments() throws IOException, TException {
        File parent = new File(dir, "three-segments");
        parent.mkdirs();
        Segment s1 = Segment.continueLog(root, parent, false);
        s1.write(text(addRecord("a", 1), "some text goes here"));
        s1.write(text(addRecord("b", 3), "another text goes here"));
        Segment s2 = s1.sortAndCloseSegment(100L);
        s2.write(text(addRecord("d", 5), "some text goes here 2"));
        s2.write(text(addRecord("c", 7), "another text goes here 2"));
        Segment s3 = s2.sortAndCloseSegment(200L);
        s3.write(text(addRecord("e", 9), "some text goes here 3"));
        s3.write(text(addRecord("f", 11), "another text goes here 3"));
        s3.release(); // release it but leave it open
        
        List<Segment> segments = Segment.getSegments(root, parent);
        assertEquals("Didn't find three segments as expected", 3, segments.size());
        
        Segment ss1 = segments.get(0);
        //assertSegmentInfo(ss1, 1L, 2L, false);
        
        List<LogRecord> records1 = Lists.newArrayList(ss1.reader());
        assertRecordIds(records1, 1L, 3L);
        assertRecordDocIds(records1, "a", "b");
        
        Segment ss2 = segments.get(1);
        //assertSegmentInfo(ss2, 3L, 4L, false);
        
        List<LogRecord> records2 = Lists.newArrayList(ss2.reader());
        assertRecordIds(records2, 7L, 5L);
        assertRecordDocIds(records2, "c", "d");
        
        Segment ss3 = segments.get(2);
        //assertSegmentInfo(ss3, 5L, null, false);
        
        List<LogRecord> records3 = Lists.newArrayList(ss3.reader());
        assertRecordIds(records3, 9L, 11L);
        assertRecordDocIds(records3, "e", "f");
    }*/
    
    @TestInfo(testType=UNIT)
    public void testLiveLog() throws TException, IOException {
        RawLog log = new RawLog(root, 100);
        log.write(text(addRecord("a", "idx"), "some text goes here"));
        log.write(text(addRecord("a", "idx"), "another text goes here"));
        log.write(text(addRecord("c", "idx"), "some text goes here 2"));
        log.write(text(addRecord("b", "idx"), "another text goes here 2"));
        log.write(text(addRecord("c", "idx"), "some text goes here 3"));
        log.write(text(addRecord("b", "idx"), "another text goes here 3"));
        log.flush();
    
        List<Segment> segments = log.getLiveSegments();
        assertEquals("Didn't find 3 segments as expected", 3, segments.size());
        
        Segment s1 = segments.get(0);
        //assertSegmentInfo(s1, 1L, 2L, false);
        
        for (LogRecord r : s1.reader()) {
            System.out.println(r);
        }
        
        List<LogRecord> records1 = Lists.newArrayList(s1.reader());
        //assertRecordIds(records1, 1L, 2L);
        assertRecordDocIds(records1, "a", "a");
        
        Segment s2 = segments.get(1);
        //assertSegmentInfo(s2, 3L, 4L, false);
        
        List<LogRecord> records2 = Lists.newArrayList(s2.reader());
        //assertRecordIds(records2, 3L, 4L);
        assertRecordDocIds(records2, "c", "b");
        
        Segment s3 = segments.get(2);
        //assertSegmentInfo(s3, 5L, null, false);
        
        List<LogRecord> records3 = Lists.newArrayList(s3.reader());
        //assertRecordIds(records3, 5L, 6L);
        assertRecordDocIds(records3, "c", "b");
    }

    @TestInfo(testType=UNIT)
    public void testLiveLogReopen() throws TException, IOException {
        RawLog log = new RawLog(root, 100);
        log.write(text(addRecord("a", "idx"), "some text goes here"));
        log.write(text(addRecord("a", "idx"), "another text goes here"));
        log.write(text(addRecord("c", "idx"), "some text goes here 2"));
        log.write(text(addRecord("b", "idx"), "another text goes here 2"));
        log.write(text(addRecord("c", "idx"), "some text goes here 3"));
        log.flush();
        log.write(text(addRecord("b", "idx"), "another text goes here 3"));
        log.flush();
        
        List<Segment> segments = log.getLiveSegments();
        assertEquals("Didn't find 3 segments as expected", 3, segments.size());
        
        Segment s1 = segments.get(0);
        //assertSegmentInfo(s1, 1L, 2L, false);
        
        List<LogRecord> records1 = Lists.newArrayList(s1.reader());
        //assertRecordIds(records1, 1L, 2L);
        assertRecordDocIds(records1, "a", "a");
        
        Segment s2 = segments.get(1);
        ///assertSegmentInfo(s2, 3L, 4L, false);
        
        List<LogRecord> records2 = Lists.newArrayList(s2.reader());
        //assertRecordIds(records2, 3L, 4L);
        assertRecordDocIds(records2, "c", "b");
        
        Segment s3 = segments.get(2);
        //assertSegmentInfo(s3, 5L, null, false);
        
        List<LogRecord> records3 = Lists.newArrayList(s3.reader());
        //assertRecordIds(records3, 5L, 6L);
        assertRecordDocIds(records3, "c", "b");
    }

    /*@TestInfo(testType=UNIT)
    public void testIndexLogStraight() throws TException, IOException {
        IndexLog log = new IndexLog("saraza", manager, 100);
        log.write(text(addRecord("a",1), "some text goes here"));
        log.write(text(addRecord("b",3), "another text goes here"));
        log.write(text(addRecord("d",5), "some text goes here 2"));
        log.write(text(addRecord("c",7), "another text goes here 2"));
        log.write(text(addRecord("e",9), "some text goes here 3"));
        log.write(text(addRecord("f",11), "another text goes here 3"));
        log.release();
        
        List<Segment> segments = log.getSegments();
        assertEquals("Didn't find 3 segments as expected", 3, segments.size());
        
        Segment s1 = segments.get(0);
        assertSegmentInfo(s1, 1L, 3L, true);
        
        List<LogRecord> records1 = Lists.newArrayList(s1.reader());
        assertRecordIds(records1, 1L, 3L);
        assertRecordDocIds(records1, "a", "b");
        
        Segment s2 = segments.get(1);
        assertSegmentInfo(s2, 4L, 7L, true);
        
        List<LogRecord> records2 = Lists.newArrayList(s2.reader());
        assertRecordIds(records2, 7L, 5L);
        assertRecordDocIds(records2, "c", "d");
        
        Segment s3 = segments.get(2);
        assertSegmentInfo(s3, 8L, null, false);
        
        List<LogRecord> records3 = Lists.newArrayList(s3.reader());
        assertRecordIds(records3, 9L, 11L);
        assertRecordDocIds(records3, "e", "f");
    }

    @TestInfo(testType=UNIT)
    public void testIndexLogSegmentMerge() throws TException, IOException {
        IndexLog log = new IndexLog("saraza", manager, 100);
        log.write(text(addRecord("a",1), "some text goes here"));
        log.write(text(addRecord("a",3), "another text goes here"));
        log.write(text(addRecord("b",5), "some text goes here 2"));
        log.write(text(addRecord("b",7), "another text goes here 2"));
        log.write(text(addRecord("c",9), "some text goes here 3"));
        log.write(text(addRecord("c",11), "another text goes here 3"));
        log.release();
        
        List<Segment> segments = log.getSegments();
        assertEquals("Didn't find 3 segments as expected", 3, segments.size());
        
        Segment s1 = segments.get(0);
        assertSegmentInfo(s1, 1L, 3L, true);
        
        List<LogRecord> records1 = Lists.newArrayList(s1.reader());
        assertRecordIds(records1, 3L);
        assertRecordDocIds(records1, "a");
        
        Segment s2 = segments.get(1);
        assertSegmentInfo(s2, 4L, 7L, true);
        
        List<LogRecord> records2 = Lists.newArrayList(s2.reader());
        assertRecordIds(records2, 7L);
        assertRecordDocIds(records2, "b");
        
        Segment s3 = segments.get(2);
        assertSegmentInfo(s3, 8L, null, false);
        
        List<LogRecord> records3 = Lists.newArrayList(s3.reader());
        assertRecordIds(records3, 9L, 11L);
        assertRecordDocIds(records3, "c", "c");
    }

    @TestInfo(testType=UNIT)
    public void testIndexLogOptimized() throws TException, IOException {
        IndexLog log = new IndexLog("saraza", manager, 100);
        log.write(text(addRecord("a",1), "some text goes here"));
        log.write(text(addRecord("a",3), "another text goes here"));
        log.write(text(addRecord("c",5), "some text goes here 2"));
        log.write(text(addRecord("b",7), "another text goes here 2"));
        log.write(text(addRecord("c",9), "some text goes here 3"));
        log.write(text(addRecord("b",11), "another text goes here 3"));
        log.release();
        
        log = new IndexLog("saraza", manager, 100);
        log.forceClose(12);
        
        log = new IndexLog("saraza", manager, 100);
        new LogOptimizer(manager).optimize(log);
        
        Segment segment = log.getLargestOptimizedSegment();
        assertSegmentInfo(segment, 1L, 12L, true);
        
        List<LogRecord> records = Lists.newArrayList(segment.reader());
        assertRecordIds(records, 3L, 11L, 9L);
        assertRecordDocIds(records, "a", "b", "c");
    }*/

    /*@SuppressWarnings("unchecked")
    @TestInfo(testType=UNIT)
    public void testLiveLogDealt() throws TException, IOException {
        LiveLog log = new LiveLog(root, 1024 * 1024);
        Set<String> originalIds[] = new Set[2];
        originalIds[0] = Sets.newHashSet();
        originalIds[1] = Sets.newHashSet();
        for (int i = 0; i < 5000; i++) {
            String id = String.valueOf(i);
            originalIds[i%2].add(id);
            log.write(text(addRecord(id, "idx" + i%2), LONG_TEXT));
        }
        log.flush();
        assertEquals(0, new IndexLog("idx0", root, 256 * 1024).getSegments().size());
        assertEquals(0, new IndexLog("idx1", root, 256 * 1024).getSegments().size());
        LogDealerServer dealer = new LogDealerServer(root, 1024 * 1024, 256 * 1024);
        dealer.runCycle();
        for (int i = 0; i < 2; i++) {
            String idx = "idx" + i;
            IndexLog indexLog = new IndexLog(idx, root, 256 * 1024);
            indexLog.forceClose(dealer.getNextTimestamp());
            LogOptimizer optimizer = new LogOptimizer(root);
            optimizer.checkOptimize(indexLog);
            Segment segment = indexLog.getLargestOptimizedSegment();
            Segment last = log.getLastSegment();
            Set<String> ids = Sets.newHashSet();
            for (LogRecord c : segment.reader()) { ids.add(c.get_docid()); }
            for (LogRecord c : last.reader()) { if (c.get_index_code().equals(idx)) ids.add(c.get_docid()); }
            assertEquals(originalIds[i], ids);
        }
    }*/

    /*@SuppressWarnings("unchecked")
    @TestInfo(testType=TestType.INTEGRATION)
    public void testIntegration() throws TException, IOException {
        LogWriterServer writer = new LogWriterServer(root, 20000, true);
        IndexesLogServer reader = new IndexesLogServer(root, 20000, 10000);
        LogOptimizer optimizer = new LogOptimizer(reader.dealer, root);
        
        for (int i = 0; i < 300; i++) {
            String id = String.valueOf(i);
            String code = "code" + i%3;
            LogRecord r1 = variable(text(addRecord(id,code), LONG_TEXT), 1, 2.0);
            writer.send_batch(batch(r1));
        }
        
        Map<String, LogRecord> maps[] = new Map[3];
        for (int i = 0; i < 3; i++) {
            maps[i] = readFully(reader, "code"+i);
        }
        
        for (int i = 0; i < 300; i++) {
            String docid = String.valueOf(i);
            int idx = i % 3;
            assertTrue("Document not found on index " + i, maps[idx].containsKey(docid));
            assertEquals("Variable not set properly", 2.0, maps[idx].get(docid).get_variables().get(1));
        }
    
        reader.dealer.runCycle();
        
        for (int i = 0; i < 3; i++) {
            maps[i] = readFully(reader, "code"+i);
        }
        for (int i = 0; i < 300; i++) {
            String docid = String.valueOf(i);
            int idx = i % 3;
            assertTrue("Document not found on index " + i, maps[idx].containsKey(docid));
            assertEquals("Variable not set properly", 2.0, maps[idx].get(docid).get_variables().get(1));
        }
        
        optimizer.optimize("code0");
        optimizer.optimize("code1");
        
        for (int i = 0; i < 3; i++) {
            maps[i] = readFully(reader, "code"+i);
        }
        for (int i = 0; i < 300; i++) {
            String docid = String.valueOf(i);
            int idx = i % 3;
            assertTrue("Document not found on index " + i, maps[idx].containsKey(docid));
            assertEquals("Variable not set properly", 2.0, maps[idx].get(docid).get_variables().get(1));
        }
        
        optimizer.optimize("code1");
        optimizer.optimize("code2");
        
        for (int i = 0; i < 3; i++) {
            maps[i] = readFully(reader, "code"+i);
        }
        for (int i = 0; i < 300; i++) {
            String docid = String.valueOf(i);
            int idx = i % 3;
            assertTrue("Document not found on index " + i, maps[idx].containsKey(docid));
            assertEquals("Variable not set properly", 2.0, maps[idx].get(docid).get_variables().get(1));
        }
    }*/

}
