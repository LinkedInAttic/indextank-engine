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

import com.flaptor.indextank.api.resources.Autocomplete;
import com.flaptor.indextank.api.resources.DeleteDocs;
import com.flaptor.indextank.api.resources.Docs;
import com.flaptor.indextank.api.resources.Search;
import com.ghosthack.turismo.action.Action;
import com.ghosthack.turismo.routes.RoutesList;

public class EmbeddedApiV1 extends RoutesList {

    protected void map() {

        get("/indexes/:name/autocomplete", new Autocomplete());

        get("/indexes/:name/search", new Search());

        put("/indexes/:name/docs", new Docs());

        delete("/indexes/:name/docs", new DeleteDocs());
        
        get("/indexes", new Action() {
            public void run() {
                // dummy response for embedded server
                print("{\"idx\": {\"status\": \"LIVE\", \"code\": \"dbajo\", " +
                        "\"started\": true, \"public_search\": true, " +
                        "\"creation_time\": \"2011-04-22T05:37:43\", \"size\": 0}}");
            }
        });

        get("/indexes/:name", new Action() {
            public void run() {
                // dummy response for embedded server
                print("{\"status\": \"LIVE\", \"code\": \"dbajo\", " +
                        "\"started\": true, \"public_search\": true, " +
                        "\"creation_time\": \"2011-04-22T05:37:43\", \"size\": 0}");
            }
        });

        put("/indexes/:name", new Action() {
            public void run() {
                // dummy
                res().setStatus(204);
            }
        });

        delete("/indexes/:name", new Action() {
            public void run() {
                // dummy
                res().setStatus(204);
            }
        });

    }

}
