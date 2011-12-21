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
