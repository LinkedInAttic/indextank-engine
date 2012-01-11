
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
