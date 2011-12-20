package com.flaptor.indextank.index.results;

import java.util.Iterator;
import java.util.Map;

import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.google.common.collect.Multiset;

public class SimpleScoredDocIds implements TopMatches {

	private int totalMatches;
	private Iterable<ScoredMatch> delegate;
	private final int limit;
	private Map<String, Multiset<String>> facetingResults;

	public SimpleScoredDocIds(Iterable<ScoredMatch> delegate, int limit, int totalMatches, Map<String, Multiset<String>> facetingResults) {
		this.delegate = delegate;
		this.limit = limit;
		this.totalMatches = totalMatches;
		this.facetingResults = facetingResults;
	}

	@Override
	public int getTotalMatches() {
		return totalMatches;
	}

	@Override
	public int getLimit() {
		return limit;
	}

	@Override
	public Iterator<ScoredMatch> iterator() {
		return delegate.iterator();
	}

	@Override
	public Map<String, Multiset<String>> getFacetingResults() {
		return facetingResults;
	}

	
}
