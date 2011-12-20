package com.flaptor.indextank.index.scorer;

import java.util.Arrays;
import java.util.Collection;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;

public class IntersectionMatchFilter implements MatchFilter {
	private Collection<MatchFilter> filters;
	
	public IntersectionMatchFilter(MatchFilter... filters) {
		this(Arrays.asList(filters));
	}
	
	public IntersectionMatchFilter(Collection<MatchFilter> filters) {
		this.filters = filters;
	}

	@Override
	public boolean matches(DocId documentId, double textualScore, int now,
			QueryVariables queryVars) {
		
		for (MatchFilter filter : this.filters) {
			if (!filter.matches(documentId, textualScore, now, queryVars)) {
				return false;
			}
		}
		
		return true;
	}

}
