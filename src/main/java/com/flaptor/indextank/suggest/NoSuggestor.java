package com.flaptor.indextank.suggest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.query.Query;
import com.google.common.collect.Lists;

/**
 * A dummy suggestor implementation that does not suggest anything.
 */
public class NoSuggestor implements Suggestor {

    @Override
    public void noteQuery(Query query, int matches) {
    }

    @Override
    public void noteAdd(String documentId, Document doc) {
    }

    @Override
    public List<String> complete(String partialQuery, String field) {
        List<String> retVal = Lists.newArrayList();
        retVal.add(partialQuery);
        return retVal;
    }

    @Override
    public void dump() {
    }
    
    @Override
    public Map<String, String> getStats() {
        return Collections.emptyMap();
    }
}
