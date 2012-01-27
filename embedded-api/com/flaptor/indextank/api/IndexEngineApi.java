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

package com.flaptor.indextank.api;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.flaptor.indextank.BoostingIndexer;
import com.flaptor.indextank.api.util.Timestamp;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.scorer.BoostsScorer;
import com.flaptor.indextank.index.scorer.DynamicDataManager;
import com.flaptor.indextank.index.scorer.FunctionRangeFilter;
import com.flaptor.indextank.index.scorer.IntersectionMatchFilter;
import com.flaptor.indextank.index.scorer.MatchFilter;
import com.flaptor.indextank.index.scorer.VariablesRangeFilter;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.NoSuchQueryVariableException;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.indextank.query.QueryVariablesImpl;
import com.flaptor.indextank.rpc.CategoryFilter;
import com.flaptor.indextank.rpc.IndextankException;
import com.flaptor.indextank.rpc.InvalidQueryException;
import com.flaptor.indextank.rpc.MissingQueryVariableException;
import com.flaptor.indextank.rpc.RangeFilter;
import com.flaptor.indextank.search.DocumentSearcher;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.util.Pair;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class IndexEngineApi {

    protected final EmbeddedIndexEngine engine;
    protected final String characterEncoding;

    public IndexEngineApi(EmbeddedIndexEngine engine) {
        this.engine = engine;
        characterEncoding = engine.getCharacterEncoding();
    }

    public SearchResults search(String queryStr, 
            int start, int len, 
            int scoringFunctionIndex, 
            Map<Integer, Double> queryVariables, 
            List<CategoryFilter> facetsFilter, 
            List<RangeFilter> variableRangeFilters, 
            List<RangeFilter> functionRangeFilters, 
            Map<String,String> extraParameters) throws IndexEngineApiException {
        DocumentSearcher searcher = engine.getSearcher();
        try { 
            Query query = generateQuery(queryStr, start, len, 
                    QueryVariablesImpl.fromMap(queryVariables), 
                    convertToMultimap(facetsFilter), 
                    new IntersectionMatchFilter(
                            convertToVariableRangeFilter(variableRangeFilters), 
                            convertToFunctionRangeFilter(functionRangeFilters)
                    )
            );
            SearchResults search = searcher.search(query, start, len, scoringFunctionIndex, extraParameters);
            return search;
        } catch (NoSuchQueryVariableException e) {
            throw new IndexEngineApiException("Missing query variable with index '" + e.getMissingVariableIndex() + "'", e);
        } catch (ParseException e) {
            throw new IndexEngineApiException("Invalid query", e);
        } catch (RuntimeException e) {
            throw new IndexEngineApiException(e);
        } catch (InterruptedException e) {
            throw new IndexEngineApiException("Interrupted while searching.", e);
        }
    }

    private VariablesRangeFilter convertToVariableRangeFilter(List<RangeFilter> rangeFilters) {
        DynamicDataManager dynamicDataManager = engine.getDynamicDataManager();
        Multimap<Integer, Pair<Float, Float>> filters = HashMultimap.create();
        for (RangeFilter filter : rangeFilters) {
            filters.put(filter.get_key(), new Pair<Float, Float>(filter.is_no_floor() ? null : (float)filter.get_floor(), filter.is_no_ceil() ? null : (float)filter.get_ceil()));
        }
        
        return new VariablesRangeFilter(dynamicDataManager, filters);
    }

    private FunctionRangeFilter convertToFunctionRangeFilter(List<RangeFilter> rangeFilters) {
        DynamicDataManager dynamicDataManager = engine.getDynamicDataManager();
        BoostsScorer scorer = engine.getScorer();
        Multimap<Integer, Pair<Float, Float>> filters = HashMultimap.create();
        for (RangeFilter filter : rangeFilters) {
            filters.put(filter.get_key(), new Pair<Float, Float>(filter.is_no_floor() ? null : (float)filter.get_floor(), filter.is_no_ceil() ? null : (float)filter.get_ceil()));
        }
        
        return new FunctionRangeFilter(scorer, dynamicDataManager, filters);
    }

    private Query generateQuery(String str, int start, int len, QueryVariables vars, Multimap<String, String> facetsFilter, MatchFilter rangeFilters) throws ParseException {
        IndexEngineParser parser = engine.getParser();
        return new Query(parser.parseQuery(str), str, vars, facetsFilter, rangeFilters);
    }

    private Multimap<String, String> convertToMultimap(List<CategoryFilter> facetsFilter) {
        Multimap<String, String> result = HashMultimap.create();
        for (CategoryFilter facetFilter : facetsFilter) {
            result.put(facetFilter.get_category(), facetFilter.get_value());
        }
        
        return result;
    }
    
    public void putDocument(String id, JSONObject fields, JSONObject variables, JSONObject categories) {
        BoostingIndexer indexer = engine.getIndexer();
        indexer.add(id, new Document(prepareProperties(fields)), Timestamp.inSeconds(), prepareBoosts(variables));
        indexer.updateCategories(id, prepareProperties(categories));
    }
    
    public void deleteDocument(String id) {
        BoostingIndexer indexer = engine.getIndexer();
        indexer.del(id);
    }
    
    private Map<String, String> prepareProperties(JSONObject jo) {
        Map<String, String> properties = Maps.newHashMap();
        if(jo == null) {
            return properties;
        }
        for(Object o: jo.entrySet()) {
            @SuppressWarnings("rawtypes")
            Map.Entry e = (Map.Entry) o;
            Object key = e.getKey();
            Object value = e.getValue();
            properties.put(valueOf(key), valueOf(value));
        }
        return properties;
    }

    private Map<Integer, Double> prepareBoosts(JSONObject jo) {
        Map<Integer, Double> dynamicBoosts = Maps.newHashMap();
        if(jo == null) {
            return dynamicBoosts;
        }
        for(Object o: jo.entrySet()) {
            @SuppressWarnings("rawtypes")
            Map.Entry e = (Map.Entry) o;
            Object key = e.getKey();
            Object value = e.getValue();
            dynamicBoosts.put(parseInt(valueOf(key)), parseDouble(valueOf(value)));
        }
        return dynamicBoosts;
    }

    public List<String> complete(String query, String field) {
        return engine.getSuggestor().complete(query, field);
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }
}
