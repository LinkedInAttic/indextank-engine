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

package com.flaptor.indextank.api.resources;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.flaptor.indextank.api.IndexEngineApi;
import com.flaptor.indextank.api.util.QueryHelper;
import com.flaptor.indextank.rpc.CategoryFilter;
import com.flaptor.indextank.rpc.IndextankException;
import com.flaptor.indextank.rpc.InvalidQueryException;
import com.flaptor.indextank.rpc.MissingQueryVariableException;
import com.flaptor.indextank.rpc.RangeFilter;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.ghosthack.turismo.action.Action;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public class Search extends Action {

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        IndexEngineApi api = (IndexEngineApi) ctx().getAttribute("api");

        String q = params("q");
        int start = QueryHelper.parseIntParam(params("start"), 0);
        int len = QueryHelper.parseIntParam(params("end"), 10);
        int function = QueryHelper.parseIntParam(params("function"), 0);
        Map<Integer, Double> vars = Maps.newHashMap();
        List<CategoryFilter> facetFilters = Lists.newArrayList();
        List<RangeFilter> variableRangeFilters = Lists.newArrayList();
        List<RangeFilter> functionRangeFilters = Lists.newArrayList();
        Map<String, String> extras = Maps.newHashMap();
        extras.put("fetch_variables", "*");
        extras.put("fetch_categories", "*");
        extras.put("fetch_fields", "*");
        //extras.put("snippet_fields", "*");
        //extras.put("snippet_type", "*");

        try {
            long t0 = System.currentTimeMillis();
            SearchResults results = api.search(q, start, len, function, vars, facetFilters, variableRangeFilters, functionRangeFilters, extras);
            long t1 = System.currentTimeMillis();
            double searchTime = (t1 - t0) / 1000;
            
            JSONArray ja = new JSONArray();
            for(SearchResult result: results.getResults()) {
                JSONObject document = new JSONObject();
                document.putAll(result.getFields());
                document.put("docid", result.getDocId());
                document.put("query_relevance_score", result.getScore());
                for(Entry<Integer, Double> entry: result.getVariables().entrySet()) {
                    document.put("variable_" + entry.getKey(), entry.getValue());
                }
                for(Entry<String, String> entry: result.getCategories().entrySet()) {
                    document.put("category_" + entry.getKey(), entry.getValue());
                }
                ja.add(document);
            }
            
            JSONObject jo = new JSONObject();
            jo.put("query", q);
            jo.put("results", ja);
            jo.put("matches", results.getMatches());
            jo.put("facets", toFacets(results.getFacets()));
            String didYouMean = results.getDidYouMean();
            if(didYouMean != null) {
                jo.put("didyoumean", didYouMean);
            }
            jo.put("search_time", String.format("%.3f", searchTime));
            
            print(jo.toJSONString());
            return;

        } catch (IndextankException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidQueryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingQueryVariableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        res().setStatus(503);
        print("Service unavailable"); // TODO: descriptive error msg
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Integer>> toFacets(Map<String, Multiset<String>> facets) {
        JSONObject results = new JSONObject();
        for (Entry<String, Multiset<String>> entry : facets.entrySet()) {
            JSONObject value = new JSONObject();
            for (String catValue : entry.getValue()) {
                value.put(catValue, entry.getValue().count(catValue));
            }
            results.put(entry.getKey(), value);
        }
        return results;
    }

    private static final Logger LOG = Logger.getLogger(Search.class.getName());
    private static final boolean LOG_ENABLED = true;

}
