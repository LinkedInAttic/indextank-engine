package com.flaptor.indextank;

import java.io.IOException;
import java.util.Map;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.indextank.index.scorer.Scorer;

/**
 * Indexer that handles inclusion, update and removal of Document and the management
 * of dynamic boosts (custom ones and timestamp) 
 * 
 * @author iperez
 *
 */
public interface BoostingIndexer {
	/**
	 * Add a new {@link Document} to the Index or update an existing one.<br> 
	 * When adding a document, its dynamic boosts values must be set. Specifying the values is accomplished by
	 * passing the dynamicBoosts parameter with a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * In this map, no index can be larger than the number of available boosts the {@link IndexEngine}'s {@link Scorer} has, minus one (since it is zero based).
	 * The value for any available boost index not specified in the map is defaulted to zero. 
	 * 
	 * @param docId external (customer) identifier of the document to add
	 * @param document the {@link Document} to add
	 * @param timestampBoost a <code>float</code> representing the time of the document (the younger the document, the larger the boost should be)
	 * @param dynamicBoosts a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * @throws {@link IllegalArgumentException} if an invalid index is passed for a boost   
	 */
	public void add(String docId, Document document, int timestampBoost, Map<Integer, Double> dynamicBoosts);

	/**
	 * Remove a document from the index.
	 * 
	 * @param docId external (customer) identifier of the document to remove
	 */
	public void del(String docId);
	
	/**
	 * Update the special boost for the timestamp
	 * 
	 * @param docId external (customer) identifier of the document
	 * @param timestampBoost a <code>float</code> representing the time of the document (the younger the document, the larger the boost should be)
	 */
	public void updateTimestamp(String docId, int timestampBoost);
	
	/**
	 * Update one or more of the dynamic boosts values.
	 * 
	 * @param docId external (customer) identifier of the document
	 * @param updatedBoosts a Map from the boost index (zero based) to the boost value (a <code>double</code>). No index can be larger than the available boosts the {@link IndexEngine}'s {@link Scorer} has, minus one (since it is zero based)
	 * @throws {@link IllegalArgumentException} if an invalid index is passed for a boost  
	 */
	public void updateBoosts(String docId, Map<Integer, Double> updatedBoosts);

    public void updateCategories(String docId, Map<String, String> categories);
	/**
	 * Promote a document to be the first result for a specific query.
	 * 
	 * @param docId external (customer) identifier of the document
	 * @param query the exact query the document must be promoted to the first result for
	 */
    public void promoteResult(String docId, String query);

    /**
     * Dumps the current state to disk.
     */
    public void dump() throws IOException;

    public void addScoreFunction(int functionIndex, String definition) throws Exception;

    public void removeScoreFunction(int functionIndex);

    public Map<Integer,String> listScoreFunctions();
    
    public Map<String, String> getStats();

}
