package com.flaptor.indextank.index.scorer;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;

public class MockScorer implements Scorer {

	@Override
	public void putScoringFunction(Integer functionIndex, ScoreFunction function) {
	}
	
    @Override
	public void removeScoringFunction(Integer functionIndex) {
	}

	@Override
	public double scoreDocument(DocId documentId, double textualScore, int now, QueryVariables queryVars, Integer functionIndex) {
		return textualScore;
	}

}
