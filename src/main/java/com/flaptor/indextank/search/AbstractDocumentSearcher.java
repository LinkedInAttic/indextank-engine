package com.flaptor.indextank.search;

import java.util.Collections;

import com.flaptor.indextank.query.Query;

public abstract class AbstractDocumentSearcher implements DocumentSearcher {

	@Override
	public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex) throws InterruptedException {
		return search(query, start, limit, scoringFunctionIndex, Collections.<String,String>emptyMap());
	}

}
