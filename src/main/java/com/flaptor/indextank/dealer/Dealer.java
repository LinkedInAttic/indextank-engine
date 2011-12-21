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


package com.flaptor.indextank.dealer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.flaptor.indextank.BoostingIndexer;
import com.flaptor.indextank.IndexRecoverer;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.Promoter;
import com.flaptor.indextank.index.lsi.DumpCompletionListener;
import com.flaptor.indextank.index.lsi.LargeScaleIndex;
import com.flaptor.indextank.index.rti.RealTimeIndex;
import com.flaptor.indextank.index.scorer.DynamicDataManager;
import com.flaptor.indextank.index.scorer.UserFunctionsManager;
import com.flaptor.indextank.suggest.Suggestor;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Deals index changes and commands to the RTI and LSI as needed.
 * @author Flaptor Team
 */
public class Dealer implements DumpCompletionListener, BoostingIndexer {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
    private LargeScaleIndex lsi;
    private RealTimeIndex rti;
    private Suggestor suggestor;
    private UserFunctionsManager functionsManager;
    private AtomicInteger docCount;
    private final Promoter promoter;
    private long timeOfMark;
    private boolean dumpInProgress;
    
    private int rtiSize;

	private final DynamicDataManager dynamicDataManager;

    public Dealer(LargeScaleIndex lsi, RealTimeIndex rti, Suggestor suggestor, DynamicDataManager dynamicDataManager, int rtiSize, Promoter promoter, UserFunctionsManager functionsManager) {
		Preconditions.checkNotNull(lsi);
        Preconditions.checkNotNull(rti);
        Preconditions.checkNotNull(suggestor);
        Preconditions.checkNotNull(functionsManager);
        Preconditions.checkNotNull(dynamicDataManager);
        Preconditions.checkArgument(rtiSize > 0);
        Preconditions.checkNotNull(promoter);
        this.lsi = lsi;
        this.rti = rti;
        this.suggestor = suggestor;
        this.functionsManager = functionsManager;
        this.docCount = new AtomicInteger(0);
        this.rtiSize = rtiSize;
        this.dynamicDataManager = dynamicDataManager;
        this.timeOfMark = 0;
        this.promoter = promoter;
        this.dumpInProgress = false;
    }

    @Override
    public void dump() throws IOException {
        logger.info("Starting Dealer's dump");
        dumpInProgress = true;
        lock.writeLock().lock();
        try {
            switchIndexesOnce(true);
        } finally {
            lock.writeLock().unlock();
        }
        dynamicDataManager.dump();
        suggestor.dump();
        promoter.dump();
        while (dumpInProgress) {
            Execute.sleep(100);
        }
        logger.info("Dealer's dump completed.");
    }

	@Override
	public void add(String docId, Document document, int timestampBoost, Map<Integer, Double> dynamicBoosts) {
        long startTime = System.currentTimeMillis();
    	/* 
    	 * Locking: 
    	 * 
    	 *   lock.writeLock:
    	 *     Acquired if the threshold has been hit. It will remain locked
    	 *     during the switching operation and it will guarantee that no adds will be 
    	 *     executed until the switch has been executed. Many threads may acquire it
    	 *     but only one will execute the switch thanks to the AtomicInteger in docCount.
    	 *      
    	 *   lock.readLock:
    	 *     Acquired for adds to internal structures (allowing multiple adds in parallel)
    	 *     but guaranteeing that they will wait until any ongoing switch is executed
    	 *     
    	 *   This way, switch operations are guaranteed to be atomic in respect to adds. 
    	 *   
    	 *   The use of the AtomicInteger also guarantees that no more than `threshold` 
    	 *   documents will be added to the indexes before the switch operation is effectively
    	 *   executed
    	 *  
    	 */
        int loopCount = 0;
    	while (true) {
    		loopCount++;
    		int currentCount = docCount.get();
    		
    		if (currentCount == rtiSize) {
    			// hit the threshold, should initiate a switch
                timeOfMark = System.currentTimeMillis()/1000;
    			lock.writeLock().lock();
    			try {
    				switchIndexesOnce(false);
    			} finally {
    				lock.writeLock().unlock();
    			}
    		} else {
    			lock.readLock().lock();
    			try {
	    			if (compareCountAndAdd(docId, document, timestampBoost, dynamicBoosts, currentCount)) {
	    			    if (loopCount == 1) {
	    			        logger.debug(String.format("(Add) optimistic locking took %d loops. Time tu this point: %d ms.", loopCount, System.currentTimeMillis() - startTime));
	    			    }
	    				break;
	    			}
    			} finally {
    				lock.readLock().unlock();
    			}
    		}
    	}
        
    	//Just to add the information to the suggestor
        long startTimeSuggestor = System.currentTimeMillis();
        suggestor.noteAdd(docId, document);
        logger.debug("(Add) suggest took: " + (System.currentTimeMillis() - startTimeSuggestor) + " ms.");
        //Now we log the total time this call took.
        logger.debug("(Add) whole method took: " + (System.currentTimeMillis() - startTime) + " ms.");
	
        
	}

	@Override
	public void updateBoosts(String documentId, Map<Integer, Double> updatedBoosts) {
		handleBoosts(documentId, updatedBoosts);
	}

    @Override
    public void updateCategories(String documentId, Map<String, String> categories) {
    	dynamicDataManager.setCategoryValues(documentId, categories);
    }

	@Override
	public void updateTimestamp(String documentId, int timestampBoost) {
		handleBoosts(documentId, timestampBoost);
	}

	
	private void handleBoosts(String documentId, int timestampBoost) {
		handleBoosts(documentId, timestampBoost, null);
	}
	
	private void handleBoosts(String documentId, Map<Integer, Double> updatedBoosts) {
		handleBoosts(documentId, null, updatedBoosts);
	}
	
    private void handleBoosts(String documentId, Integer timestampBoost, Map<Integer, Double> updatedBoosts) {
        Map<Integer, Float> floatBoosts;
    	if (updatedBoosts == null) {
    		floatBoosts = Collections.emptyMap();
    	} else {
    	    floatBoosts = Maps.newHashMap();
    	    
	        for (Entry<Integer, Double> entry : updatedBoosts.entrySet()) {
	            floatBoosts.put(entry.getKey(), entry.getValue().floatValue());
	        }
    	}
    	
    	if (timestampBoost != null) {   	
    		dynamicDataManager.setBoosts(documentId, timestampBoost, floatBoosts);
    	} else {
    		dynamicDataManager.setBoosts(documentId, floatBoosts);
    	}
    }
	
    @Override
    public void promoteResult(String docid, String query) {
        promoter.promoteResult(docid, query);
    }
	
	/**
	 * If currentCount matches the expectedCount, it will be incremented atomically
	 * and the given document will be indexed  
	 * @param timestampBoost 
	 * @return true iif the count matched and the add operation was executed
	 */
	private boolean compareCountAndAdd(String docId, Document doc, int timestampBoost, Map<Integer, Double> dynamicBoosts, int expectedCount) {
		if (docCount.compareAndSet(expectedCount, expectedCount + 1)) {
            long startTimeScorer = System.currentTimeMillis();
            handleBoosts(docId, timestampBoost, dynamicBoosts);
            long startTimeRti = System.currentTimeMillis();
			rti.add(docId, doc);
            long startTimeLsi = System.currentTimeMillis();
			lsi.add(docId, doc);
            long end = System.currentTimeMillis();
            logger.debug("(Add) scorer took: " + (startTimeRti - startTimeScorer) + " ms., lsi took: "
                    + (end - startTimeLsi) + "ms., rti took: " + (startTimeLsi - startTimeRti) + " ms.");
			return true;
		}
		return false;
	}

	/**
	 * This method should be called when `docCount` reaches `threshold`
	 * It can be called many times but it guarantees that only one thread
	 * will actually perform a switch and reset the `docCount` to 0. Every 
	 * other thread will do nothing.
     * @param force if set to true, the switch will happen regardless of the
     * current docCount.
	 * @return true iif the switch was executed by the current thread.
	 * false indicates that another thread has executed the switch before
	 * this one. 
	 */
	private boolean switchIndexesOnce(boolean force) {
		logger.info("(Add) attempting switch.");
        if (force) {
            docCount.set(0);
        }
		if (force || docCount.compareAndSet(rtiSize, 0)) {
			// successfully reseted count, this thread is in charge of switching
			switchIndexes();
			return true;
		}
		return false;
	}

    public void del(String docid) {
    	lock.readLock().lock();
    	try {
    		rti.del(docid);
    		lsi.del(docid);
    		dynamicDataManager.removeBoosts(docid);
    	} finally {
    		lock.readLock().unlock();
    	}
    }

    private void switchIndexes() {
        logger.debug("Starting switchIndexes. Marking rti.");
  		rti.mark();
        logger.debug("Starting lsi dump.");
  		lsi.startDump(this);
    }
    
    public void dumpCompleted() {
    	// LSI is already serving pre-mark documents
    	// it is now safe to clear them from RTI 
        logger.debug("lsi dump finished.");
    	rti.clearToMark();
        logger.debug("rti resetted. switchIndexes finished.");
        IndexRecoverer.writeTimestamp(lsi.getBaseDir(), timeOfMark);
        if (dumpInProgress) {
            dumpInProgress = false;
        }
    }

    @Override
    public void addScoreFunction(int functionIndex, String definition) throws Exception {
        this.functionsManager.addFunction(functionIndex, definition);
    }

    @Override
    public void removeScoreFunction(int functionIndex) {
        this.functionsManager.delFunction(functionIndex);
    }

    @Override
    public Map<Integer, String> listScoreFunctions() {
        return this.functionsManager.listFunctions();
    }
    
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.putAll(lsi.getStats());
        stats.putAll(rti.getStats());
        stats.putAll(suggestor.getStats());
        stats.putAll(functionsManager.getStats());
        stats.putAll(promoter.getStats());
        stats.putAll(dynamicDataManager.getStats());
        stats.put("dealer_last_mark", String.valueOf(this.timeOfMark));
        stats.put("dealer_dump_in_progress", String.valueOf(dumpInProgress));
        stats.put("dealer_doc_count", String.valueOf(docCount));
        Runtime runtime = Runtime.getRuntime();
        stats.put("jvm_free_memory", String.valueOf(runtime.freeMemory()));
        stats.put("jvm_max_memory", String.valueOf(runtime.maxMemory()));
        stats.put("jvm_total_memory", String.valueOf(runtime.totalMemory()));
        return stats;
    }



}
