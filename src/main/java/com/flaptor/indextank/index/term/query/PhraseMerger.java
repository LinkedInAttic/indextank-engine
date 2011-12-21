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

/**
 * 
 */
package com.flaptor.indextank.index.term.query;

import java.util.List;

import com.flaptor.indextank.index.term.DocTermMatch;
import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.IdentityIntersection;
import com.flaptor.indextank.util.Intersection;
import com.flaptor.indextank.util.PeekingSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;
import com.flaptor.indextank.util.Skippables;
import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

final class PhraseMerger extends AbstractSkippableIterable<RawMatch> {
    
    
    private Iterable<SkippableIterable<DocTermMatch>> cursors;
    private int[] termPositions;
    private double boost;
    
    PhraseMerger(Iterable<SkippableIterable<DocTermMatch>> cursors, int[] termPositions, double boost) {
        this.cursors = cursors;
        this.termPositions = termPositions;
        this.boost = boost;
    }

    @Override
    public SkippableIterator<RawMatch> iterator() {
        return new AbstractSkippableIterator<RawMatch>() {
            private List<PeekingSkippableIterator<DocTermMatch>> iterators = Lists.newArrayList(Iterables.transform(cursors, Intersection.<DocTermMatch>peekingIteratorFunction()));
            
            @Override
            protected RawMatch computeNext() {
                int current = -1;
                
                while (true) {
                    boolean found = true;
                    for (PeekingSkippableIterator<DocTermMatch> it : iterators) {
                        while (it.hasNext() && current != -1 && it.peek().getRawId() < current) {
                            it.skipTo(current);
                            it.next();
                        }
                        if (!it.hasNext()) {
                            return endOfData();
                        }
                        if (current != -1 && it.peek().getRawId() == current) {
                            continue;
                        } else {
                            found = false;
                            current = it.peek().getRawId();
                        }
                    }
                    if (found) {
                        RawMatch r = tryMatch(current, peekedItems());
                        for (PeekingIterator<DocTermMatch> it : iterators) {
                            it.next();
                        }
                        if (r != null)
                            return r;
                    }
                }
            }
            
            private List<DocTermMatch> peekedItems() {
                return Lists.transform(iterators, Intersection.<DocTermMatch>peekFunction());
            }

            @Override
            public void skipTo(int i) {
                for (PeekingSkippableIterator<DocTermMatch> it : iterators) {
                    it.skipTo(i);
                }
            }
        };
    }

    public static <T> Function<PeekingIterator<T>, T> peekFunction() {
        return new Function<PeekingIterator<T>, T>() {
            @Override
            public T apply(PeekingIterator<T> it) {
                return it.peek();
            }
        };
    }

    public static <T> Function<SkippableIterable<T>, PeekingSkippableIterator<T>> peekingIteratorFunction() {
        return new Function<SkippableIterable<T>, PeekingSkippableIterator<T>>() {
            @Override
            public PeekingSkippableIterator<T> apply(SkippableIterable<T> ts) {
                return new PeekingSkippableIterator<T>(ts.iterator());
            }
        };
    }

	protected RawMatch tryMatch(int id, List<DocTermMatch> items) {
		List<SkippableIterable<Integer>> positionsList = Lists.newArrayList();
		for (int i = 0; i < items.size(); i++) {
			DocTermMatch m = items.get(i);
			List<Integer> positions = Ints.asList(m.getPositions()).subList(0, m.getPositionsLength());
			positionsList.add(Skippables.fromIterable(Iterables.transform(positions, addFunction(-this.termPositions[i]))));
		}
		
		boolean matches = new IdentityIntersection<Integer>(positionsList).iterator().hasNext();
		
		if (matches) {
            double score = 1.0;
            for (DocTermMatch p : items) {
                score += p.getTermScore();
            }
            return new RawMatch(id, score, boost);
		} else {
		    return null;
		}
	}

	private static Function<Integer, Integer> addFunction(final int delta) {
		return new Function<Integer, Integer>() {
			@Override
			public Integer apply(Integer i) {
				return i + delta;
			}
		};
	}
}
