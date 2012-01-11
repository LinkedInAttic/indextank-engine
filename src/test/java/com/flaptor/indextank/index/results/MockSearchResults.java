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
import java.util.List;
import java.util.Map;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public class MockSearchResults implements TopMatches {

    List<ScoredMatch> results;

    public MockSearchResults() {
        results = Lists.newArrayList();
    }

    public void addResult(double score, String docid) {
        results.add(new ScoredMatch(score, new DocId(docid)));
    }

    public int getTotalMatches() {
        return results.size();
    }

	@Override
	public int getLimit() {
		return results.size();
	}

	@Override
	public Iterator<ScoredMatch> iterator() {
		return results.iterator();
	}

	@Override
	public Map<String, Multiset<String>> getFacetingResults() {
		return Maps.newHashMap();
	}

}
