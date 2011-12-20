package com.flaptor.indextank;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.storage.alternatives.DocumentStorage;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class DocumentStoringIndexer implements BoostingIndexer {
    //private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final BoostingIndexer delegate;
    private DocumentStorage storage;

    public DocumentStoringIndexer(BoostingIndexer delegate, DocumentStorage storage){
        Preconditions.checkNotNull(delegate);
        this.delegate = delegate;
        this.storage = storage;
    }

    @Override
    public void dump() throws IOException {
        storage.dump();
        delegate.dump();
    }

	@Override
	public void del(String docid) {
		this.delegate.del(docid);
		this.storage.deleteDocument(docid);
	}

    @Override
    public void promoteResult(String docid, String query) {
        this.delegate.promoteResult(docid, query);
    }

	@Override
	public void add(String docId, Document document, int timestampBoost, Map<Integer, Double> dynamicBoosts) {
		this.storage.saveDocument(docId, document);
		this.delegate.add(docId, document, timestampBoost, dynamicBoosts);
	}

	@Override
	public void updateBoosts(String docId, Map<Integer, Double> updatedBoosts) {
		this.delegate.updateBoosts(docId, updatedBoosts);		
	}

	@Override
	public void updateTimestamp(String docId, int timestampBoost) {
		this.delegate.updateTimestamp(docId, timestampBoost);		
	}
	
	@Override
	public void updateCategories(String docId, Map<String, String> categories) {
		this.delegate.updateCategories(docId, categories);
	}

    @Override
	public void addScoreFunction(int functionIndex, String definition) throws Exception {
		this.delegate.addScoreFunction(functionIndex, definition);		
	}
    
    @Override
	public void removeScoreFunction(int functionIndex) {
		this.delegate.removeScoreFunction(functionIndex);		
	}
    
    @Override
	public Map<Integer,String> listScoreFunctions() {
		return this.delegate.listScoreFunctions();		
	}

    @Override
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.putAll(this.delegate.getStats());
        stats.putAll(this.storage.getStats());
        return stats;
    }


}
