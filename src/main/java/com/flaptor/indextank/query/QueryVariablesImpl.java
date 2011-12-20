package com.flaptor.indextank.query;

import java.util.Map;
import java.util.Map.Entry;

public final class QueryVariablesImpl implements QueryVariables {
    private final Double[] vars;
    
	public QueryVariablesImpl(Double[] vars) {
        this.vars = vars;
    }
    public double getValue(int varIndex) {
        if (varIndex >= vars.length || vars[varIndex] == null)
            throw new NoSuchQueryVariableException("Query variable " + varIndex + " is not defined in this query.", varIndex);
            
        return vars[varIndex];
    }
	public int getVariablesCount() {
	    return vars.length;
	}
	
    public static QueryVariables fromMap(Map<Integer, Double> map) {
        int maxIdx = -1;
        for (Integer idx : map.keySet()) {
            maxIdx = Math.max(maxIdx, idx);
        }
        Double[] vars = new Double[maxIdx+1];
        for (Entry<Integer, Double> e : map.entrySet()) {
            vars[e.getKey()] = e.getValue();
        }
        return new QueryVariablesImpl(vars);
    }
}
