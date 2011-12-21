/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flaptor.indextank.index.scorer;

import java.io.IOException;
import java.util.Map;

import com.flaptor.indextank.index.DocId;

/**
 * Device that holds special score boosts for every document.
 * Each {@link BoostsManager} is defined with a specific number of 
 * boosts per document. No document can be updated for more boosts
 * than that number
 * 
 * @author iperez
 *
 */
public interface BoostsManager {
	/**
	 * Specify the values of the boosts for a document, including its timestamp.
	 * This is accomplished by passing the dynamicBoosts parameter with a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * In this map, no index can be larger than the number of available boosts the {@link BoostsManager} minus one (since it is zero based).
	 * If no boost was ever set for the document, all non-specified available boost indexes values are defaulted to zero
	 *
	 * @param documentId external (customer) identifier of the document
	 * @param boosts a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * @param timestamp the document timestamp as used for boosting purposes (null if the current timestamp for the document shouldn't be overriden)
	 * @throws {@link IllegalArgumentException} if an invalid index is passed for a boost
	 */
	public void setBoosts(String documentId, Integer timestamp,  Map<Integer, Float> boosts);
	
	/**
	 * Specify the values of the boosts for a document.
	 * This is accomplished by passing the dynamicBoosts parameter with a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * In this map, no index can be larger than the number of available boosts the {@link BoostsManager} minus one (since it is zero based).
	 * If no boost was ever set for the document, all non-specified available boost indexes values are defaulted to zero
	 *
	 * @param documentId external (customer) identifier of the document
	 * @param boosts a Map from the boost index (zero based) to the boost value (a <code>double</code>).
	 * @throws {@link IllegalArgumentException} if an invalid index is passed for a boost
	 */
	public void setBoosts(String documentId, Map<Integer, Float> boosts);
	
	/**
	 * Clean the boosts for a specific document.
	 * 
	 * @param documentId external (customer) identifier of the document
	 */
	public void removeBoosts(String documentId);

	/**
	 * Retrieve the boosts for a specific document 
	 * 
	 * @param documentId external (customer) identifier of the document
	 * @return an array of doubles with the boosts' values
	 */
	public Boosts getBoosts(DocId documentId);
	
	/**
	 * @return the number of document identifiers with boosts associated to them
	 */
	public int getDocumentCount();

    /*
     * Dumps the state of the manager to disk
     */
    public void dump() throws IOException;

    /**
     * Returns a map with docId's document variables
     */
    public Map<Integer, Double> getVariablesAsMap(DocId docId);

    /** 
     * Returns a map with docId's document categories
     */
    public Map<String, String> getCategoryValues(DocId docId);

    /** 
     * Returns a map with docId's document categories (returns empty)
     */
    Map<String, String> getCategoriesAsMap(DocId documentId);

}
