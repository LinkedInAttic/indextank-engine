package com.flaptor.indextank.index.term;

import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.term.query.RawMatch;

public interface RawMatchesDecoder {

	public abstract Iterable<ScoredMatch> decode(Iterable<RawMatch> rawMatches, double norm);

}