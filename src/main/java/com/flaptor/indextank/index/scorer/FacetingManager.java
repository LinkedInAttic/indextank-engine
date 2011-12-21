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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public abstract class FacetingManager {
	public abstract Faceter createFaceter();		
	
	public abstract MatchFilter getFacetFilter(Multimap<String, String> filteringFacets);
	
	public static Map<String, Multiset<String>> mergeFacets(Map<String, Multiset<String>> facets1, Map<String, Multiset<String>> facets2) {
		Map<String, Multiset<String>> result = Maps.newHashMap();
		
		Set<String> facets1Cats = facets1.keySet();
		
		for (String category : facets1Cats) {
			Multiset<String> facet1 = HashMultiset.create(facets1.get(category));
			Multiset<String> facet2 = facets2.get(category);
			
			if (facet2 != null) {
				facet1.addAll(facet2);
			}
			result.put(category, facet1);
		}
		
		Set<String> facets2Cats = facets2.keySet();
		
		for (String category : facets2Cats) {
			if (!result.containsKey(category)) {
				result.put(category, HashMultiset.create(facets2.get(category)));
			}
		}
		
		return result;
	}
}
