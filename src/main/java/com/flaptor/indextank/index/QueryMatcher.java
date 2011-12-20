package com.flaptor.indextank.index;

import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;


public interface QueryMatcher {

    /**
     * Search interface.
     * @param query a AQuery implementation.
     * @param limit the number of documents to retrieve.
     * @param scoringFunctionIndex TODO
     */
    public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException;
    public TopMatches findMatches(Query query, Predicate<DocId> idFilter, int limit, int scoringFunctionIndex) throws InterruptedException;
    
    public int countMatches(Query query) throws InterruptedException;
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException;
    
    public boolean hasChanges(DocId docid) throws InterruptedException;

}
