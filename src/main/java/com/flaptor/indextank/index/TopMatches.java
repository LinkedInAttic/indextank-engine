package com.flaptor.indextank.index;

import java.util.Map;

import com.google.common.collect.Multiset;

public interface TopMatches extends Iterable<ScoredMatch> {
	public int getLimit();
	public int getTotalMatches();
	public Map<String, Multiset<String>> getFacetingResults();
}
