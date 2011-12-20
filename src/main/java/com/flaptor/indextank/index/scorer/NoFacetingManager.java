package com.flaptor.indextank.index.scorer;

import java.util.Map;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class NoFacetingManager extends FacetingManager {

	@Override
	public Faceter createFaceter() {
		return new Faceter() {
			@Override
			public Map<String, Multiset<String>> getFacets() {
				return ImmutableMap.of();
			}
			@Override
			public void computeDocument(DocId documentId) {
			}
		};
	}

	@Override
	public MatchFilter getFacetFilter(Multimap<String, String> facets) {
		return new MatchFilter() {
			@Override
			public boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars) {
				return true;
			}
		};
	}

}
