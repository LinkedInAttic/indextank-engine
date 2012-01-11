package com.flaptor.indextank.scorer;

import java.io.IOException;
import java.util.Map;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.scorer.Boosts;
import com.flaptor.indextank.index.scorer.BoostsManager;

public class DummyBoostsManager implements BoostsManager {

    @Override
    public void setBoosts(String documentId, Integer timestamp,
            Map<Integer, Float> boosts) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBoosts(String documentId, Map<Integer, Float> boosts) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeBoosts(String documentId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Boosts getBoosts(DocId documentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDocumentCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void dump() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<Integer, Double> getVariablesAsMap(DocId docId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getCategoryValues(DocId docId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getCategoriesAsMap(DocId documentId) {
        // TODO Auto-generated method stub
        return null;
    }

}
