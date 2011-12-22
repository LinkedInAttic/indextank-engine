package com.flaptor.indextank.api;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONAware;

import com.flaptor.indextank.api.resources.Docs;
import com.flaptor.indextank.api.resources.Search;
import com.flaptor.indextank.api.util.QueryHelper;
import com.flaptor.indextank.rpc.CategoryFilter;
import com.flaptor.indextank.rpc.IndextankException;
import com.flaptor.indextank.rpc.InvalidQueryException;
import com.flaptor.indextank.rpc.MissingQueryVariableException;
import com.flaptor.indextank.rpc.RangeFilter;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.ghosthack.turismo.action.Action;
import com.ghosthack.turismo.routes.RoutesList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EmbeddedApiV1 extends RoutesList {

    protected void map() {

        get("/indexes/:name/search", new Search());

        put("/indexes/:name/docs", new Docs());

        get("/indexes", new Action() {
            public void run() {
                // dummy response for embedded server
                print("{\"idx\": {\"status\": \"LIVE\", \"code\": \"dbajo\", \"started\": true, \"public_search\": true, \"creation_time\": \"2011-04-22T05:37:43\", \"size\": 0}}");
            }
        });
        get("/indexes/:name", new Action() {
            public void run() {
                // dummy response for embedded server
                print("{\"status\": \"LIVE\", \"code\": \"dbajo\", \"started\": true, \"public_search\": true, \"creation_time\": \"2011-04-22T05:37:43\", \"size\": 0}");
            }
        });
        put("/indexes/:name", new Action() {
            public void run() { // dummy
                res().setStatus(204);
            }
        });
        delete("/indexes/:name", new Action() {
            public void run() { // dummy
                res().setStatus(204);
            }
        });

    }

    public abstract class BackendAction extends Action {
        public void run() {
            try {
                flow();
            } catch(BackendException e) {
                if(LOG_ENABLED) LOG.info("Backend ex: " + e.getMessage());
            }
            res().setStatus(503);
        }

        public void json(JSONAware json) {
            //FIXME: impl streamed version
            print(json.toJSONString());
        }
        
        public abstract void flow();
    }
    
    private static final Logger LOG = Logger.getLogger(EmbeddedApiV1.class.getName());
    private static final boolean LOG_ENABLED = false;

}
