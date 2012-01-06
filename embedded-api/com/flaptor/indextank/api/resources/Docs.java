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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.flaptor.indextank.api.IndexEngineApi;
import com.ghosthack.turismo.action.Action;
import com.ghosthack.turismo.servlet.Env;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Docs extends Action {

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        IndexEngineApi api = (IndexEngineApi) ctx().getAttribute("api");
        HttpServletResponse res = res();
        HttpServletRequest req = req();

        String characterEncoding = api.getCharacterEncoding();
        try {
            req.setCharacterEncoding(characterEncoding);
            res.setCharacterEncoding(characterEncoding);
            res.setContentType("application/json");
        } catch (UnsupportedEncodingException ignored) {
        }

        try {
            Object parse = JSONValue.parseWithException(req.getReader());
            if(parse instanceof JSONObject) { // 200, 400, 404, 409, 503
                JSONObject jo = (JSONObject) parse;
                try {
                    putDocument(api, jo);
                    res.setStatus(200);
                    return;
                    
                } catch(Exception e) {
                    e.printStackTrace();
                    if(LOG_ENABLED) LOG.severe(e.getMessage());
                    res.setStatus(400);
                    print("Invalid or missing argument"); // TODO: descriptive error msg
                    return;
                }
            } else if(parse instanceof JSONArray) {
                JSONArray statuses = new JSONArray();
                JSONArray ja = (JSONArray) parse;
                if(!validateDocuments(ja)) {
                    res.setStatus(400);
                    print("Invalid or missing argument"); // TODO: descriptive error msg
                    return;
                }
                boolean hasError = false;
                for(Object o: ja) {
                    JSONObject jo = (JSONObject) o;
                    JSONObject status = new JSONObject();
                    try {
                        putDocument(api, jo);
                        status.put("added", true);
                    } catch(Exception e) {
                        status.put("added", false);
                        status.put("error", "Invalid or missing argument"); // TODO: descriptive error msg
                        hasError = true;
                    }
                    statuses.add(status);
                }
                print(statuses.toJSONString());
                return;
            }
        } catch (IOException e) {
            if(LOG_ENABLED) LOG.severe("PUT doc, parse input " + e.getMessage());
        } catch (ParseException e) {
            if(LOG_ENABLED) LOG.severe("PUT doc, parse input " + e.getMessage());
        } catch (Exception e) {
            if(LOG_ENABLED) LOG.severe("PUT doc " + e.getMessage());
        }
        res.setStatus(503);
        print("Service unavailable"); // TODO: descriptive error msg
    }

    private void putDocument(IndexEngineApi api, JSONObject jo) {
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

    private static final Logger LOG = Logger.getLogger(Docs.class.getName());
    private static final boolean LOG_ENABLED = true;

}
