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
    private static final int DEFAULT_MAX_SEARCH_QUEUE_LENGTH = Integer.MAX_VALUE;
    private final int maxSearchQueueLength;
    private final DocumentSearcher delegate;
    private final Semaphore semaphore;

    public TrafficLimitingSearcher(DocumentSearcher searcher) {
        this(searcher, DEFAULT_MAX_SEARCH_QUEUE_LENGTH);
    }

    /**
     * @param searcher the delegate DocumentSearcher
     * @param maxSearchQueueLength max allowed search queue length, or 0 for no waiters allowed
     */
    public TrafficLimitingSearcher(DocumentSearcher searcher, int maxSearchQueueLength) {
		Preconditions.checkNotNull(searcher);
        Preconditions.checkArgument(maxSearchQueueLength >= 0);
        this.delegate = searcher;
        this.maxSearchQueueLength = maxSearchQueueLength;
        semaphore = new Semaphore(MAX_NUMBER_OF_PARALLEL_REQUESTS, true);
    }

    @Override
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException {
        // call delegate searcher
        try {
            int queueLen = semaphore.getQueueLength();
            if (queueLen >= maxSearchQueueLength) {
                logger.warn("Too many waiting to search, queue length = " + queueLen + ", returning without searching");
                throw new InterruptedException("Too many concurrent searches");
            }
            if (queueLen > 0) {
                logger.warn("Concurrent searches queue length is " + queueLen + ", will wait");
            }
            // consider adding a timeout to this call to semaphore.acquire()
            semaphore.acquire();
            try {
                return this.delegate.search(query, start, limit, scoringFunctionIndex, extraParameters);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            logger.warn("Throwing InterruptedException: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public int countMatches(Query query) throws InterruptedException {
        try {
            int queueLen = semaphore.getQueueLength();
            if (queueLen >= maxSearchQueueLength) {
                logger.warn("Too many waiting to search, queue length = " + queueLen + ", returning without searching");
                throw new InterruptedException("Too many concurrent searches");
            }
            if (queueLen > 0) {
                logger.warn("Concurrent searches queue length is " + queueLen + ", will wait");
            }
            // consider adding a timeout to this call to semaphore.acquire()
            semaphore.acquire();
            try {
                return this.delegate.countMatches(query);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            logger.warn("Throwing InterruptedException: " + e.getMessage());
            throw e;
        }
    }
}

