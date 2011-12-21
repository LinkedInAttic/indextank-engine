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
