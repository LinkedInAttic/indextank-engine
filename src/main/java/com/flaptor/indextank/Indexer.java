package com.flaptor.indextank;

import com.flaptor.indextank.index.Document;

/**
 * Device that handles the inclusion of new documents, and the update and removal of existing ones
 * to an Index. 
 * 
 * @author iperez
 *
 */
public interface Indexer {
	/**
	 * Add a new {@link Document} to the Index or update an existing one
	 * 
	 * @param docId external (customer) identifier of the document
	 * @param document the {@link Document} to add
	 */
    public abstract void add(String docId, Document document);
    
	/**
	 * Remove an existing {@link Document}
	 * 
	 * @param docId external (customer) identifier of the document
	 */
    public abstract void del(String docId);

}
