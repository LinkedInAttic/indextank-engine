package com.flaptor.indextank.index;

import java.io.IOException;
import java.util.Map;

public interface Promoter extends QueryMatcher {
    public void promoteResult(String docId, String queryStr);
    public String getPromotedDocId(String queryStr);
    public void dump() throws IOException;
    public Map<String, String> getStats();
}
