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
package com.flaptor.indextank.util;

import java.util.List;

import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public abstract class Intersection<K,V extends Comparable<V>> extends AbstractSkippableIterable<V> {

	private Iterable<SkippableIterable<K>> cursors;
	public Intersection(Iterable<SkippableIterable<K>> cursors) {
		this.cursors = cursors;
	}

	protected abstract V transform(K k);
	protected boolean shouldUse(V v, List<K> ks) {
		return true;
	}
	
	@Override
	public SkippableIterator<V> iterator() {
		return new AbstractSkippableIterator<V>() {
			private List<PeekingSkippableIterator<K>> iterators = Lists.newArrayList(Iterables.transform(cursors, Intersection.<K>peekingIteratorFunction()));
			
			@Override
			protected V computeNext() {
				V current = null;
				
                while (true) {
                    boolean found = true;
                    for (PeekingSkippableIterator<K> it : iterators) {
                        while (it.hasNext() && current != null && transform(it.peek()).compareTo(current) < 0) {
                        	advanceTo(it, current);
                            it.next();
                        }
                        if (!it.hasNext()) {
                            return endOfData();
                        }
                        if (current != null && transform(it.peek()).equals(current)) {
                            continue;
                        } else {
                            found = false;
                            current = transform(it.peek());
                        }
                    }
                    if (found) {
                    	V r = null;
                    	if (shouldUse(current, peekedItems())) {
                    		r = current;
                    	}
                        for (PeekingIterator<K> it : iterators) {
                        	it.next();
                        }
                        if (r != null)
                        	return r;
                    }
                }
			}
			
			private List<K> peekedItems() {
				return Lists.transform(iterators, Intersection.<K>peekFunction());
			}

			@Override
			public void skipTo(int i) {
				for (PeekingSkippableIterator<K> it : iterators) {
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

	protected void advanceTo(PeekingSkippableIterator<K> it, V current) {
	}

}
