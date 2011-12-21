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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.scorer.CategoryMaskManager.CategoryInfo;
import com.flaptor.indextank.index.scorer.DynamicDataManager.DynamicData;
import com.flaptor.indextank.index.scorer.DynamicDataManager.FacetsCollector;
import com.flaptor.indextank.query.QueryVariables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.ObjectArrays;

public class DynamicDataFacetingManager extends FacetingManager {
	private final DynamicDataManager dynamicDataManager;
	
	public class DynamicDataFacetFilter implements MatchFilter {
		private List<CategoryFilter> matchingCategories = new ArrayList<CategoryFilter>();
		private int dataOffset;
		//TODO: optimize to avoid searching the index when true
		private boolean isNoneMatchingFilter = false;
		
		private class CategoryFilter {
			public int[] bitmask;
			public int[][] values;
			
			public CategoryFilter(int[] bitmask, int[][] values) {
				this.bitmask = bitmask;
				this.values = values;
			}
		}
		
		public DynamicDataFacetFilter(Multimap<String, String> filteringFacets) {
			CategoryMaskManager maskManager = dynamicDataManager.getMaskManager();
			int maxMaskSize = maskManager.getMaxMaskSize();
			dataOffset = dynamicDataManager.getNumberOfBoosts() + 1;
			
			if (maxMaskSize == 0) {
				if (filteringFacets.size() == 0) {
					return;
				} else {
					isNoneMatchingFilter = true;
					return;
					//throw new IllegalArgumentException("Facets can't be filtered if no facets are defined");
				}
			}
			
			if (filteringFacets.values().size() == filteringFacets.keySet().size()) {
				// Only one value per category optimization
				int[] totalMask = new int[maxMaskSize];
				int[] totalValue = new int[maxMaskSize];
				
				for (Entry<String, String> entry : filteringFacets.entries()) {
					CategoryInfo categoryInfo = maskManager.getCategoryInfos().get(entry.getKey());

					if (categoryInfo == null) {
						isNoneMatchingFilter = true;
						return;
					}

					int[] bitmask = categoryInfo.getBitmask();
					Integer valueCode = categoryInfo.getValueCode(entry.getValue());
					
					if (valueCode == null) {
						isNoneMatchingFilter = true;
						return;
					}
					
					CategoryEncoder.encode(totalValue, 0, bitmask, valueCode);
					
					orArrays(bitmask, totalMask);
				}
				
				matchingCategories.add(new CategoryFilter(totalMask, new int[][] { totalValue } ));
			} else {
				
				for (String categoryKey : filteringFacets.keySet()) {
					CategoryInfo categoryInfo = maskManager.getCategoryInfos().get(categoryKey);
					if (categoryInfo == null) {
						isNoneMatchingFilter = true;
						return;
					}
					int[] bitmask = categoryInfo.getBitmask();

					Collection<String> values = filteringFacets.get(categoryKey);
					List<int[]> valuesBitmaps = Lists.newArrayList();
					
					for (String value : values) {
						Integer valueCode = categoryInfo.getValueCode(value);
						if (valueCode != null) {
							int[] encodedValue = CategoryEncoder.encode(new int[0], 0, bitmask, valueCode);
							valuesBitmaps.add(encodedValue);
						}
					}
					
					if (valuesBitmaps.isEmpty()) {
						isNoneMatchingFilter = true;
						return;
					}
					
					matchingCategories.add(new CategoryFilter(bitmask, valuesBitmaps.toArray(new int[valuesBitmaps.size()][])));
				}
			}
		}
		
		private void orArrays(int[] source, int[] target) {
			for (int i = 0; i < source.length; i++) {
				target[i] |= source[i];
			}
		}
		
		@Override
		public boolean matches(DocId documentId, double textualScore, int now, QueryVariables queryVars) {
			if (isNoneMatchingFilter) {
				return false;
			}
			
			DynamicData dynamicData = dynamicDataManager.getDynamicData(documentId);
			
			int[] data = dynamicData.getData();
			
			/*
			 * For every category we checked that at least one of the
			 * values matches.
			 */
			for (CategoryFilter categoryFilter : matchingCategories) {
				int[][] values = categoryFilter.values;
				boolean matched = false;
				
				for (int i = 0; i < values.length; i++) {
					if (CategoryEncoder.matches(data, dataOffset, categoryFilter.bitmask, values[i])) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					return false;
				}
			}
			
			return true;
		}
		
	}
	
	public DynamicDataFacetingManager(DynamicDataManager dynamicDataManager) {
		this.dynamicDataManager = dynamicDataManager;
	}

	public class DynamicDataFaceter implements Faceter, FacetsCollector {
		private Map<String, int[]> rawFacets = Maps.newHashMap();
		
		@Override
		public void computeDocument(DocId documentId) {
			dynamicDataManager.populateCollector(documentId, this);
		}

		@Override
		public Map<String, Multiset<String>> getFacets() {
			CategoryMaskManager maskManager = dynamicDataManager.getMaskManager();
			Map<String, Multiset<String>> results = Maps.newHashMap();
			
			for (Entry<String, int[]> entry : rawFacets.entrySet()) {
				Multiset<String> counts = HashMultiset.create();
				String category = entry.getKey();
				CategoryInfo categoryInfo = maskManager.getCategoryInfos().get(category);					
				int[] rawCounts = entry.getValue();
				for (int i = 0; i < rawCounts.length; i++) {
					String value = categoryInfo.getValue(i+1);
					counts.add(value, rawCounts[i]);
				}
				results.put(category, counts);
			}
			
			return results;
		}

		@Override
		public void addCategoryValue(String category, Integer valueCode) {
			int[] categorySet = rawFacets.get(category);
			if (categorySet == null) {
				categorySet = new int[dynamicDataManager.getMaskManager().getCategoryInfos().get(category).getSize()];
				rawFacets.put(category, categorySet);
			}
			categorySet[valueCode - 1]++;
		}
		
	}
	
	@Override
	public Faceter createFaceter() {
		return new DynamicDataFaceter();
	}

	@Override
	public MatchFilter getFacetFilter(Multimap<String, String> filteringFacets) {
		return new DynamicDataFacetFilter(filteringFacets);
	}
	
}

