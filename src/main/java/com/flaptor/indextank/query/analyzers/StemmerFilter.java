package com.flaptor.indextank.query.analyzers;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;

public class StemmerFilter extends AnalyzerFilter {
	private String stemmerName;
	
	public StemmerFilter(String stemmerName) {
		this.stemmerName = stemmerName;
	}

	@Override
	public TokenFilter filter(TokenStream stream) {
		return new SnowballFilter(stream, stemmerName);
	}

	public static StemmerFilter buildFilter(Map<Object, Object> configuration) {
		return new StemmerFilter((String)configuration.get("stemmerName"));
	}

}
