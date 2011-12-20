package com.flaptor.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.flaptor.util.collect.SlicingIterable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;



public class Functional {

    
    public static <E> List<E> list() {
        return Lists.newArrayList();
    }

    public static <E> List<E> list(Iterable<E> it) {
        return Lists.newArrayList(it);
    }

    public static <E> Set<E> set() {
        return Sets.newHashSet();
    }

    public static <E> Set<E> set(Iterable<E> it) {
        return Sets.newHashSet(it);
    }

    public static <K,V> Map<K,V> map() {
        return Maps.newHashMap();
    }

    public static <K,V> Map<K,V> map(Map<K,V> map) {
        return Maps.newHashMap(map);
    }

    public static <K,V> Map<K,V> map(Iterable<? extends Map.Entry<? extends K, ? extends V>> items) {
        Map<K, V> m;
        if (items instanceof Collection<?>) {
            int size = ((Collection<?>)items).size();
            m = Maps.newHashMapWithExpectedSize(size);
        } else {
            m = Maps.newHashMap();
        }
        for (Entry<? extends K, ? extends V> kv : items) {
            m.put(kv.getKey(), kv.getValue());
        }
        return m;
    }

    public static <K extends Comparable<K>,V> TreeMap<K,V> treemap() {
        return new TreeMap<K, V>();
    }

    public static <K extends Comparable<K>,V> TreeMap<K,V> treemap(Map<K,V> map) {
        return new TreeMap<K, V>(map);
    }

    public static <K extends Comparable<K>,V> TreeMap<K,V> treemap(Iterable<? extends Map.Entry<? extends K, ? extends V>> items) {
        TreeMap<K, V> m = treemap();
        for (Entry<? extends K, ? extends V> kv : items) {
            m.put(kv.getKey(), kv.getValue());
        }
        return m;
    }

    public static <E> Iterable<E> limiting(Iterable<? extends E> it, int limit) {
        return slicing(it, 0, limit);
    }
    
    public static <E> Iterable<E> slicing(Iterable<? extends E> it, int start) {
        return slicing(it, start, Integer.MAX_VALUE);
    }
    
    public static <E> Iterable<E> slicing(Iterable<? extends E> it, int start, int end) {
        return new SlicingIterable<E>(it, start, end);
    }
 
    public static <E> Iterable<E> filtering(Iterable<E> unfiltered, Predicate<? super E> predicate) {
        return Iterables.filter(unfiltered, predicate);
    }

    public static <E,T> Iterable<E> filtering(Iterable<E> it, final Function<E,T> function, final Predicate<T> predicate) {
        return Iterables.filter(it, new Predicate<E>() {
            @Override
            public boolean apply(E e) {
                T property = function.apply(e);
                return predicate.apply(property);
            }
        });
    }

    public static <F,T> Iterable<T> transforming(Iterable<F> fromIterable, Function<? super F, ? extends T> function) {
        return Iterables.transform(fromIterable, function);
    }

    public static <E extends Comparable<E>> Iterable<E> merging(Iterable<E>... its) {
        return CollectionsUtil.mergeIterables(Arrays.asList(its));
    }
    
    public static <E> Predicate<E> isIn(final Collection<E> collection) {
        return new Predicate<E>() {
            public boolean apply(E e) {
                return collection.contains(e);
            }
        };
    }
    
    public static int size(Iterable<?> it) {
        if (it instanceof Collection<?>) {
            return ((Collection<?>)it).size();
        }
        int size = 0;
        for (@SuppressWarnings("unused") Object o : it) {
            size++;
        }
        return size;
    }

    public static <E> Iterable<E> concatting(Iterable<? extends Iterable<? extends E>> its) {
        return Iterables.concat(its);
    }
    public static <E> Iterable<E> concatting(Iterable<? extends E>... its) {
        return Iterables.concat(its);
    }
    
}
