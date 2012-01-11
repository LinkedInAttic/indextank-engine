package com.flaptor.indextank.index;

import java.util.Map;

public class DummyPromoter extends AbstractPromoter {
    @Override
    public void promoteResult(String docId, String queryStr) {
    }
    @Override
    public String getPromotedDocId(String queryStr) {
        return null;
    }
    @Override
    public void dump() {
    }
    @Override
    public Map<String, String> getStats() {
        return null;
    }
}
