package com.flaptor.indextank.index.scorer;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;

public interface MatchFilter {
	boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars);
}
