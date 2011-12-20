package com.flaptor.indextank.index.term.query;

import java.util.List;

import com.flaptor.indextank.util.IdentityIntersection;
import com.flaptor.indextank.util.PeekingSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;

class AndMerger extends IdentityIntersection<RawMatch> {
	
    @SuppressWarnings("unchecked")
	AndMerger(SkippableIterable<RawMatch> l, SkippableIterable<RawMatch> r) {
        super(l,r);
    }

    @Override
    protected boolean shouldUse(RawMatch result, List<RawMatch> originals) {
        double score = 1.0;
        for (RawMatch p : originals) {
            score *= p.getScore();
        }
        result.setScore(score);
        return true;
    }
    
    @Override
    protected void advanceTo(PeekingSkippableIterator<RawMatch> it, RawMatch current) {
    	it.skipTo(current.getRawId());
    }
}
