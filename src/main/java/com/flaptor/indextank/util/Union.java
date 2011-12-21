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

package com.flaptor.indextank.util;

import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.flaptor.util.Pair;
import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class Union<K,V extends Comparable<V>> extends AbstractSkippableIterable<V> {

	private Iterable<SkippableIterable<K>> cursors;
	public Union(Iterable<SkippableIterable<K>> cursors) {
		this.cursors = cursors;
	}

	protected abstract V transform(K k);
	protected boolean shouldUse(V v, List<K> ks) {
		return true;
	}
	
	protected int comp(K a, K b) {
		return transform(a).compareTo(transform(b));
	}

	private static class Head<K> {
	    final int position;
	    K value;
	    
	    public Head(int position) {
            this.position = position;
        }
	}
	
	@Override
	public SkippableIterator<V> iterator() {
		return new AbstractSkippableIterator<V>() {
			private List<PeekingSkippableIterator<K>> iterators = Lists.newArrayList(Iterables.transform(cursors, Union.<K>peekingIteratorFunction()));
			/*
			 * SortedSet with all the heads (first elements) for all the iterators and the position in the iterators list of the
			 * SkippableIterator the element is in.
			 */
			private TreeSet<Head<K>> heads = new TreeSet<Head<K>>(new Comparator<Head<K>>() {
                @Override
                public int compare(
                        Head<K> o1,
                        Head<K> o2) {
                    int compare = comp(o1.value, o2.value);
                    if (compare != 0) {
                        return compare;
                    } else {
                        return o1.position - o2.position;
                    }
                }
            });

			private List<Head<K>> prebuiltHeads = Lists.newArrayListWithExpectedSize(iterators.size());
			{
			    for (int i = 0; i < iterators.size(); i++) {
                    prebuiltHeads.add(new Head<K>(i));
                }
			}
			private Head<K> getHead(int position, K value) {
			    Head<K> head = prebuiltHeads.get(position);
			    head.value = value;
			    return head;
			}
			
			{
                /*
                 *  Keep all the heads of all the iterators sorted.  
                 */
                for (int i = 0; i < iterators.size(); i++) {
                    PeekingSkippableIterator<K> it = iterators.get(i);
                    if (it.hasNext()) {
                        heads.add(getHead(i, it.next()));
                    }
                }

			}
			
			private List<K> ks = Lists.newArrayList();
			private BitSet nextsToDo = new BitSet(); 
			
			@Override
			protected V computeNext() {
                while (true) {
                    int pos = 0;
                    while (true) {
                        pos = nextsToDo.nextSetBit(pos);
                        if (pos < 0) {
                            break;
                        }
                        if (iterators.get(pos).hasNext()) {
                            heads.add(getHead(pos, iterators.get(pos).next()));
                        }
                        nextsToDo.clear(pos);
                        pos++;
                    }

                    Head<K> currentElement = heads.pollFirst();
                    if (currentElement == null) {
                        return endOfData();
                    }
                    int position = currentElement.position;
                    nextsToDo.set(position);
                    
                    
                    // advance the iterator from which we took the last element
                    PeekingSkippableIterator<K> peekingSkippableIterator = iterators.get(position);
                    

                    ks.clear();
                    
                    K currentK = currentElement.value;
                    ks.add(currentK);
                    
                    // add all other heads while the key is the same
                    while (heads.size() > 0 && comp(heads.first().value, currentK) == 0) {
                        Head<K> newElement = heads.pollFirst();
                        nextsToDo.set(newElement.position);

                        PeekingSkippableIterator<K> newElementIterator = iterators.get(newElement.position);
                        ks.add(newElement.value);
                    }
                    
                    V v = transform(ks.get(0));
                    if (shouldUse(v, ks)) {
                    	return v;
                    }
                }
			}
			@Override
			public void skipTo(int i) {
				for (SkippableIterator<K> it : iterators) {
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

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		List<Integer> l1 = Lists.newArrayList(1,2,3,3,4,5,6,7,8,9,10);
		List<Integer> l2 = Lists.newArrayList(1,2,3,4,5,6,8,9,10);
		IdentityUnion<Integer> union = new IdentityUnion<Integer>(Skippables.fromIterable(l1), Skippables.fromIterable(l2));
		System.out.println(union);
	}

}
