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
        return new IndexEngineAnalyzer(configuration);
    }
    
}
