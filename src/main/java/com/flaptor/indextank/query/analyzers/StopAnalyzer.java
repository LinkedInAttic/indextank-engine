package com.flaptor.indextank.query.analyzers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.json.simple.JSONArray;

import com.google.common.collect.ImmutableSet;

public abstract class StopAnalyzer extends StandardAnalyzer {
    
    private static final String STOPWORDS = "stopwords";
    
    public StopAnalyzer(Map<Object, Object> analyzerConfiguration) {
        super(Version.LUCENE_CURRENT, getStopWords(analyzerConfiguration));
    }

    private static Set<String> getStopWords(Map<Object, Object> analyzerConfiguration) {
        if (analyzerConfiguration.containsKey(STOPWORDS)) {
            JSONArray stopwordList = (JSONArray)analyzerConfiguration.get(STOPWORDS);
            Set<String> stopwords = new HashSet<String>(stopwordList.size());
            for (Object stopword : stopwordList){
                if ( !(stopword instanceof String) ) {
                    throw new IllegalArgumentException("Stopwords aren't Strings");
                }
                stopwords.add((String)stopword);
            }
            return stopwords;
        } else {
            return ImmutableSet.of();
        }
    }
}
