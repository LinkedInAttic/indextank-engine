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
