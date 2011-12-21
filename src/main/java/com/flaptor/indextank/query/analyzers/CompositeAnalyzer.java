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
