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
