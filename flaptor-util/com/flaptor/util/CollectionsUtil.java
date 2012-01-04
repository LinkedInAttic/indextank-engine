/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/

package com.flaptor.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * Utility methods for Collections API
 * 
 * @author santip
 *
 */
public class CollectionsUtil {

    /**
     * @return a comparator that provides the natural ordering (i.e. the order defined by T.compareTo(T)
     */
    public static <T extends Comparable<T>> Comparator<T> naturalComparator() {
        return new Comparator<T>() {
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        };
    }
    
    public static <T> Comparator<T> inverseComparator(final Comparator<T> comp) {
        return new Comparator<T>() {
            public int compare(T o1, T o2) {
                return comp.compare(o2, o1);
            }
        };
    }

    public static <T extends Comparable<T>> Iterable<T> mergeIterables(final Iterable<? extends Iterable<? extends T>> iterables) {
        return mergeIterablesUsingComparator(iterables, Ordering.natural());
    }
    
    public static <T> Iterable<T> mergeIterablesUsingComparator(final Iterable<? extends Iterable<? extends T>> iterables, final Comparator<? super T> comparator) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return mergeIterators(Iterables.transform(iterables, new Function<Iterable<? extends T>, Iterator<? extends T>>() {
                    public Iterator<? extends T> apply(Iterable<? extends T> from) {
                        return from.iterator();
                    }
                }).iterator(), comparator);
            }
        };
    }
        
    public static <T extends Comparable<T>> Iterator<T> mergeIterators(Iterator<? extends Iterator<? extends T>> iterators) {
        return mergeIterators(iterators, Ordering.natural());
    }
    
    public static <T> Iterator<T> mergeIterators(Iterator<? extends Iterator<? extends T>> iterators, final Comparator<? super T> comparator) {
        final List<PeekingIterator<? extends T>> peekingIterators = Lists.newArrayList(Iterators.transform(iterators, new Function<Iterator<? extends T>, PeekingIterator<? extends T>>() {
            public PeekingIterator<? extends T> apply(Iterator<? extends T> from) {
                return new PeekingIterator<T>(from);
            }
        }));
        
        return new AbstractIterator<T>() {
            protected T computeNext() {
                int bestIndex = -1;
                T bestElement = null;
                for (int i = 0; i < peekingIterators.size(); i++) {
                    PeekingIterator<? extends T> it = peekingIterators.get(i);
                    if (it.hasNext()) {
                        T element = it.peek();
                        if (bestElement == null || comparator.compare(element, bestElement) < 0) {
                            bestElement = element;
                            bestIndex = i;
                        }
                    }
                }
                if (bestIndex == -1) {
                    return endOfData();
                } else {
                    return peekingIterators.get(bestIndex).next();
                }
            }
        };
    }

    public static class PeekingIterator<T> implements Iterator<T> {
        private Iterator<? extends T> delegate;
        private boolean peeked = false;
        private T next;

        public PeekingIterator(Iterator<? extends T> delegate) {
            this.delegate = delegate;
        }
        
        public T peek() {
            try {
                return peeked ? next : (next = delegate.next());
            } finally {
                peeked = true;
            }
        }
        
        public boolean hasNext() {
            return peeked || delegate.hasNext();
        }
        public T next() {
            try {
                return peeked ? next : delegate.next();
            } finally {
                peeked = false;
            }
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    public static <T> Iterator<T> limit(final Iterator<T> iterator, final int limit) {
        return new Iterator<T>() {
            private int left = limit;
            public boolean hasNext() {
                return iterator.hasNext() && left > 0;
            }
            public T next() {
                left--;
                return iterator.next();
            }
            public void remove() {
                iterator.remove();
            }
        };
    }
    
    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limit) {
        return new AbstractIterable<T>() {
            public Iterator<T> iterator() {
                return limit(iterable.iterator(), limit);
            }
        };
    }
    
    /**
     * Creates a list of sorted entries by copying them from the entrySet of a map. 
     * 
     * @param map the source map
     * @param comparator the desired order for the entries list 
     * 
     * @return a newly created sorted list
     */
    public static <K,V> List<Entry<K,V>> order(Map<K,V> map, java.util.Comparator<Entry<K,V>> comparator) {
        return Ordering.from(comparator).sortedCopy(map.entrySet());
    }

    /**
     * Creates a list of sorted entries by copying them from the entrySet of a map. They are sorted by
     * the natural order of the value (defined by V.compareTo(V)) 
     * 
     * @param map the source map
     * 
     * @return a newly created sorted list
     */
    public static <K,V extends Comparable<V>> List<Entry<K,V>> orderByValue(Map<K,V> map, final boolean desc){
    	return order(map, new Comparator<Entry<K,V>>() {
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				return (desc ? -1 : 1) * o1.getValue().compareTo(o2.getValue());
			}
    	});
    }
    
    /**
     * Creates a list of sorted entries by copying them from the entrySet of a map. They are sorted by
     * the natural order of the key (defined by K.compareTo(K)) 
     * 
     * @param map the source map
     * 
     * @return a newly created sorted list
     */
    public static <K extends Comparable<K>,V> List<Entry<K,V>> orderByKey(Map<K,V> map, final boolean desc){
    	return order(map, new Comparator<Entry<K,V>>() {
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				return (desc ? -1 : 1) * o1.getKey().compareTo(o2.getKey());
			}
    	});
    }
    
    /**
     * given a map of ? -> Integer counting occurrences, this adds one to the occurrence
     * @param <T>
     * @param map
     * @param key
     */
    public static <T> void addOne(Map<T,Integer> map, T key) {
        Integer count = map.get(key);
        if (count == null) count = 0;
        map.put(key, count + 1);
    }
    
    public static <T> Iterable<T> allButOne(Iterable<T> all, final T one) {
        return Iterables.filter(all, new Predicate<T>() {
            public boolean apply(T t) {
                return !Objects.equal(one, t);
            }
        });
        
    }

    
    
    public static Function<String, Integer> PARSE_INT_FUNCTION = new Function<String, Integer>() {
        public Integer apply(String from) {
            return Integer.valueOf(from);
        }
    };
    
    public static Function<String, Long> PARSE_LONG_FUNCTION = new Function<String, Long>() {
        public Long apply(String from) {
            return Long.valueOf(from);
        }
    };
    
    public static Function<String, Float> PARSE_FLOAT_FUNCTION = new Function<String, Float>() {
        public Float apply(String from) {
            return Float.valueOf(from);
        }
    };
    
    public static List<Long> toLongList(Iterable<String> strings) {
        return Lists.newArrayList(Iterables.transform(strings, PARSE_LONG_FUNCTION));
    }
    
    public static List<Integer> toIntList(Iterable<String> strings) {
        return Lists.newArrayList(Iterables.transform(strings, PARSE_INT_FUNCTION));
    }
    
    public static List<Float> toFloatList(Iterable<String> strings) {
        return Lists.newArrayList(Iterables.transform(strings, PARSE_FLOAT_FUNCTION));
    }
    
    public static List<Integer> toIntList(String... strings) {
        return toIntList(Arrays.asList(strings));
    }
    
    public static List<Long> toLongList(String... strings) {
        return toLongList(Arrays.asList(strings));
    }
    
    public static List<Float> toFloatList(String... strings) {
        return toFloatList(Arrays.asList(strings));
    }
    
    public static int[] toIntArray(Iterable<String> strings) {
        List<Integer> list = toIntList(strings);
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    public static long[] toLongArray(Iterable<String> strings) {
        List<Long> list = toLongList(strings);
        long[] array = new long[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    public static float[] toFloatArray(Iterable<String> strings) {
        List<Float> list = toFloatList(strings);
        float[] array = new float[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    public static long[] toLongArray(String... strings) {
        long[] longs = new long[strings.length];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = Long.parseLong(strings[i]);
        }
        return longs;
    }
    public static int[] toIntArray(String... strings) {
        int[] ints = new int[strings.length];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }
    public static float[] toFloatArray(String... strings) {
        float[] floats = new float[strings.length]; 
        for (int i = 0; i < floats.length; i++) {
            floats[i] = Float.parseFloat(strings[i]);
        }
        return floats;
    }

    public static <T extends Comparable<T>> SortedSet<T> sortTopN(
            Iterable<T> iterable, int n) {
        return sortTopN(iterable, n, Ordering.<T> natural());
    }

    public static <T> SortedSet<T> sortTopN(Iterable<T> iterable, int n,
            Comparator<T> comparator) {
        TreeSet<T> r = Sets.newTreeSet(comparator);
        for (T t : iterable) {
            r.add(t);
            if (r.size() > n) {
                r.pollLast();
            }
        }
        return r;
    }
    
}
