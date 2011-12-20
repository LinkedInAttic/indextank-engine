package com.flaptor.indextank.index.scorer;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;

public class NoneMatchFilter implements MatchFilter {
	@Override
	public boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars) {
		return true;
	}
}
