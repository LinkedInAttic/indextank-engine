package com.flaptor.indextank.blender;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.results.SimpleScoredDocIds;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.query.Query;
import com.flaptor.util.CollectionsUtil;
import com.flaptor.util.Execute;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 * A BlendingQueryMatcher is created based on two internal matchers. One of
 * them should be static and provide older results and the other one should
 * be fresher and its results will be considered true over the old ones.
 * <br><br>
 * This matcher will merge results from both searchers and will prioritize
 * the current results. Any result with changes in the newer matcher will
 * be ignored in the older matcher.  
 * 
 * @author Santiago Perez (santip)
 */
public class BlendingQueryMatcher implements QueryMatcher {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	private final QueryMatcher historySearcher;
	private final QueryMatcher currentSearcher;

    /**
     * Constructor.
     */
	public BlendingQueryMatcher(QueryMatcher history, QueryMatcher current) {
		this.historySearcher = history;
		this.currentSearcher = current;
	}
	
	@Override
	public TopMatches findMatches(Query query, Predicate<DocId> docFilter, int limit, int scoringFunctionIndex) throws InterruptedException {
		Predicate<DocId> historyFilter = notModified(currentSearcher, docFilter);
		
        /* instrumentation */ long historyStart = System.currentTimeMillis();
		
        TopMatches history = historySearcher.findMatches(query, historyFilter, limit, scoringFunctionIndex);
		int historyMatches = history.getTotalMatches();
		
		/* instrumentation */ long currentStart = System.currentTimeMillis();
		
        TopMatches current = docFilter == null ? currentSearcher.findMatches(query, limit, scoringFunctionIndex) : currentSearcher.findMatches(query, docFilter, limit, scoringFunctionIndex);
		int currentMatches = current.getTotalMatches();

		/* instrumentation */ long endSearch = System.currentTimeMillis();
		
		List<ScoredMatch> currentResults = Lists.newArrayList(current);
		
		Iterable<ScoredMatch> historyResults = filterIds(history, getIds(currentResults));
		List<ScoredMatch> results = mergeIntoList(currentResults, historyResults, limit);
        int matches = Math.abs(currentMatches) + Math.abs(historyMatches);
		if (results.size() < limit) {
			matches = results.size();
		}
		
		if (currentMatches < 0 || historyMatches < 0) {
		    matches = -matches;
		}
		
		/* instrumentation */  long end = System.currentTimeMillis();
		
        logger.debug("(Search) historic searcher took: " + (currentStart - historyStart) + " ms., current searcher took: " +
                + (endSearch - currentStart) + " ms., merge took: " + (end - endSearch) + " ms.");
        
        Map<String, Multiset<String>> facets = FacetingManager.mergeFacets(history.getFacetingResults(), current.getFacetingResults());
        
		return new SimpleScoredDocIds(results, limit, matches, facets);
	}

	public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException {
		return findMatches(query, null, limit, scoringFunctionIndex);
	}
	
	@Override
	public boolean hasChanges(DocId docid) throws InterruptedException {
		return historySearcher.hasChanges(docid) || currentSearcher.hasChanges(docid);
	}
	
	private static Set<DocId> getIds(List<ScoredMatch> currentResults) {
		return Sets.newHashSet(Iterables.transform(currentResults, ScoredMatch.DOC_ID_FUNCTION));
	}
	
	private static List<ScoredMatch> mergeIntoList(Iterable<ScoredMatch> a, Iterable<ScoredMatch> b, int limit) {
		Iterable<ScoredMatch> merged = CollectionsUtil.mergeIterables(ImmutableList.of(a, b));
		return Lists.newArrayList(merged);
	}

	private static Iterable<ScoredMatch> filterIds(Iterable<ScoredMatch> results, final Set<DocId> ids) {
		return Iterables.filter(results, new Predicate<ScoredMatch>() {
			@Override
			public boolean apply(ScoredMatch r) {
				return !ids.contains(r.getDocId());
			}
		});
	}

	private static Predicate<DocId> notModified(final QueryMatcher s, final Predicate<DocId> andFilter) {
		return new Predicate<DocId>() {
			@Override
			public boolean apply(DocId docid) {
                try {
                    return !s.hasChanges(docid) && (andFilter == null || andFilter.apply(docid)) ;
                } catch (InterruptedException e) {
                    return false;
                }
			}
		};
	}

    @Override
    public int countMatches(Query query) throws InterruptedException {
        return countMatches(query, null);
    }

    @Override
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException {
        Predicate<DocId> historyFilter = notModified(currentSearcher, idFilter);
        
        int count = historySearcher.countMatches(query, historyFilter);
        
        if (idFilter == null) {
            count += currentSearcher.countMatches(query);
        } else {
            count += currentSearcher.countMatches(query, idFilter);
        }
        return count;
    }


}
