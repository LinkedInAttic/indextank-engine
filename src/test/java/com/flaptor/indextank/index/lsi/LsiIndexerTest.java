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


package com.flaptor.indextank.index.lsi;

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.scorer.MockScorer;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;


public class LsiIndexerTest extends TestCase {

    private LsiIndexer lsiIndexer;
    private LsiIndex index;
    private File tempDir;

    @Override
	protected void setUp() throws Exception {
        tempDir = FileUtil.createTempDir("lsiindexer","test");
        Scorer scorer = new MockScorer();
        index = new LsiIndex(new IndexEngineParser("text"),tempDir.getAbsolutePath(), scorer, new NoFacetingManager());
        lsiIndexer = new LsiIndexer(index);
    
	}
	
    @Override
	protected void tearDown() throws Exception {
        FileUtil.deleteDir(tempDir);
	}
	
	@TestInfo(testType=UNIT)
	public void testUpdates() throws IOException {
        Document doc = new Document();
        doc.setField("fieldA","A");
        doc.setField("fieldB","B");

        lsiIndexer.add("A",doc);
        lsiIndexer.add("A",doc);

        doc.setField("fieldC","C");
        lsiIndexer.add("A",doc);
        lsiIndexer.makeDirectoryCheckpoint();

        assertEquals("Wrong document count", 1, index.getLuceneIndexWriter().numDocs());
        assertTrue("fieldC not found", index.getLuceneIndexWriter().getReader().getFieldNames(IndexReader.FieldOption.INDEXED).contains("fieldC"));
	}
	
    @TestInfo(testType=UNIT)
	public void testHandleDeletes() throws IOException, InterruptedException {
        Document doc = new Document();
        doc.setField("fieldA","A");
        doc.setField("fieldB","B");

        lsiIndexer.add("A",doc);
        lsiIndexer.add("A",doc);
        lsiIndexer.add("A",doc);
        lsiIndexer.del("A");
        lsiIndexer.makeDirectoryCheckpoint();

        assertEquals("Wrong document count", 0, index.getLuceneIndexWriter().numDocs());
	}
    
    @TestInfo(testType=UNIT)
	public void testHandleAddAfterDelete() throws IOException, InterruptedException {
        Document doc = new Document();
        doc.setField("fieldA","A");
        doc.setField("fieldB","B");

        lsiIndexer.add("A",doc);
        lsiIndexer.del("A");
        lsiIndexer.add("A",doc);
        lsiIndexer.makeDirectoryCheckpoint();

        assertEquals("Wrong document count", 1, index.getLuceneIndexWriter().numDocs());
	}
    
    @TestInfo(testType=UNIT)
	public void testHandleMultipleAdds() throws IOException, InterruptedException {
        Document doc = new Document();
        doc.setField("fieldA","A");
        doc.setField("fieldB","B");

        lsiIndexer.add("A",doc);
        lsiIndexer.add("B",doc);
        lsiIndexer.add("C",doc);
        lsiIndexer.makeDirectoryCheckpoint();

        assertEquals("Wrong document count", 3, index.getLuceneIndexWriter().numDocs());
	}
    
    @TestInfo(testType=UNIT)
	public void testHandleDeleteMissingDocument() throws IOException, InterruptedException {
        Document doc = new Document();
        doc.setField("fieldA","A");
        doc.setField("fieldB","B");

        lsiIndexer.add("A",doc);
        lsiIndexer.add("B",doc);
        lsiIndexer.del("C"); // does not exist
        lsiIndexer.makeDirectoryCheckpoint();

        assertEquals("Wrong document count", 2, index.getLuceneIndexWriter().numDocs());
	}

	
}
