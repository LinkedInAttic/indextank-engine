package com.flaptor.indextank.search;

import java.util.Map;

import com.flaptor.indextank.query.Query;


public interface DocumentSearcher {

    /**
     * Search interface.
     * @param query a AQuery implementation.
     * @param start the offset of the first document to retrieve.
     * @param limit the number of documents to retrieve.
     * @param scoringFunctionIndex TODO
     * @param snippetFields TODO
     * @param fetchFields TODO
     * @param snippetFields the field names for which snippets should be created 
     * @param fetchFields the field names that should be fetched completely
     */
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException;

    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex) throws InterruptedException;

    public int countMatches(Query query) throws InterruptedException;

}
