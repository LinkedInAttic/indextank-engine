package com.flaptor.indextank.index.scorer;

import java.util.Collection;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.scorer.DynamicDataManager.DynamicData;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.util.Pair;
import com.google.common.collect.Multimap;

public class VariablesRangeFilter implements MatchFilter {
	private DynamicDataManager dynamicDataManager;
	/**
	 * The ranges apply considering both ends as inclusive and using OR to
	 * join filters for the same variable and AND for different variables. 
	 */
	private Multimap<Integer, Pair<Float, Float>> ranges; 
	public static MatchFilter NO_FILTER = new NoneMatchFilter();
	
	
	public VariablesRangeFilter(DynamicDataManager dynamicDataManager,	Multimap<Integer, Pair<Float, Float>> ranges) {
		this.dynamicDataManager = dynamicDataManager;
		this.ranges = ranges;
	}

	@Override
	public boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars) {
		DynamicData dynamicData = dynamicDataManager.getDynamicData(documentId);
		for (Integer variable : ranges.keySet()) {
			if (!checkVariable(dynamicData.getBoost(variable), ranges.get(variable))) {
				return false;
			}
		}
		return true;
	}

	private boolean checkVariable(Float value, Collection<Pair<Float, Float>> ranges) {
		for (Pair<Float, Float> range : ranges) {
			if ((range.first() == null || value >= range.first()) && (range.last() == null || value <= range.last())) {
				return true;
			}
		}
		return false;
	}
}

