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

package com.flaptor.indextank.index;

import com.flaptor.indextank.index.results.SimpleScoredDocIds;
import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public abstract class AbstractPromoter implements Promoter {

	@Override
	public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) {
		String queryStr = query.getOriginalStr();
		String docId = null;
		if (queryStr != null) {
			docId = this.getPromotedDocId(queryStr.toLowerCase());
		}
		if (docId != null) {
			ScoredMatch match = new ScoredMatch(Double.MAX_VALUE, new DocId(docId));
			return new SimpleScoredDocIds(
			        ImmutableList.of(match), limit, 1, Maps.<String, Multiset<String>>newHashMap());
		}
		return new SimpleScoredDocIds(ImmutableList.<ScoredMatch>of(), limit, 0, Maps.<String, Multiset<String>>newHashMap());
	}
	
	@Override
	public TopMatches findMatches(Query query, Predicate<DocId> idFilter, int limit, int scoringFunctionIndex) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean hasChanges(DocId docid) {
		return false;
	}
	
	@Override
	public int countMatches(Query query) {
	    return 0;
	}
	
	@Override
	public int countMatches(Query query, Predicate<DocId> idFilter) {
	    return 0;
	}
	
}
