package com.flaptor.indextank.query.analyzers;

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

public class CompositeAnalyzer extends Analyzer {
	private final Map<String, Analyzer> perfieldsAnalyzers;
	private final Analyzer defaultAnalyzer;
	
	public CompositeAnalyzer(Analyzer defaultAnalyzer, Map<String, Analyzer> perfieldAnalyzers) {
		this.defaultAnalyzer = defaultAnalyzer;
		this.perfieldsAnalyzers = perfieldAnalyzers;
	}
	
	@Override
	public final TokenStream tokenStream(String fieldName, Reader reader) {
		Analyzer analyzer = perfieldsAnalyzers.get(fieldName);
		
		if (analyzer == null) {
			analyzer = defaultAnalyzer;
		}
		
		return analyzer.tokenStream(fieldName, reader);
	}

}
