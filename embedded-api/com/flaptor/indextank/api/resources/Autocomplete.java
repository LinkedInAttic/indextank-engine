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

import com.flaptor.indextank.api.IndexEngineApi;
import com.ghosthack.turismo.action.Action;

import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

public class Autocomplete extends Action {

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

        String query = params("query");
        String field = params("field");
        String callback = params("callback");

        if (field == null || field.isEmpty()) {
            field = "text";
        }

        List<String> complete = api.complete(query, field);

        JSONObject json = new JSONObject();
        json.put("query", query);
        json.put("suggestions", complete);

        if(callback != null && !callback.trim().isEmpty()) {
          print(callback.trim()+"("+json.toJSONString()+")");
        } else {
          print(json.toJSONString());
        }

    }

    private static final Logger LOG = Logger.getLogger(Autocomplete.class.getName());
    private static final boolean LOG_ENABLED = true;

}
