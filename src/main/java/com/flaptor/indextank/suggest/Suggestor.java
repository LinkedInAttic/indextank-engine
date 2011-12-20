package com.flaptor.indextank.suggest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.query.Query;

/**
 * Query suggestor/autocompleter/"did you mean?" interface.
 */
public interface Suggestor {

    /**
     * Notifies the suggestor of a performed query.
     * Can be used by the suggestor implementation to generate statistics
     * about performed queries in order to use them to make suggestions in
     * the future.
     * @query the query performed by the user.
     * @matches the number of matching document this query has.
     */
    public void noteQuery(Query query, int matches);

    /**
     * Notifies the suggestor of a new document in the index.
     * Can be used by the suggestor implementation to generate statistics
     * about the corpus in order to use them to make suggestions in the
     * future.
     */
    public void noteAdd(String documentId, Document doc);

    /**
     * Main autocomplete method.
     * This method is called by the user's frontend
     * while the user is typing in order to get a List of queries to suggest.
     * @param field 
     */
    public List<String> complete(String partialQuery, String field);

    public Map<String, String> getStats();

    /**
     * Dumps the state to disk.
     */
    public void dump() throws IOException;
}
