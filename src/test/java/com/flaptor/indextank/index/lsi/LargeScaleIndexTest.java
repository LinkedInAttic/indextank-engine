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
import java.util.List;
import java.util.Set;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.scorer.MockScorer;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.google.common.collect.Lists;


public class LargeScaleIndexTest extends TestCase {

    private LargeScaleIndex lsi;
    private File tempDir;

    @Override
	protected void setUp() throws Exception {
        tempDir = FileUtil.createTempDir("lsiindexer","test");
        MockScorer scorer = new MockScorer();
        lsi = new LargeScaleIndex(scorer, new IndexEngineParser("text"),tempDir, new NoFacetingManager());
    
	}
	
    @Override
	protected void tearDown() throws Exception {
        FileUtil.deleteDir(tempDir);
	}
	
	@TestInfo(testType=UNIT)
	public void testCheckpoint() throws IOException, InterruptedException {
        List<Document> docs = Lists.newArrayList();
        int limit = 10;
        for (int i = 0; i < limit; i++) { 
            Document doc = new Document();
            doc.setField("fieldA","A term"+i);
            doc.setField("fieldB","B term"+i);
            docs.add(doc);
        } 
      

        this.lsi.add("first_doc", docs.get(0));
        IndexingDumpCompletionListener idcl = new IndexingDumpCompletionListener(this.lsi,docs);
        idcl.start();
        this.lsi.startDump(idcl);
        idcl.waitUntilCompleted();

        assertEquals("Checkpoint made it to the index", 0, lsi.findMatches(new Query(new TermQuery("documentId","checkpoint_doc"),null,null),1, 1).getTotalMatches());
        assertEquals("First doc didn't make it to the index", 1, lsi.findMatches(new Query(new TermQuery("documentId","first_doc"),null,null),1, 1).getTotalMatches());
        // TODO test there is only one thing on the index.
        
        this.lsi.startDump(new DummyDumpCompletionListener());
        assertEquals("Checkpoint didn't make it to the index", 1, lsi.findMatches(new Query(new TermQuery("documentId","checkpoint_doc"),null,null),1, 1).getTotalMatches());
        assertEquals("First doc didn't make it to the index", 1, lsi.findMatches(new Query(new TermQuery("documentId","first_doc"),null,null),1, 1).getTotalMatches());
        // TODO test that there are limit + 2 documents on the index

	}

    
	@TestInfo(testType=UNIT)
	public void testEnqueuesWhileProcessingQueue() throws IOException, InterruptedException {
        List<Document> docs = Lists.newArrayList();
        int limit = 10;
        for (int i = 0; i < limit; i++) { 
            Document doc = new Document();
            doc.setField("fieldA","A term"+i);
            doc.setField("fieldB","B term"+i);
            docs.add(doc);
        } 
        Document doc = new IndexTriggeringDocument(this.lsi,"badapple");
        doc.setField("version","first");
        doc.setField("pepe","badapple");
        docs.add(doc);


        IndexingDumpCompletionListener idcl = new IndexingDumpCompletionListener(this.lsi,docs);
        idcl.start();
        this.lsi.startDump(idcl);
        idcl.waitUntilCompleted();
        
        assertEquals("Bad Apple made it to the index", 0, lsi.findMatches(new Query(new TermQuery("pepe","badapple"),null,null),1, 0).getTotalMatches());

        Execute.sleep(1000);
        docs = Lists.newArrayList();
        idcl = new IndexingDumpCompletionListener(this.lsi,docs);
        idcl.start();
        this.lsi.startDump(idcl);
        idcl.waitUntilCompleted();

        assertEquals("I expected just ONE bad apple", 1, lsi.findMatches(new Query(new TermQuery("pepe","badapple"),null,null),1, 0).getTotalMatches());
        assertEquals("I expected one document that matched version:latest", 1, lsi.findMatches(new Query(new TermQuery("version","latest"),null,null),1, 0).getTotalMatches());
        
    } 

    private class IndexingDumpCompletionListener extends Thread implements DumpCompletionListener {
        private LargeScaleIndex lsi;
        private List<Document> docs;
        private boolean called;

        IndexingDumpCompletionListener(LargeScaleIndex lsi, List<Document> docs){
            this.lsi = lsi;
            this.docs = docs;
            this.called = false;
        }

        public void dumpCompleted(){
            try { 
                this.join();
                Document doc = new Document();
                doc.setField("fieldA","checkpoint");
                this.lsi.add("checkpoint_doc",doc);
            } catch (InterruptedException ie){
                // TODO log this  
            } finally { 
                this.called = true;
            } 
        }

        public void run(){
            int i = 0;
            for (Document doc: this.docs) {
                String docId  = "docID_" + i++;

                // makes easier to predict documentID.
                if (doc.getField("pepe")!= null) {
                    docId = doc.getField("pepe");
                }
                Execute.sleep(100);
                this.lsi.add(docId,doc);
            }
        }

        public void waitUntilCompleted(){
            while (!this.called){
                Execute.sleep(100);
            }
        }
    }

    private class DummyDumpCompletionListener implements DumpCompletionListener {
        public void dumpCompleted(){};
    }

    private class IndexTriggeringDocument extends Document {
        private LargeScaleIndex lsi;
        private String doc_id;
        private boolean triggered;

        public IndexTriggeringDocument(LargeScaleIndex lsi, String doc_id){
            this.lsi = lsi;
            this.doc_id = doc_id;
            this.triggered = false;
        }

        public Set<String> getFieldNames(){
            // Trigger the indexing of an update of self.
            // This method should be only called when indexing, so this is kinda safe.
            if (!this.triggered) { 
                Document doc = new Document();
                doc.setField("version","latest");
                doc.setField("pepe",this.getField("pepe"));
                this.lsi.add(this.doc_id,doc);
                this.triggered = true; 
                Execute.sleep(100);
            } 
            return super.getFieldNames();
        }
    }

}
