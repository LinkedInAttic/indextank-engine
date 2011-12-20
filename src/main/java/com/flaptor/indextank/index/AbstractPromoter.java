package com.flaptor.indextank.index;

import com.flaptor.indextank.index.results.SimpleScoredDocIds;
import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public abstract class AbstractPromoter implements Promoter {

	@Override
	public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) {
		String queryStr = query.getOriginalStr();
		String docId = null;
		if (queryStr != null) {
			docId = this.getPromotedDocId(queryStr.toLowerCase());
		}
		if (docId != null) {
			ScoredMatch match = new ScoredMatch(Double.MAX_VALUE, new DocId(docId));
			return new SimpleScoredDocIds(
			        ImmutableList.of(match), limit, 1, Maps.<String, Multiset<String>>newHashMap());
		}
		return new SimpleScoredDocIds(ImmutableList.<ScoredMatch>of(), limit, 0, Maps.<String, Multiset<String>>newHashMap());
	}
	
	@Override
	public TopMatches findMatches(Query query, Predicate<DocId> idFilter, int limit, int scoringFunctionIndex) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean hasChanges(DocId docid) {
		return false;
	}
	
	@Override
	public int countMatches(Query query) {
	    return 0;
	}
	
	@Override
	public int countMatches(Query query, Predicate<DocId> idFilter) {
	    return 0;
	}
	
}
