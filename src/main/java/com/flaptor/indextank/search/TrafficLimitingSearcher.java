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

import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.flaptor.indextank.query.Query;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;


public class TrafficLimitingSearcher extends AbstractDocumentSearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final int MAX_NUMBER_OF_PARALLEL_REQUESTS = 3;
    private final DocumentSearcher delegate;
    private final Semaphore semaphore;

    public TrafficLimitingSearcher(DocumentSearcher searcher) {
		Preconditions.checkNotNull(searcher);
        this.delegate = searcher;
        semaphore = new Semaphore(MAX_NUMBER_OF_PARALLEL_REQUESTS);
    }


    @Override
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException {
    	// call delegate searcher
        try {
            try {
                if (!semaphore.tryAcquire()) {
                    logger.warn("Too many concurrent searches. Will wait.");
                    semaphore.acquire();
                }
            } catch (InterruptedException e) {}
            return this.delegate.search(query, start, limit, scoringFunctionIndex, extraParameters);
        } finally {
            semaphore.release();
        }
    }

    @Override
    public int countMatches(Query query) throws InterruptedException {
        try {
            try {
                if (!semaphore.tryAcquire()) {
                    logger.warn("Too many concurrent searches. Will wait.");
                    semaphore.acquire();
                }
            } catch (InterruptedException e) {}
            return this.delegate.countMatches(query);
        } finally {
            semaphore.release();
        }
    }
}
