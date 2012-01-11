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


package com.flaptor.indextank.index.rti;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.scorer.MockScorer;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;

public class RealTimeIndexStub extends RealTimeIndex {

    private TopMatches res;

    public RealTimeIndexStub(TopMatches res, int rtiSize) {
        super(new MockScorer(), new IndexEngineParser("text"), rtiSize, new NoFacetingManager());
        this.res = res;
    }

    @Override
    public QueryMatcher getSearchSession() {
        return new QueryMatcher() {
        	@Override
        	public TopMatches findMatches(Query query, Predicate<DocId> docFilter, int limit, int scoringFunctionIndex) {
        		return res;
        	}
            @Override
            public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) {
                return res;
            }
            @Override
            public boolean hasChanges(DocId docid) {
                return false;
            }
            @Override
            public int countMatches(Query query) {
                return res.getTotalMatches();
            }
            @Override
            public int countMatches(Query query, Predicate<DocId> idFilter) {
                return res.getTotalMatches();
            }
        };
    }
    
}
