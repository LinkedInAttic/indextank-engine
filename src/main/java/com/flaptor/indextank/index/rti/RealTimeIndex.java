package com.flaptor.indextank.index.rti;

import java.util.Map;

import com.flaptor.indextank.Indexer;
import com.flaptor.indextank.blender.BlendingQueryMatcher;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.rti.inverted.InvertedIndex;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.query.IndexEngineParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 *
 * @author Flaptor Team
 */
public class RealTimeIndex implements Indexer {

	InvertedIndex markedIndex;
    InvertedIndex index;
    Scorer scorer;
	private final int rtiSize;
	private final IndexEngineParser parser;
	private final FacetingManager facetingManager;

    public RealTimeIndex(Scorer scorer, IndexEngineParser parser, int rtiSize, FacetingManager facetingManager) {
        this.parser = parser;
		this.facetingManager = facetingManager;
		Preconditions.checkNotNull(scorer);
        Preconditions.checkArgument(rtiSize > 0);
        this.scorer = scorer;
		this.rtiSize = rtiSize;
        this.index = new InvertedIndex(scorer, parser, rtiSize, this.facetingManager);
        this.markedIndex = null;
    }

    public void add(String docid, Document doc) {
        index.add(docid, doc);
    }

    public void del(String docid) {
        index.del(docid);
    }

    private QueryMatcher getMergedSearcher() {
    	QueryMatcher matcher = index;
    	QueryMatcher markedMatcher = markedIndex;
    	if (markedMatcher != null) {
    		return new BlendingQueryMatcher(markedMatcher, matcher);
    	} else {
    		return matcher;
    	}
    }
    
    public QueryMatcher getSearchSession() {
    	return getMergedSearcher();
    }
    
    public void mark() {
    	synchronized (this) {
    		Preconditions.checkState(markedIndex == null, "Cannot mark twice. clearToMark should be called before marking again.");
    		markedIndex = index; 
    		index = new InvertedIndex(scorer, parser, rtiSize, facetingManager);
    	}
    }
    
    public void clearToMark() {
    	synchronized (this) {
    		Preconditions.checkState(markedIndex != null, "Mark not found. It was either never marked or already cleared.");
    		markedIndex = null; 
    	}
    }

    public Map<String, String> getStats() {
        Map<String, String> stats = Maps.newHashMap();
        InvertedIndex current = index;
        InvertedIndex marked = markedIndex;
        stats.put("rti_size", String.valueOf(rtiSize));
        stats.putAll(current.getStats("rti_current_index_"));
        if (marked != null) {
            stats.putAll(current.getStats("rti_marked_index_"));
        }
        return stats;
    }
    
}
