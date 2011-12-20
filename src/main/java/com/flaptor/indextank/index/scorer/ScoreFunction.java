package com.flaptor.indextank.index.scorer;

import com.flaptor.indextank.query.QueryVariables;


public interface ScoreFunction {
    /**
     * Calculates the score for a document, based only on it's textual score,
     * and additional boost factors.
     */
    public abstract double score(double textualScore, int age, Boosts docVars, QueryVariables queryVars);
}
