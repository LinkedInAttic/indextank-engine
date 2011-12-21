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
