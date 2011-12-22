package com.flaptor.indextank.api;

import java.io.File;

import com.flaptor.indextank.api.EmbeddedIndexEngine.Components;
import com.flaptor.indextank.api.util.JettyHelper;
import com.flaptor.indextank.index.IndexEngine;


public class Launcher {

    public static void main(String[] args) throws Exception {
        final String base = "target/base/";
        final String [] params = new String[]{
                "--facets", 
                "--rti-size", "500", 
                "--conf-file", "sample-engine-config", 
                "--port", Configuration.port + "", // indexer port+1, searcher port+2, suggestor port+3
                "--environment-prefix", "TEST", 
                "--recover", 
                "--dir", base + "index", 
                "--snippets", 
                "--suggest", "documents", 
                "--boosts", "3", 
                "--index-code", Configuration.indexCode, 
                "--functions", "0:-age", 
                };
        new File(base + "index").mkdirs();
        Configuration.engine = EmbeddedIndexEngine.instantiate(params);
        JettyHelper.server(8080, "/v1/*", "com.flaptor.indextank.api.EmbeddedApiV1");
    }

}
