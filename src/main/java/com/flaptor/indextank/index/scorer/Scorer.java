package com.flaptor.indextank.index.scorer;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;

public interface Scorer {

	/**
	 * Scores a document.
	 * Calculates the score of a document given its textual score and  it's documentId (to
	 * retrieve boost values).
	 * In the case that no boost values for this documentId are stored, the textual score is
	 * returned.
	 * @param documentId document identifier
	 * @param textualScore already calculated textual score
	 * @param functionIndex index of the function to be used for the scoring
	 * 
	 * @return the final score for this document.
	 */
	public double scoreDocument(DocId documentId, double textualScore, int now, QueryVariables queryVars, Integer functionIndex);
	
	/**
	 * Add a scoring function for the given index.
	 * 
	 * @param functionIndex scoring function index 
	 * @param function scoring function
	 */
	public void putScoringFunction(Integer functionIndex, ScoreFunction function);

    public void removeScoringFunction(Integer functionIndex);

}