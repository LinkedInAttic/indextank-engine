package com.flaptor.indextank.api.resources;

import java.io.IOException;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.flaptor.indextank.api.EngineApi;
import com.ghosthack.turismo.action.Action;

public class Docs extends Action {

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            Object parse = JSONValue.parseWithException(req().getReader());
            if(parse instanceof JSONObject) { // 200, 400, 404, 409, 503
                JSONObject jo = (JSONObject) parse;
                try {
                    putDocument(jo);
                    res().setStatus(200);
                    return;
                    
                } catch(Exception e) {
                    e.printStackTrace();
                    if(LOG_ENABLED) LOG.severe(e.getMessage());
                    res().setStatus(400);
                    print("Invalid or missing argument"); // TODO: descriptive error msg
                    return;
                }
            } else if(parse instanceof JSONArray) {
                JSONArray statuses = new JSONArray();
                JSONArray ja = (JSONArray) parse;
                if(!validateDocuments(ja)) {
                    res().setStatus(400);
                    print("Invalid or missing argument"); // TODO: descriptive error msg
                    return;
                }
                boolean hasError = false;
                for(Object o: ja) {
                    JSONObject jo = (JSONObject) o;
                    JSONObject status = new JSONObject();
                    try {
                        putDocument(jo);
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

    private static final Logger LOG = Logger.getLogger(Docs.class.getName());
    private static final boolean LOG_ENABLED = true;

}
