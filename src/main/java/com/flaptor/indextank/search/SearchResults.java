package com.flaptor.indextank.search;

import java.util.Map;

import com.google.common.collect.Multiset;

public class SearchResults {
    private int matches;
    private Iterable<SearchResult> results;
    private final Map<String, Multiset<String>> facets;
    private final String didYouMean;

    public SearchResults(Iterable<SearchResult> results, int matches, Map<String, Multiset<String>> facets){
        this(results, matches, facets, null);
    }

    public SearchResults(Iterable<SearchResult> results, int matches, Map<String, Multiset<String>> facets, String didYouMean){
        this.matches = matches;
        this.results = results;
		this.facets = facets;
        this.didYouMean = didYouMean;
    }

	public int getMatches(){
        return this.matches;
    }

	public Iterable<SearchResult> getResults(){
    	return this.results;
    }

	public Map<String, Multiset<String>> getFacets() {
		return facets;
	}

    public String getDidYouMean(){
        return this.didYouMean;
    }
}
