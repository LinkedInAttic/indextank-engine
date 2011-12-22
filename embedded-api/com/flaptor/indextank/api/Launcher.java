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

import java.io.File;

import com.flaptor.indextank.api.util.JettyHelper;


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
        EmbeddedIndexEngine engine = EmbeddedIndexEngine.instantiate(params);
        IndexEngineApi api = new IndexEngineApi(engine);
        JettyHelper.server(Configuration.port, "/v1/*", EmbeddedApiV1.class.getName(), api);
    }

}
