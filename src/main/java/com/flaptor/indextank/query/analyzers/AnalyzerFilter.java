package com.flaptor.indextank.query.analyzers;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

public abstract class AnalyzerFilter {
	abstract public TokenFilter filter(TokenStream stream); 
}
