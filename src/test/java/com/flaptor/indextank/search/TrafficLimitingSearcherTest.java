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


package com.flaptor.indextank.search;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.util.TestInfo;
import com.google.common.collect.Lists;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.flaptor.util.TestInfo.TestType.UNIT;

public class TrafficLimitingSearcherTest extends IndexTankTestCase {
	private TrafficLimitingSearcher sleepSearcher;
    private TrafficLimitingSearcher fastSearcher;

    @Override
	protected void setUp() throws Exception {
        super.setUp();
        this.sleepSearcher = new TrafficLimitingSearcher(new SleepingSearcher(3000), 3);
        this.fastSearcher = new TrafficLimitingSearcher(new SleepingSearcher(0), 3);
	}

    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    }

    private void sleep(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @TestInfo(testType=UNIT)
    public void testQueueTooLong() {
        // start 3 that can run in parallel, and sleep to let them start
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        // then start 3 to fill up the queue
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        new Thread(new RunSearch(sleepSearcher)).start();
        sleep(100);
        // now start one that should throw an exception because the queue is at max length
        try {
            final Query query = new Query(new TermQuery("text", "nada"), null, null);
            sleepSearcher.search(query, 0, 10, 0);
            fail("Should have thrown InterruptedException");
        } catch (InterruptedException e) {
            System.out.println("InterruptedException thrown, success");
        }
    }

    @TestInfo(testType=UNIT)
    public void testNonConcurrentSearching() throws InterruptedException {
        // make sure serial searches don't cause an InterruptedException
        for (int i = 0; i < 100; i++) {
            final Query query = new Query(new TermQuery("text", "nada"), null, null);
            fastSearcher.search(query, 0, 10, 0);
        }
    }
    
    @TestInfo(testType=UNIT)
    public void testConstructor() {
        new TrafficLimitingSearcher(sleepSearcher, 0);
        try {
            new TrafficLimitingSearcher(sleepSearcher, -1);
            fail("Constructor should not allow negative max queue length");
        } catch (IllegalArgumentException iae) {
            // pass
        }
    }

    private static class RunSearch implements Runnable {
        final DocumentSearcher searcher;

        private RunSearch(DocumentSearcher searcher) {
            this.searcher = searcher;
        }

        @Override
        public void run() {
            try {
                final Query query = new Query(new TermQuery("text","hola"),null,null);
                searcher.search(query, 0, 10, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class SleepingSearcher extends AbstractDocumentSearcher {
        private int sleepTime = 3000;

        private SleepingSearcher(int sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException {
            sleep(sleepTime);
            return new SearchResults(Lists.<SearchResult>newArrayList(), 0, null);
        }

        @Override
        public int countMatches(Query query) throws InterruptedException {
            sleep(sleepTime);                
            return 0;
        }
    }
}

