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
