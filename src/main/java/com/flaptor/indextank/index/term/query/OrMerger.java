package com.flaptor.indextank.index.term.query;

import java.util.List;

import com.flaptor.indextank.util.IdentityUnion;
import com.flaptor.indextank.util.SkippableIterable;

class OrMerger extends IdentityUnion<RawMatch> {

    @SuppressWarnings("unchecked")
	OrMerger(SkippableIterable<RawMatch> left, SkippableIterable<RawMatch> right) {
        super(left, right);
    }

    @Override
    protected boolean shouldUse(RawMatch result, List<RawMatch> originals) {
        double score = 0.0;
        for (RawMatch p : originals) {
            score += p.getScore();
        }
        result.setScore(score);
        
        return true; // every element of the union should be returned
    }
}
