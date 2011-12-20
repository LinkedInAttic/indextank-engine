package com.flaptor.indextank.query;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.flaptor.indextank.query.analyzers.StopAnalyzer;
import com.google.common.collect.Maps;

public class IndexEngineAnalyzer extends StopAnalyzer {

    @SuppressWarnings("deprecation")
	public IndexEngineAnalyzer(Map<Object, Object> configuration) {
        super(configuration);
    }

    public IndexEngineAnalyzer() {
        this(Maps.newHashMap());
    }
    
    /** Constructs a {@link StandardTokenizer} filtered by a {@link
      StandardFilter}, a {@link LowerCaseFilter} and a {@link StopFilter}. */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = super.tokenStream(fieldName, reader);
        result = new ASCIIFoldingFilter(result);
        
        return result;
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        return tokenStream(fieldName, reader);
    }

    public static Analyzer buildAnalyzer(Map<Object, Object> configuration) {
        return new IndexEngineAnalyzer(configuration);
    }
    
}
