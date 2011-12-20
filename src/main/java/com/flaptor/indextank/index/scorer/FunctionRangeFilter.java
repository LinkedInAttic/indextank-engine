package com.flaptor.indextank.index.scorer;

import java.util.Collection;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.util.Pair;
import com.google.common.collect.Multimap;

public class FunctionRangeFilter implements MatchFilter {
	/**
	 * The ranges apply considering both ends as inclusive and using OR to
	 * join filters for the same variable and AND for different variables. 
	 */
	private Multimap<Integer, Pair<Float, Float>> ranges; 
	public static MatchFilter NO_FILTER = new NoneMatchFilter();
	private final Scorer scorer;
	private DynamicDataManager dynamicDataManager;
	
	
	public FunctionRangeFilter(Scorer scorer, DynamicDataManager dynamicDataManager, Multimap<Integer, Pair<Float, Float>> ranges) {
		this.scorer = scorer;
		this.dynamicDataManager = dynamicDataManager;
		this.ranges = ranges;
	}

	@Override
	public boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars) {
		for (Integer functionIndex : ranges.keySet()) {
			double value = scorer.scoreDocument(documentId, textualScore, now, queryVars, functionIndex);
			if (!checkValue(value, ranges.get(functionIndex))) {
				return false;
			}
		}
		return true;
	}

	private boolean checkValue(double value, Collection<Pair<Float, Float>> ranges) {
		for (Pair<Float, Float> range : ranges) {
			if ((range.first() == null || value >= range.first()) && (range.last() == null || value <= range.last())) {
				return true;
			}
		}
		return false;
	}
}

