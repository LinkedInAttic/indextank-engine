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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.flaptor.indextank.Indexer;
import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public class LargeScaleIndex implements QueryMatcher, Indexer {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final String INDEX_DIRECTORY = "index";
	
    protected LsiIndex index;
    protected LsiIndexer indexer;
    //private LsiSearcher searcher;
    private QueryMatcher searcher;
    private BlockingQueue<Operation> queue;
    @SuppressWarnings("unused")
    private Scorer scorer;
    private File baseDir;
   
    private ReadWriteLock rwl;
    private Lock r;
    private Lock w;
    private boolean checkpoint;

    LargeScaleIndex() {
        // empty constructor for test stubs
    }
    
    /**
     * Create an LSI and its components
     * 
     * @param scorer The scorer to use when ranking results
     * @param parser 
     * @param basePath The base path (a directory) from the which all the LSI directories will be found.   
     */
    public LargeScaleIndex(Scorer scorer, IndexEngineParser parser, File baseDir, FacetingManager facetingManager) {
		Preconditions.checkNotNull(scorer);
        Preconditions.checkNotNull(parser);
        Preconditions.checkNotNull(baseDir);
    	
    	this.baseDir = baseDir;

    	if (!baseDir.exists() || !baseDir.isDirectory()) {
    		throw new IllegalArgumentException("The basePath must be an existing directory");
    	}
        File indexDir= new File(baseDir,INDEX_DIRECTORY);
        if (!indexDir.exists()) { 
            logger.info("Starting with a FRESH, BRAND NEW index.");
            indexDir.mkdir();
        } 

        try {
            index = new LsiIndex(parser, indexDir.getAbsolutePath(), scorer, facetingManager);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException when trying to use the directory set in the index.directory property.", e);
        }

        this.scorer = scorer;
        this.indexer = new LsiIndexer(index);
        this.searcher = new LsiSearcher(index);
        this.queue = new ArrayBlockingQueue<Operation>(1000);
        this.rwl = new ReentrantReadWriteLock();
        this.r = rwl.readLock();
        this.w = rwl.writeLock();
        this.checkpoint = false;

    }

    @Override
    public boolean hasChanges(DocId docid) throws InterruptedException {
    	return searcher.hasChanges(docid);
    }
    
    @Override
    public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException {
        return searcher.findMatches(query, limit, scoringFunctionIndex);
    }

	@Override
	public TopMatches findMatches(Query query, Predicate<DocId> docFilter, int limit, int scoringFunctionIndex) throws InterruptedException {
		return searcher.findMatches(query, docFilter, limit, scoringFunctionIndex);
	}


    /*
     * (non-Javadoc)
     * @see com.flaptor.indextank.index.IIndexer#add(java.lang.String, com.flaptor.indextank.index.Document)
     */
    public void add(String docid, Document doc) {
        r.lock();
        boolean enqueue = this.checkpoint;
        r.unlock();

        if (enqueue) {
            try {
                logger.debug("enqueueing " + docid + " for later indexing" ); 
                this.queue.put(new AddOperation(docid,doc));
            } catch (InterruptedException ie){
                // TODO log it here, document dumped / lost 
            }
        } else {
            indexer.add(docid,doc); 
        }
    }

    /*
     * (non-Javadoc)
     * @see com.flaptor.indextank.index.IIndexer#del(java.lang.String)
     */
    public void del(String docid) {
        r.lock();
        boolean enqueue = this.checkpoint;
        r.unlock();
        if (enqueue) { 
            try { 
                this.queue.put(new DelOperation(docid));
            } catch (InterruptedException ie){
                // TODO log it here, document dumped / lost 
            }
        } else {
            indexer.del(docid);
        }
    }
   
    public void startDump(DumpCompletionListener listener){
        w.lock();
        if (this.checkpoint) {
            throw new IllegalStateException("2 simultaneous dumps");// TODO 2 checkpoints simultaneous?;
        }
        this.checkpoint = true;
        w.unlock();
        logger.debug("About to start a directory checkpoint");
        indexer.makeDirectoryCheckpoint();
        logger.debug("Directory checkpoint done. Telling listener about it.");
        listener.dumpCompleted();
        logger.debug("Consuming queue of pending operations for next segment...");
        OperationWorker worker = new OperationWorker(this.indexer,this.queue);
        worker.start();
        try { 
            worker.join();
        } catch (InterruptedException ie){
            // TODO log ie
        }
        // TODO buggy. something may get into the queue after the worker loop. 
        logger.debug("Done consuming the queue. Ready to accept direct operations");
        w.lock();
        this.checkpoint = false;
        w.unlock();
    }

    public File getBaseDir() {
        return baseDir;
    }

    /**
     * INNER CLASSES
     */
    private interface Operation {
        public void execute(LsiIndexer indexer);    
    }

    private class AddOperation implements Operation {
        Document doc;
        String docid;

        AddOperation(String docid, Document doc){
            this.docid = docid;
            this.doc = doc;
        }
        public void execute(LsiIndexer indexer){
            indexer.add(this.docid,this.doc);
        }
    }
    
    private class DelOperation implements Operation {
        String docid;
        DelOperation(String docid){
            this.docid = docid;
        }
        public void execute(LsiIndexer indexer){
            indexer.del(this.docid);
        }
    } 

    private class OperationWorker extends Thread {
        BlockingQueue<Operation> queue;
        LsiIndexer indexer;

        OperationWorker(LsiIndexer indexer, BlockingQueue<Operation> queue){
            this.queue = queue;
            this.indexer = indexer;
        }

        public void run(){
            while (true){
                Operation op = this.queue.poll();
                if (null != op) {
                    op.execute(this.indexer);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public int countMatches(Query query) throws InterruptedException {
        return searcher.countMatches(query);
    }

    @Override
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException {
        return searcher.countMatches(query, idFilter);
    }
    
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap(index.getStats());
        stats.put("lsi_queue_size", String.valueOf(queue.size()));
        return stats;
    }  

}
