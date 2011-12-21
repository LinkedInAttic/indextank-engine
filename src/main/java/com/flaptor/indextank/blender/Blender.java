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

package com.flaptor.indextank.blender;

import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.Promoter;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.lsi.LargeScaleIndex;
import com.flaptor.indextank.index.rti.RealTimeIndex;
import com.flaptor.indextank.index.scorer.BoostsManager;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.search.AbstractDocumentSearcher;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.indextank.suggest.Suggestor;
import com.flaptor.indextank.util.SkippingIterable;
import com.flaptor.util.CollectionsUtil;
import com.flaptor.util.Execute;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
/**
 *
 * @author Flaptor Team
 */
public class Blender extends AbstractDocumentSearcher implements QueryMatcher {
    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private final LargeScaleIndex lsi;
    private final RealTimeIndex rti;
    private final Suggestor suggestor;
    private final Promoter promoter;
    private final BoostsManager boostsManager;

    public Blender(LargeScaleIndex lsi, RealTimeIndex rti, Suggestor suggestor, Promoter promoter, BoostsManager boostsManager) {
        Preconditions.checkNotNull(lsi);
        Preconditions.checkNotNull(rti);
        Preconditions.checkNotNull(promoter);
        Preconditions.checkNotNull(suggestor);
        Preconditions.checkNotNull(boostsManager);
        this.lsi = lsi;
        this.rti = rti;
        this.suggestor = suggestor;
        this.promoter = promoter;
        this.boostsManager = boostsManager;
    };

    private QueryMatcher getSearcher() {
    	QueryMatcher matcher = new BlendingQueryMatcher(lsi, rti.getSearchSession());
    	matcher = new BlendingQueryMatcher(matcher, promoter);
    	return matcher;
    }
    
    @Override
    public TopMatches findMatches(Query query, Predicate<DocId> docFilter, int limit, int scoringFunctionIndex) throws InterruptedException {
        TopMatches retVal = getSearcher().findMatches(query, docFilter, limit, scoringFunctionIndex);
    	suggestor.noteQuery(query, retVal.getTotalMatches());
    	return retVal;
    }
    
    @Override
    public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(limit > 0);
        TopMatches retVal = getSearcher().findMatches(query, limit, scoringFunctionIndex);
    	suggestor.noteQuery(query, retVal.getTotalMatches());
    	return retVal;
    }

	@Override
	public boolean hasChanges(DocId docid) throws InterruptedException {
		return getSearcher().hasChanges(docid);
	}

	@Override
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException {
        TopMatches matches = this.findMatches(query, start+limit, scoringFunctionIndex);
        Iterable<ScoredMatch> ids = matches;
        
        // apply pagination 
        ids = SkippingIterable.skipping(ids, start);
        ids = CollectionsUtil.limit(ids, limit);
        
        // base transform function
        final Function<ScoredMatch, SearchResult> baseTransform = ScoredMatch.SEARCH_RESULT_FUNCTION;
        Function<ScoredMatch, SearchResult> transformFunction = baseTransform;
        String fetchCategories = extraParameters.get("fetch_categories");
        String fetchVariables = extraParameters.get("fetch_variables");

        // adds fetch variables to the transform function
        if ("*".equals(fetchVariables) || "true".equalsIgnoreCase(fetchVariables)) {
            transformFunction = new Function<ScoredMatch, SearchResult>() {
                @Override
                public SearchResult apply(ScoredMatch scoredMatch) {
                    SearchResult searchResult = baseTransform.apply(scoredMatch);
                    searchResult.setVariables(boostsManager.getVariablesAsMap(scoredMatch.getDocId()));
                    return searchResult;
                }
            };
        }

        // adds fetch categories to the transform function
        final Function<ScoredMatch, SearchResult> prevTransform = transformFunction;
        if ("*".equals(fetchCategories) || "true".equalsIgnoreCase(fetchCategories)) {
            transformFunction = new Function<ScoredMatch, SearchResult>() {
                @Override
                public SearchResult apply(ScoredMatch scoredMatch) {
                    SearchResult searchResult = prevTransform.apply(scoredMatch);
                    searchResult.setCategories(boostsManager.getCategoryValues(scoredMatch.getDocId()));
                    return searchResult;
                }
            };
        }
        
        // convert to search results
        Iterable<SearchResult> results = Iterables.transform(ids, transformFunction);        

        // fix'em in a list
        results = Lists.newArrayList(results);

        return new SearchResults(results, matches.getTotalMatches(), matches.getFacetingResults());
    }
	
    @Override
    public int countMatches(Query query) throws InterruptedException {
        return getSearcher().countMatches(query);
    }

    @Override
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException {
        return getSearcher().countMatches(query, idFilter);
    }
    	
}
