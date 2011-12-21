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

package com.flaptor.indextank.index.term.query;

import java.util.List;
import java.util.NavigableMap;
import java.util.PriorityQueue;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.results.SimpleScoredDocIds;
import com.flaptor.indextank.index.scorer.Faceter;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.MatchFilter;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.index.term.DocTermMatch;
import com.flaptor.indextank.index.term.TermMatcher;
import com.flaptor.indextank.query.AndQuery;
import com.flaptor.indextank.query.DifferenceQuery;
import com.flaptor.indextank.query.MatchAllQuery;
import com.flaptor.indextank.query.OrQuery;
import com.flaptor.indextank.query.PrefixTermQuery;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.QueryNode;
import com.flaptor.indextank.query.RangeQuery;
import com.flaptor.indextank.query.SimplePhraseQuery;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;
import com.flaptor.indextank.util.Skippables;
import com.flaptor.indextank.util.Union;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class TermBasedQueryMatcher implements QueryMatcher {

	private final TermMatcher matcher;
	private final Scorer scorer;
	private final FacetingManager facetingManager;

	public TermBasedQueryMatcher(Scorer scorer, TermMatcher matcher, FacetingManager facetingManager) {
		this.matcher = matcher;
		this.scorer = scorer;
		this.facetingManager = facetingManager;
	}
	
	public TopMatches findMatches(Query query, Predicate<DocId> idFilter, int limit, int scoringFunctionIndex) {
		Iterable<RawMatch> rawMatches = match(query.getRoot());
		return getBestResults(rawMatches, idFilter, limit, query, scoringFunctionIndex);
	}

	@Override
	public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) {
		return findMatches(query, Predicates.<DocId>alwaysTrue(), limit, scoringFunctionIndex);
	}

	public int countMatches(Query query, Predicate<DocId> idFilter) {
	    return getCount(match(query.getRoot()), idFilter);
	}
	
	@Override
	public int countMatches(Query query) {
	    return countMatches(query, Predicates.<DocId>alwaysTrue());
	}
	
	@Override
	public boolean hasChanges(DocId docid) {
		return matcher.hasChanges(docid);
	}

	private TopMatches getBestResults(Iterable<RawMatch> rawMatches, Predicate<DocId> docFilter, int n, Query query, int scoringFunctionIndex) {
	    String property = System.getProperty("limitTermBasedQueryMatcher");
	    int limit = Integer.MAX_VALUE;
	    int minTime = Integer.MAX_VALUE;
	    if (property != null) {
	        String[] parts = property.split(",");
	        limit = Integer.parseInt(parts[0]);
	        minTime = Integer.parseInt(parts[1]);
	    }
	    
	    Faceter faceter = facetingManager.createFaceter();
	    
	    long startTime = System.currentTimeMillis();
		PriorityQueue<ScoredMatch> top = new PriorityQueue<ScoredMatch>(n, ScoredMatch.INVERSE_ORDER);
		
		int totalCount = 0;

		Iterable<ScoredMatch> matches = matcher.decode(rawMatches, query.getRoot().getBoostedNorm());

		MatchFilter facetFilter = null;
		if (query.getFilteringFacets() != null) {
			facetFilter = facetingManager.getFacetFilter(query.getFilteringFacets());
		}
		
		for (ScoredMatch match : matches) {
			if (docFilter.apply(match.getDocId())) {
				if (facetFilter == null || facetFilter.matches(match.getDocId(), match.getScore(), query.getNow(), query.getVars())) {
					if (query.getRangeFilter() == null || query.getRangeFilter().matches(match.getDocId(), match.getScore(), query.getNow(), query.getVars())) {
					    rescore(match, query, scoringFunctionIndex);
					    faceter.computeDocument(match.getDocId());
					    
					    if (top.size() < n || top.peek().compareTo(match) > 0) {
					        ScoredMatch newMatch;
					        if (top.size() == n) {
					            newMatch = top.remove();
					            newMatch.getDocId().updateFrom(match.getDocId());
					            newMatch.setScore(match.getScore());
					        } else {
					            newMatch = new ScoredMatch(match.getScore(), match.getDocId().copy(256));
					        }
		                    top.add(newMatch);
					    }
						totalCount++;
						
						if (totalCount > limit) {
						    if (System.currentTimeMillis() - startTime > minTime) {
						        totalCount = -totalCount;
						        break;
						    }
						}
					}
				}
			}
		}
		List<ScoredMatch> list = Lists.newArrayList(new ScoredMatch[top.size()]);
		for (int i = top.size()-1; i >= 0; i--) {
		    list.set(i, top.poll());
        }
		return new SimpleScoredDocIds(list, n, totalCount, faceter.getFacets());
	}
	
	private int getCount(Iterable<RawMatch> rawMatches, Predicate<DocId> docFilter) {
	    int totalCount = 0;
	    Iterable<ScoredMatch> matches = matcher.decode(rawMatches, 1d);
	    
	    for (ScoredMatch m : matches) {
	        if (docFilter.apply(m.getDocId())) {
	            totalCount++;
	        }
	    }
	    return totalCount;
	}

	private ScoredMatch rescore(ScoredMatch match, Query query, int functionIndex) {
	    match.setScore(scorer.scoreDocument(match.getDocId(), match.getScore(), query.getNow(), query.getVars(), functionIndex));
	    return match;
	}
	
	private SkippableIterable<RawMatch> match(QueryNode query) {
		// dispatch to specific methods based on query type
        if      (query instanceof TermQuery)         return matchTerm       (         (TermQuery) query );
		else if (query instanceof PrefixTermQuery)   return matchPrefix     (   (PrefixTermQuery) query );
		else if (query instanceof AndQuery)          return matchAnd        (          (AndQuery) query );
		else if (query instanceof OrQuery)           return matchOr         (           (OrQuery) query );
		else if (query instanceof DifferenceQuery)   return matchDifference (   (DifferenceQuery) query );
		else if (query instanceof SimplePhraseQuery) return matchPhrase     ( (SimplePhraseQuery) query );
		else if (query instanceof MatchAllQuery)     return matchAll        (     (MatchAllQuery) query );
		else if (query instanceof RangeQuery)        throw new IllegalArgumentException("Range queries not supported yet");
		else throw new IllegalArgumentException("Unsupported query type: " + query.getClass());
    }

    private SkippableIterable<RawMatch> matchTerm(TermQuery query) {
		final SkippableIterable<DocTermMatch> items = matcher.getMatches(query.getField(), query.getTerm());
        final double boost = query.getBoost();
		return new AbstractSkippableIterable<RawMatch>() {
            public SkippableIterator<RawMatch> iterator() {
                return new SkippableIterator<RawMatch>() {
                    RawMatch m = new RawMatch(0, 0d, boost);
                    SkippableIterator<DocTermMatch> it = items.iterator();
                    public void skipTo(int i) {
                        it.skipTo(i);
                    }
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                    @Override
                    public RawMatch next() {
                        DocTermMatch dtm = it.next();
                        m.setRawId(dtm.getRawId());
                        m.setScore(dtm.getTermScore());
                        m.setBoost(boost);
                        //System.out.println("TERM: [S:"+m.getScore()+", B:"+m.getBoost()+", N:"+m.getNorm()+"]");
                        return m;
                    }
                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
	}

    private SkippableIterable<RawMatch> matchPrefix(PrefixTermQuery query) {
        NavigableMap<String, SkippableIterable<DocTermMatch>> matches = matcher.getMatches(query.getField(), query.getTerm(), getNextPrefix(query.getTerm()));
        final RawMatch rawMatch = new RawMatch(0, 0d, query.getBoost());

        Union<DocTermMatch, RawMatch> union = new Union<DocTermMatch, RawMatch>(matches.values()) {
            @Override
            protected RawMatch transform(DocTermMatch k) {
                rawMatch.setRawId(k.getRawId());
                rawMatch.setScore(0d);
                return rawMatch;
            }
            
            @Override
            protected boolean shouldUse(RawMatch v, List<DocTermMatch> ks) {
                for (DocTermMatch docTermMatch : ks) {
                    rawMatch.setScore(rawMatch.getScore() + docTermMatch.getSquareTermScore());
                }
                
                rawMatch.setScore(Math.sqrt(rawMatch.getScore()));
                
                return true;
            }

            @Override
            protected int comp(DocTermMatch a, DocTermMatch b) {
                return a.getRawId() - b.getRawId();
            }
        };
        
        return union;
    }
    
    private static String getNextPrefix(String prefix) {
        return prefix.substring(0, prefix.length() - 1) + (char)(prefix.charAt(prefix.length() - 1) + 1);      
    }
    
	private SkippableIterable<RawMatch> matchAnd(AndQuery query) {
		SkippableIterable<RawMatch> left = match(query.getLeftQuery());
		SkippableIterable<RawMatch> right = match(query.getRightQuery());
        double boost = query.getBoost();
		SkippableIterable<RawMatch> am = new AndMerger2(left, right, boost);
        return am;
	}

	private SkippableIterable<RawMatch> matchPhrase(final SimplePhraseQuery query) {
		String field = query.getField();
		List<String> terms = query.getTerms();
		int[] termPositions = query.getTermPositions();
        double boost = query.getBoost();
		// each term gets converted to its item list by matching it to the given field
		return new PhraseMerger(Iterables.transform(terms, getFieldMatcher(field)), termPositions, boost);
	}

	private SkippableIterable<RawMatch> matchOr(OrQuery query) {
        QueryNode leftQuery = query.getLeftQuery();
        QueryNode rightQuery = query.getRightQuery();
		SkippableIterable<RawMatch> left = match(leftQuery);
		SkippableIterable<RawMatch> right = match(rightQuery);
		return new OrMerger2(left, right, query.getBoost(), leftQuery.getBoost(), rightQuery.getBoost(), leftQuery.getNorm(), rightQuery.getNorm());
	}

	private SkippableIterable<RawMatch> matchDifference(DifferenceQuery query) {
		SkippableIterable<RawMatch> left = match(query.getLeftQuery());
		SkippableIterable<RawMatch> right = match(query.getRightQuery());
        double boost = query.getBoost();
		return new DifferenceMerger(left, right, boost);
	}
	
	private SkippableIterable<RawMatch> matchAll(MatchAllQuery query) {
	    return Skippables.transform(matcher.getAllDocs(), new Function<Integer, RawMatch>() {
            @Override
            public RawMatch apply(Integer i) {
                return new RawMatch(i, 1d, 1d);
            }
        });
	}
	
	private Function<String, SkippableIterable<DocTermMatch>> getFieldMatcher(final String field) {
		return new Function<String, SkippableIterable<DocTermMatch>>() {
			@Override
			public SkippableIterable<DocTermMatch> apply(String term) {
				return matcher.getMatches(field, term);
			}
		};
	}

}
