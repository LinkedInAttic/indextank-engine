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

package com.flaptor.indextank.search;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.suggest.DidYouMeanSuggestor;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.google.common.base.Preconditions;


public class DidYouMeanSearcher extends AbstractDocumentSearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    // if a query returns less than MIN_RESULTS, did you mean will trigger
    private static final int MIN_RESULTS = 10;
    private final DocumentSearcher delegate;
    private final DidYouMeanSuggestor suggestor;

    public DidYouMeanSearcher(DocumentSearcher searcher, DidYouMeanSuggestor suggestor) {
		Preconditions.checkNotNull(searcher);
		Preconditions.checkNotNull(suggestor);
        this.delegate = searcher;
        this.suggestor = suggestor;
    }

    @Override
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException{
        SearchResults results = this.delegate.search(query, start, limit, scoringFunctionIndex, extraParameters);
        if (results.getMatches() < MIN_RESULTS){
            if (null == results.getDidYouMean()) { 
                List<Pair<Query, String>> suggestions = this.suggestor.suggest(query);
                String bestSuggestion = null;
                for (Pair<Query, String> suggestion : suggestions) {
                    //new Query(root, originalStr, vars, filteringFacets, rangeFilter)
                    SearchResults betterSearchResults = this.delegate.search(suggestion.first(), 0, 1, scoringFunctionIndex, extraParameters);
                    if (betterSearchResults.getMatches() > 0) {
                        bestSuggestion = suggestion.last();
                        break;
                    }
                }
                if (bestSuggestion != null) {
                    results = new SearchResults(results.getResults(), results.getMatches(), results.getFacets(), bestSuggestion);
                } else {
                    results = new SearchResults(results.getResults(), results.getMatches(), results.getFacets());
                }
            } else {
                logger.debug("already had a suggestion for query " + query.toString());
            }
        }

        return results;
    }

    @Override
    public int countMatches(Query query) throws InterruptedException{
        return this.delegate.countMatches(query);
    }
}
