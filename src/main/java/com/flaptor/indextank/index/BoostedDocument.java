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

package com.flaptor.indextank.index;

import java.util.Map;


public class BoostedDocument {
	private Document document;
	private int timestamp;
	private Map<Integer, Float> boosts;
    private Map<String, String> categories;

	public BoostedDocument(Document document, int timestamp, Map<Integer, Float> boosts, Map<String, String> categories) {
		this.document = document;
		this.timestamp = timestamp;
		this.boosts = boosts;
        this.categories = categories;
	}

	public Document getDocument() {
		return document;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public Map<Integer, Float> getBoosts() {
		return boosts;
	}

    public Map<String, String> getCategories() {
        return categories;
    }

}
