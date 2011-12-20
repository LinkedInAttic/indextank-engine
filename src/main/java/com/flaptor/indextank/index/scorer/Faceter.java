package com.flaptor.indextank.index.scorer;

import java.util.Map;

import com.flaptor.indextank.index.DocId;
import com.google.common.collect.Multiset;

public interface Faceter {
	public Map<String, Multiset<String>> getFacets();
	public void computeDocument(DocId documentId);
}
