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

package com.flaptor.indextank.index.rti.inverted;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.flaptor.indextank.Indexer;
import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.index.term.DocTermMatch;
import com.flaptor.indextank.index.term.TermMatcher;
import com.flaptor.indextank.index.term.query.RawMatch;
import com.flaptor.indextank.index.term.query.TermBasedQueryMatcher;
import com.flaptor.indextank.query.AToken;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;
import com.flaptor.indextank.util.Skippables;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;


public class InvertedIndex implements Indexer, QueryMatcher, TermMatcher {

	private final int maxDocCount;
	private final DocId[] docids;
	private final BitSet internalDeletes;
	private final AtomicInteger docCount;
	
	private final ConcurrentMap<DocId, Integer> docidsIndexes = new MapMaker().makeMap();
	private final ConcurrentNavigableMap<Key, DocTermMatchList> invertedIndex = new ConcurrentSkipListMap<Key, DocTermMatchList>();
	private final ConcurrentMap<DocId, DocId> deletes = new MapMaker().makeMap();
	//private final ConcurrentHashMap<DocId, DocId> deletes = Maps.newConcurrentHashMap();
	private final QueryMatcher matcher;
	private final IndexEngineParser parser;

	public InvertedIndex(Scorer scorer, IndexEngineParser parser, int maxDocCount, FacetingManager facetingManager) {
        Preconditions.checkArgument(maxDocCount > 0);
		this.maxDocCount = maxDocCount;
		this.docids = new DocId[maxDocCount];
		this.internalDeletes = new BitSet(maxDocCount);
		this.docCount = new AtomicInteger(0);
		this.matcher = new TermBasedQueryMatcher(scorer, this, facetingManager);
        this.parser = parser;
	}
	
	public void add(String sdocid, final Document document) {
		int idx = docCount.getAndIncrement();
		if (idx < maxDocCount) {
		    DocId docid = new DocId(sdocid);
			docids[idx] = docid;
			Integer oldIdx = docidsIndexes.put(docid, idx);
			if (oldIdx != null) {
				internalDel(oldIdx);
			}
			internalAdd(idx, document);
		} else {
			throw new IllegalStateException("MaxDocCount (" + maxDocCount + ") reached. Cannot add more documents.");
		}
	}

	public void del(String sdocid) {
	    DocId docid = new DocId(sdocid);
    	Integer idx = docidsIndexes.get(docid);
    	if (idx != null) {
    		internalDel(idx);
    	} else {
    		deletes.put(docid, docid);
    	}
    }

	private void internalAdd(int idx, final Document document) {
		for (String field : document.getFieldNames()) {
			Iterator<AToken> tokens = parser.parseDocumentField(field, document.getField(field));
			SortedSetMultimap<String, Integer> termPositions = TreeMultimap.create();
			int tokenCount = 0;
			while (tokens.hasNext()) {
				tokenCount++;
				AToken token = tokens.next();
				termPositions.put(token.getText(), token.getPosition());
			}
				
			for (String term : termPositions.keySet()) {
				Key key = new Key(field, term);
				SortedSet<Integer> positionsSet = termPositions.get(term);
                int[] positions = new int[positionsSet.size()];
                int p = 0;
                for (Integer i : positionsSet) {
                    positions[p++] = i;
                }
				DocTermMatchList original = invertedIndex.putIfAbsent(key, new DocTermMatchList(idx, positions, tokenCount));
				if (original != null) {
					original.add(idx, positions, tokenCount);
				}
			}
		}
	}

	private void internalDel(int idx) {
		internalDeletes.set(idx);
	}
	
	public SkippableIterable<DocTermMatch> getMatches(String field, String term) {
		DocTermMatchList docList = invertedIndex.get(new Key(field, term));
		if (docList == null) {
			return Skippables.emptyIterable();
		} else {
			return Skippables.filter(docList, notDeletedPredicate());
		}
	}

	@Override
	public NavigableMap<String, SkippableIterable<DocTermMatch>> getMatches(String field, String termFrom, String termTo) {
	    Key leftBoundary = new Key(field, termFrom);
	    Key rightBoundary = new Key(field, termTo);
	    
	    ConcurrentNavigableMap<Key, DocTermMatchList> range = invertedIndex.subMap(leftBoundary, rightBoundary);
	    
	    NavigableMap<String, SkippableIterable<DocTermMatch>> result = new TreeMap<String, SkippableIterable<DocTermMatch>>();
	    
	    int numberOfTerms = 0;
	    for (Entry<Key, DocTermMatchList> entry : range.entrySet()) {
            result.put(entry.getKey().term, Skippables.filter(entry.getValue(), notDeletedPredicate()));
            numberOfTerms++;
            if (numberOfTerms >= 1000) {
                break;
            }
        }
	    
	    return result;
	}
	
	
	private Predicate<DocTermMatch> notDeletedPredicate() {
		return new Predicate<DocTermMatch>() {
			@Override
			public boolean apply(DocTermMatch item) {
				return !internalDeletes.get(item.getRawId());
			}
		};
	}

	public boolean hasChanges(DocId docid) {
	    return docidsIndexes.containsKey(docid) || deletes.containsKey(docid);
	}

	@Override
	public Iterable<ScoredMatch> decode(Iterable<RawMatch> rawMatches, final double boostedNorm) {
		return Iterables.transform(rawMatches, new Function<RawMatch, ScoredMatch>() {
            @Override
            public ScoredMatch apply(RawMatch rawMatch) {
                //System.out.println("RESULT: "+rawMatch.getNormalizedScore());
                return new ScoredMatch(rawMatch.getBoostedScore() / boostedNorm, docids[rawMatch.getRawId()]);
            }
        });
	}
	
	/* QueryMatcher interface - delegates in internal matcher instance */

	public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException {
		return matcher.findMatches(query, limit, scoringFunctionIndex);
	}

	public TopMatches findMatches(Query query, Predicate<DocId> idFilter,
			int limit, int scoringFunctionIndex) throws InterruptedException {
		return matcher.findMatches(query, idFilter, limit, scoringFunctionIndex);
	}

    @Override
    public SkippableIterable<Integer> getAllDocs() {
        return new AbstractSkippableIterable<Integer>() {
            @Override
            public SkippableIterator<Integer> iterator() {
                return new AbstractSkippableIterator<Integer>() {
                    int current = -1;
                    @Override
                    public void skipTo(int i) {
                        current = i-1;
                    }
                    
                    @Override
                    protected Integer computeNext() {
                        while (++current < docCount.get()) {
                            if (!internalDeletes.get(current)) {
                                return current;
                            }
                        }
                        return endOfData();
                    }
                };
            }
        };
    }

    @Override
    public int countMatches(Query query) throws InterruptedException {
        return matcher.countMatches(query);
    }

    @Override
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException {
        return matcher.countMatches(query, idFilter);
    }

    public Map<String, String> getStats(String prefix) {
        Map<String, String> stats = Maps.newHashMap();
        stats.put(prefix + "size", String.valueOf(docCount.get()));
        stats.put(prefix + "terms", String.valueOf(invertedIndex.size()));
        stats.put(prefix + "deletes", String.valueOf(deletes.size()));
        stats.put(prefix + "internal_deletes", String.valueOf(internalDeletes.cardinality()));
        return stats;
    }
	
}
