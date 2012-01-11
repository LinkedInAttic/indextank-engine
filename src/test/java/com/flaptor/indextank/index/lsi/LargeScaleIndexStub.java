package com.flaptor.indextank.index.lsi;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;

public class LargeScaleIndexStub extends LargeScaleIndex {

    private TopMatches res;

    public LargeScaleIndexStub(TopMatches res) {
        super();
        this.res = res;
    }

    @Override
    public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) {
        return res;
    }
    
    @Override
    public TopMatches findMatches(Query query, Predicate<DocId> docFilter, int limit, int scoringFunctionIndex) {
    	return res;
    }

}
