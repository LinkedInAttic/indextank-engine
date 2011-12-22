package com.flaptor.indextank.api.resources;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.flaptor.indextank.api.EngineApi;
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

public class Search extends Action {

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        String q = params("q");
        int start = QueryHelper.parseIntParam(params("start"), 0);
        int len = QueryHelper.parseIntParam(params("end"), 10);
        int function = QueryHelper.parseIntParam(params("function"), 0);
        Map<Integer, Double> vars = Maps.newHashMap();
        List<CategoryFilter> facets = Lists.newArrayList();
        List<RangeFilter> variableRangeFilters = Lists.newArrayList();
        List<RangeFilter> functionRangeFilters = Lists.newArrayList();
        Map<String, String> extras = Maps.newHashMap();

        try {
            SearchResults results = api.search(q, start, len, function, vars, facets, variableRangeFilters, functionRangeFilters, extras);
            for(SearchResult result: results.getResults()) {
                System.out.println(result);
            }
            
            // dummy response for embedded server
            int matches = 0;
            print("{"
                    + "\"matches\": " + matches
                    + "}");
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

    private void putDocument(JSONObject jo) {
        String docid = String.valueOf(jo.get("docid")); // TODO: empty & < 1024b
        JSONObject fields = (JSONObject) jo.get("fields"); // TODO: sum(field.value) < 100Kb
        JSONObject variables = (JSONObject) jo.get("variables");
        JSONObject categories = (JSONObject) jo.get("categories");
        api.putDocument(docid, fields, variables, categories);
    }

    private boolean validateDocument(JSONObject jo) {
        return true; // TODO: validate the document
    }

    private boolean validateDocuments(JSONArray ja) {
        for(Object o: ja) {
            JSONObject jo = (JSONObject) o;
            if(!validateDocument(jo)) {
                return false;
            }
        }
        return true;
    }

    private final EngineApi api = new EngineApi();

    private static final Logger LOG = Logger.getLogger(Search.class.getName());
    private static final boolean LOG_ENABLED = true;

}
