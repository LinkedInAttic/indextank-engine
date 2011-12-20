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
