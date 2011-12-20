package com.flaptor.indextank.index;

import java.util.Map;


public class BoostedDocument {
	private Document document;
	private int timestamp;
	private Map<Integer, Float> boosts;
    private Map<String, String> categories;

	public BoostedDocument(Document document, int timestamp, Map<Integer, Float> boosts, Map<String, String> categories) {
		this.document = document;
		this.timestamp = timestamp;
		this.boosts = boosts;
        this.categories = categories;
	}

	public Document getDocument() {
		return document;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public Map<Integer, Float> getBoosts() {
		return boosts;
	}

    public Map<String, String> getCategories() {
        return categories;
    }

}
