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

package com.flaptor.indextank.storage.alternatives;

import java.util.Map;

import com.flaptor.indextank.index.BoostedDocument;
import com.flaptor.indextank.index.Document;
import com.flaptor.util.Pair;

/**
 * Access to the storage of a singular Index. 
 * When performing a read to the storage, it is possible (always depending on the underlying
 * implementation) to specify if a consistent read is required. When consistency is required,
 * the read guarantees that any writes already confirmed to the writer will be fully seen by this observer.
 * 
 * @author iperez
 *
 */
public interface IndexStorage {
	/**
	 * Retrieve a document by its id
	 * 
	 * @param docId The document id
	 * @param requireConsistency <code>true</code> if the read needs to be consistent
	 * @return an IndexTank Document
	 */
	public BoostedDocument getDocument(String docId, boolean requireConsistency);

	/**
	 * Retrieve a document by its id
	 * 
	 * @param docId The document id
	 * @return an IndexTank Document
	 */
	public BoostedDocument getDocument(String docId);

	/**
	 * Retrieve a specific subset of documents by their doc ids
	 * 
	 * @param docIds the docIds of the docs to retrieve
	 * @param requireConsistency <code>true</code> if the read needs to be consistent
	 * @return a Map from doc ids to IndexTank Documents
	 */
	public Map<String, BoostedDocument> getDocuments(Iterable<String> docIds, boolean requireConsistency);
	
	/**
	 * Retrieve a specific subset of documents by their doc ids
	 * 
	 * @param docIds the docIds of the docs to retrieve
	 * @return a Map from doc ids to IndexTank Documents
	 */
	public Map<String, BoostedDocument> getDocuments(Iterable<String> docIds);

	/**
	 * Retrieve all the documents for this Index
	 * 
	 * @param requireConsistency <code>true</code> if the read needs to be consistent
	 * @return an Iterable of IndexTank Documents
	 */
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments(boolean requireConsistency);

	/**
	 * Retrieve all the documents for this Index back to a timestamp
	 *
	 * @param requireConsistency <code>true</code> if the read needs to be consistent
	 * @param backTo timestamp of the oldest document that needs to be restored
	 * @return an Iterable of IndexTank Documents
	 */
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments(boolean requireConsistency, long backTo);

	/**
	 * Retrieve all the documents for this Index
	 * 
	 * @return an Iterable of IndexTank Documents
	 */
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments();

    /**
	 * Retrieve all the documents for this Index back to a timestamp
	 *
	 * @param backTo timestamp of the oldest document that needs to be restored
	 * @return an Iterable of IndexTank Documents
	 */
	public Iterable<Pair<String, BoostedDocument>> getAllDocuments(long backTo);
	
	/**
	 * Store a new document or update an existing one (depending on whether the sourceDocId already
	 * exists in the storage or not).  
	 * 
	 * @param sourceDocId the source's document identifier 
	 * @param document the document to store
	 * @param timestamp the document's creation timestamp (as used for boosting)
	 * @param boosts the document's boosts
	 * @param asynchronous true if the call to the storage must be non-blocking
	 * @return the document identifier for the storage
	 */
	public String putDocument(String sourceDocId, Document document, int timestamp, Map<Integer, Float> boosts, boolean asynchronous);

	/**
	 * Update the document's creation timestamp
	 * 
	 * @param timestamp the document's creation timestamp (as used for boosting) 
	 */
	public void updateTimestamp(String sourceDocId, int timestamp);
	
	/**
	 * Update the document's boosts' values
	 * 
	 * @param boosts the document's boosts to be updated (all unspecified boosts will remain as they were)
	 */
	public void updateBoosts(String sourceDocId, Map<Integer, Float> boosts);
	
	/**
	 * Update the document's categories' values
	 *
	 * @param boosts the document's categories to be updated (all unspecified categories will remain as they were)
	 */
	public void updateCategories(String sourceDocId, Map<String, String> categories);

	/**
	 * Completely remove a document from storage.
	 * 
	 * @param sourceDocId the source's document identifier
	 * @param asynchronous true if the call to the storage must be non-blocking
	 */
	public void removeDocument(String sourceDocId, boolean asynchronous);
}
