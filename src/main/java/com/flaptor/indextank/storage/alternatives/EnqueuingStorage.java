package com.flaptor.indextank.storage.alternatives;

import java.util.Map;

import com.flaptor.indextank.index.Document;

public interface EnqueuingStorage {
	public void enqueueAddDocument(String indexId, final String sourceDocId, final Document document, final int timestamp, final Map<Integer, Float> boosts);
	public void enqueueUpdateBoosts(String indexId, final String sourceDocId, final Map<Integer, Float> boosts);
	public void enqueueUpdateTimestamp(String indexId, final String sourceDocId, final int timestamp);
	public void enqueueUpdateCategories(String indexId, final String sourceDocId, final Map<String, String> categories);
	public void enqueueRemoveDocument(String indexId, final String sourceDocId);
}