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

import com.flaptor.indextank.index.Document;

public interface EnqueuingStorage {
	public void enqueueAddDocument(String indexId, final String sourceDocId, final Document document, final int timestamp, final Map<Integer, Float> boosts);
	public void enqueueUpdateBoosts(String indexId, final String sourceDocId, final Map<Integer, Float> boosts);
	public void enqueueUpdateTimestamp(String indexId, final String sourceDocId, final int timestamp);
	public void enqueueUpdateCategories(String indexId, final String sourceDocId, final Map<String, String> categories);
	public void enqueueRemoveDocument(String indexId, final String sourceDocId);
}
