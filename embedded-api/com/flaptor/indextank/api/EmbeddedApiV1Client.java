package com.flaptor.indextank.api;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;

public class EmbeddedApiV1Client {
    
    private String url;
    private String index;
    private WebResource resource;
    private boolean logging = true;

    private static final ClientConfig config = new DefaultClientConfig();
    private static final Client client = Client.create(config);

    public EmbeddedApiV1Client(String string, String string2) {
        this.url = string;
        this.index = string2;
        resource = client.resource(url);
        if(logging) {
            resource.addFilter(new LoggingFilter());
        }
    }

    public static void main(String[] args) {
        EmbeddedApiV1Client client = new EmbeddedApiV1Client("http://localhost:8080", "idx");
        JSONObject jo = new JSONObject();
        jo.put("docid", "docid");
        JSONObject fields = new JSONObject();
        jo.put("fields", fields);
        fields.put("text", "adrian");
        JSONArray ja = new JSONArray();
        ja.add(jo);
        client.putDocument(jo);
        //client.putDocument(ja);
    }

    private void putDocument(JSONAware jo) {
        String path = "/v1/indexes/" + index + "/docs";
        ClientResponse response = resource.path(path)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(ClientResponse.class, jo.toJSONString());
    }

}
