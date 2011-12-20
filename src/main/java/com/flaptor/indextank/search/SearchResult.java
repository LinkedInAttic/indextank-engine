package com.flaptor.indextank.search;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 *
 * @author Flaptor Team
 */
public class SearchResult {

    private double score;
    private String docId;
    private Map<String,String> fields;
    private Map<String, String> categories = Maps.newHashMap();
    private Map<Integer, Double> variables = Maps.newHashMap();

    public SearchResult(double score, String docId) {
    	this.score = score;
    	this.docId = docId;
    	this.fields = Maps.newHashMap();
    }
    
    public SearchResult(double score, String docId, Map<String,String> fields) {
    	this(score, docId);
        this.fields.putAll(fields);
    }

    public double getScore() {
        return score;
    }

    public String getDocId() {
        return docId;
    }

    public Map<String,String> getFields(){
        return fields;
    }
    
    @Override
    public String toString() {
    	return docId;
    }

    public String setField(String name, String value) {
    	return this.fields.put(name, value);
    }
    public String getField(String name) {
    	return this.fields.get(name);
    }
    
    public Map<String, String> getCategories() {
        return categories;
    }
    public void setCategories(Map<String, String> categories) {
        this.categories = categories;
    }
    public Map<Integer, Double> getVariables() {
        return variables;
    }
    public void setVariables(Map<Integer, Double> variables) {
        this.variables = variables;
    }
    
    // TODO field setters and getters?
    // or just update getFields return value ?

}
