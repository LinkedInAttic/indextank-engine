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

package com.flaptor.indextank.query;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.util.Version;

import com.google.common.collect.Maps;

public class IndexEngineCJKAnalyzer extends CJKAnalyzer {

    public IndexEngineCJKAnalyzer(Map<Object, Object> configuration) {
        // Matching version requested for first testing user
        super(Version.valueOf((String)configuration.get("match_version")));
    }

    public IndexEngineCJKAnalyzer() {
        this(Maps.newHashMap());
    }

    public static Analyzer buildAnalyzer(Map<Object, Object> configuration) {
        return new IndexEngineCJKAnalyzer(configuration);
    }
    
}
