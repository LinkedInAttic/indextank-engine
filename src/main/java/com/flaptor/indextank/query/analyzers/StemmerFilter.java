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
