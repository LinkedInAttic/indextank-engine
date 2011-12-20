package com.flaptor.indextank.storage.alternatives;

import java.util.Map;

import com.flaptor.indextank.index.BoostedDocument;
import com.flaptor.util.Pair;

/**
 * Abstract implementation of IndexStorage that takes care of the defaults for the <code>requireConsistency</code> 
 * parameters (setting them as <code>false</code>
 * 
 * @author iperez
 *
 */
public abstract class AbstractIndexStorage implements IndexStorage {

	@Override
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments() {
		return getAllDocuments(false);
	}

	@Override
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments(long backTo) {
		return getAllDocuments(false, backTo);
	}

	@Override
	public BoostedDocument getDocument(String docId) {
		return getDocument(docId, false);
	}

	@Override
	public Map<String, BoostedDocument> getDocuments(Iterable<String> docIds) {
		return getDocuments(docIds, false);
	}

}
