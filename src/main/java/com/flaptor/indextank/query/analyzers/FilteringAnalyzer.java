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

package com.flaptor.indextank.query.analyzers;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import com.google.common.collect.Lists;

public class FilteringAnalyzer extends Analyzer {
	private List<AnalyzerFilter> filters = Lists.newArrayList();
	private final Analyzer original;
	
	/**
	 * Constructs the Analyzer's filter given its configuration. Will only consider a
	 * "filters" key in the root object. The value for this key has to be a list
	 * of maps each one with the keys "factory" (string) and "configuration" (map).
	 * Factory needs to be a class name for a class with a static method:
	 * AnalyzerFilter buildFilter(Map) 
	 * 
	 * @param filtersConfiguration 
	 */
	public FilteringAnalyzer(Analyzer original, Map<Object, Object> analyzerConfiguration) {
		this.original = original;
		String key = "filters";
		if (analyzerConfiguration.containsKey(key)) {
			List<Map<Object, Object>> filtersConfigurations;
			try {
				filtersConfigurations = (List<Map<Object, Object>>) analyzerConfiguration.get(key);
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("Value for 'filters' key should be a List<Map>", e);
			}
			
			for (Map<Object, Object> filter : filtersConfigurations) {
				String factoryClassString;
				Map<Object, Object> factoryConfiguration;
				try {
					factoryClassString = (String) filter.get("factory");
					factoryConfiguration = (Map<Object, Object>) filter.get("configuration");
				} catch (ClassCastException e) {
					throw new IllegalArgumentException("Value for 'factory' should be a String with the factory class name, 'configuration' should be a map.", e);
				}
				
				try {
					Class<?> factoryClass = Class.forName(factoryClassString);

					Method method = factoryClass.getMethod("buildFilter", new Class[] {Map.class});
					
					AnalyzerFilter tokenFilter = (AnalyzerFilter) method.invoke(null, factoryConfiguration);

					filters.add(tokenFilter);
					
				} catch (ClassCastException e) {
					throw new RuntimeException("Filter factory method does not return an AnalyzerFilter", e);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Filter factory class not found", e);
				} catch (SecurityException e) {
					throw new RuntimeException("Filter factory class not instantiable", e);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Filter factory class does not have the required static method buildFilter", e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("Filter factory class does not have the required static method buildFilter", e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Filter factory class does not have the required static method buildFilter or it is not accessible", e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException("Filter factory class threw an exception for the give configuration", e);
				}
			}
		}
	}
	
	@Override
	public final TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = analyzePreFilters(fieldName, reader);
		
		for (AnalyzerFilter filter : filters) {
			result = filter.filter(result);
		}
		
		return result;
	}

	private TokenStream analyzePreFilters(String fieldName, Reader reader) {
		return original.tokenStream(fieldName, reader);
	}

}
