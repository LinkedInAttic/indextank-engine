package com.flaptor.util;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class Pairs {

    public static <T> Function<Pair<T, ?>, T> getFirstFunction() {
        return new Function<Pair<T, ?>, T>() {
            public T apply(Pair<T, ?> from) {
                return from.first();
            }
        };
    }
    
    public static <T> Function<Pair<?, T>, T> getLastFunction() {
        return new Function<Pair<?, T>, T>() {
            public T apply(Pair<?, T> from) {
                return from.last();
            }
        };
    }
    
    /**
     * The empty pair (immutable).
     * @see #emptyPair()
     */
    public static final Pair<Object, Object> EMPTY_PAIR = newPair(null, null);

    /**
     * Creates a new pair for the given values
     */
    public static <T1, T2> Pair<T1, T2> newPair(T1 t1, T2 t2) {
        return new Pair<T1, T2>(t1, t2);
    }

    /**
     * Creates a new sorted pair for the given values
     */
    public static <T extends Comparable<T>> Pair<T, T> newSortedPair(T t1, T t2) {
        return sort(newPair(t1, t2));
    }
   
    /**
     * Creates a new sorted pair for the given values
     */
    public static <T> Pair<T, T> newSortedPair(T t1, T t2, Comparator<T> comparator) {
        return sort(newPair(t1, t2), comparator);
    }
    
    /**
     * Returns the empty pair.
     * Unlike the like-named field, this method is parameterized.
     * (Unlike this method, the field does not provide type safety.)
     *
     * @see #EMPTY_PAIR
     */
    @SuppressWarnings("unchecked") 
    // safety is guaranteed by the immutability of pair plus the type safety of null
    public static <T1, T2> Pair<T1, T2> empty() {
        return (Pair<T1, T2>) EMPTY_PAIR;
    }

    /**
     * Creates a new pair with elements in the given pair sorted in such a way that `first` <= `last`
     */
    public static <T extends Comparable<T>> Pair<T, T> sort(Pair<T, T> pair) {
        if (pair.first().compareTo(pair.last()) > 0) {
            return swap(pair);
        } else {
            return pair;
        }
    }

    /**
     * Creates a new pair with elements in the given pair sorted in such a way that `first` <= `last`
     */
    public static <T> Pair<T, T> sort(Pair<T, T> pair, Comparator<T> comparator) {
        if (comparator.compare(pair.first(), pair.last()) > 0) {
            return swap(pair);
        } else {
            return pair;
        }
    }


    /**
     * Creates a new pair with the elements of the given pair swapped
     */
    public static <T1, T2> Pair<T2, T1> swap(Pair<T1, T2> pair) {
        return newPair(pair.last(), pair.first());        
    }

    /**
     * Narrows the generic arguments of the pair.
     */
    @SuppressWarnings("unchecked")  // safety is guaranteed by the immutability of pair
    public static <T1, T2> Pair<T1, T2> narrow(Pair<? extends T1, ? extends T2> pair) {
        return (Pair<T1, T2>) pair;
    }

    public static <T> List<T> toList(Pair<? extends T, ? extends T> pair) {
        List<T> list = Lists.newArrayListWithCapacity(2);
        list.add(pair.first());
        list.add(pair.last());
        return list;
    }

    public static <T> Set<T> toSet(Pair<? extends T, ? extends T> pair) {
        Set<T> set = Sets.newHashSet();
        set.add(pair.first());
        set.add(pair.last());
        return set;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Pair<? extends T, ? extends T> pair, T[] array) {
        if (array.length < 2) {
            array = (T[])Array.newInstance(array.getClass().getComponentType(), 2);
        }
        array[0] = pair.first();
        array[1] = pair.last();
        return array;
    }
    
    

}