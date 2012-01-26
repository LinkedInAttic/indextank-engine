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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.flaptor.indextank.api.IndexEngineApi;
import com.flaptor.indextank.api.IndexEngineApiException;
import com.flaptor.indextank.api.util.QueryHelper;
import com.flaptor.indextank.rpc.CategoryFilter;
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
        HttpServletResponse res = res();

        String characterEncoding = api.getCharacterEncoding();
        try {
            req().setCharacterEncoding(characterEncoding);
            res.setCharacterEncoding(characterEncoding);
            res.setContentType("application/json");
        } catch (UnsupportedEncodingException ignored) {
        }

        String q = params("q");
        String fetchVariables = params("fetch_variables");
        String fetchCategories = params("fetch_categories");
        String fetch = params("fetch");
        String snippet = params("snippet");
        int start = QueryHelper.parseIntParam(params("start"), 0);
        int len = QueryHelper.parseIntParam(params("end"), 10);
        int function = QueryHelper.parseIntParam(params("function"), 0);
        Map<Integer, Double> vars = Maps.newHashMap();
        List<CategoryFilter> facetFilters = Lists.newArrayList();
        List<RangeFilter> variableRangeFilters = Lists.newArrayList();
        List<RangeFilter> functionRangeFilters = Lists.newArrayList();
        Map<String, String> extras = createExtraParameters(fetch, snippet,
                fetchVariables, fetchCategories);

        try {
            long t0 = System.currentTimeMillis();
            SearchResults results = api.search(q, start, len, function, vars, facetFilters, variableRangeFilters, functionRangeFilters, extras);
            long t1 = System.currentTimeMillis();
            double searchTime = (t1 - t0) / 1000;
            int matches = results.getMatches();
            Map<String, Map<String, Integer>> facets = toFacets(results.getFacets());
            String didYouMean = results.getDidYouMean();
            
            JSONArray ja = new JSONArray();
            for(SearchResult result: results.getResults()) {
                addResult(ja, result);
            }

            JSONObject jo = createResponse(q, searchTime, ja, matches, facets, didYouMean);
            
            print(jo.toJSONString());
            return;

        } catch (IndexEngineApiException e) {
            e.printStackTrace();
        }

        res.setStatus(503);
        print("Service unavailable"); // TODO: descriptive error msg
    }

    @SuppressWarnings("unchecked")
    private JSONObject createResponse(String q, double searchTime,
            JSONArray ja, int matches,
            Map<String, Map<String, Integer>> facets, String didYouMean) {
        JSONObject jo = new JSONObject();
        jo.put("query", q);
        jo.put("results", ja);
        jo.put("matches", matches);
        jo.put("facets", facets);
        if(didYouMean != null) {
            jo.put("didyoumean", didYouMean);
        }
        jo.put("search_time", String.format("%.3f", searchTime));
        return jo;
    }

    private Map<String, String> createExtraParameters(String fetch,
            String snippet, String fetchVariables, String fetchCategories) {
        Map<String, String> extras = Maps.newHashMap();
        if("true".equalsIgnoreCase(fetchVariables) || "*".equals(fetchVariables)) {
            if(LOG_ENABLED) LOG.fine("Fetch variables: all");
            extras.put("fetch_variables", "*");
        }
        if("true".equalsIgnoreCase(fetchCategories) || "*".equals(fetchCategories)) {
            if(LOG_ENABLED) LOG.fine("Fetch categories: all");
            extras.put("fetch_categories", "*");
        }
        if(fetch != null) {
            if(LOG_ENABLED) LOG.fine("Fetch fields: " + fetch);
            extras.put("fetch_fields", fetch);
        }
        if(snippet != null) {
            if(LOG_ENABLED) LOG.fine("Fetch snippets: " + snippet);
            extras.put("snippet_fields", snippet);
        }
        return extras;
    }

    @SuppressWarnings("unchecked")
    private void addResult(JSONArray ja, SearchResult result) {
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
