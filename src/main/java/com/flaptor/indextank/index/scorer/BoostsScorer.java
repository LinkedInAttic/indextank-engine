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

import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;

public class BoostsScorer implements Scorer {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	private final BoostsManager boostsManager;
	private final Map<Integer, ScoreFunction> scoringFunctions;

    public BoostsScorer(BoostsManager boostsManager, Map<Integer, ScoreFunction> scoringFunctions) {
        Preconditions.checkNotNull(scoringFunctions);
        this.boostsManager = boostsManager;
        this.scoringFunctions = scoringFunctions;
    }

	@Override
	public void putScoringFunction(Integer functionIndex, ScoreFunction function) {
		scoringFunctions.put(functionIndex, function);
	}

	@Override
    public double scoreDocument(DocId documentId, double textualScore, int now, QueryVariables queryVars, Integer functionIndex) {
        Boosts docVars = boostsManager.getBoosts(documentId);
        int age = now - docVars.getTimestamp();
        ScoreFunction scoreFunction = scoringFunctions.get(functionIndex);
        if (scoreFunction == null) {
            return -age;
        }
        return scoreFunction.score(textualScore, age, docVars, queryVars);
    }

    @Override
    public void removeScoringFunction(Integer functionIndex) {
        scoringFunctions.remove(functionIndex);
    }

}
