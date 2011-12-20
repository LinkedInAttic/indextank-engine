package com.flaptor.indextank.storage.alternatives;

import java.io.IOException;
import java.util.Map;

import com.flaptor.indextank.index.Document;

public interface DocumentStorage {
	public Document getDocument(String docId);
	public void saveDocument(String docId, Document document);
	public void deleteDocument(String docId);
    public void dump() throws IOException;
    public Map<String, String> getStats();
}
