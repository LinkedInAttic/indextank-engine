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

package com.flaptor.indextank.index.term;

import java.util.NavigableMap;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.util.SkippableIterable;


public interface TermMatcher extends RawMatchesDecoder {

	public SkippableIterable<DocTermMatch> getMatches(String field, String term);
	/**
	 * Range (prefix) matching. All terms (existing in the index) between termFrom and termTo
	 * (lexicographically speaking) will be matched for the field.  
	 * 
	 * @param field the field to match in
	 * @param termFrom the left boundary of the range (inclusive)
	 * @param termTo the rigth boundary of the range (exclusive)
	 * 
	 * @return a NavigableMap from the terms to the resulting Iterables of DocTermMatch
	 */
	public NavigableMap<String, SkippableIterable<DocTermMatch>> getMatches(String field, String termFrom, String termTo);
	public SkippableIterable<Integer> getAllDocs();
	public boolean hasChanges(DocId docid);

}
