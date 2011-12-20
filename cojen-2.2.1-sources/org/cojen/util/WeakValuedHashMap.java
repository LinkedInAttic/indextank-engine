/*
 *  Copyright 2006-2010 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A Map that weakly references its values and can be used as a simple cache.
 * WeakValuedHashMap is not thread-safe and must be wrapped with
 * Collections.synchronizedMap to be made thread-safe.
 * <p>
 * Note: Weakly referenced entries may be automatically removed during
 * either accessor or mutator operations, possibly causing a concurrent
 * modification to be detected. Therefore, even if multiple threads are only
 * accessing this map, be sure to synchronize this map first. Also, do not
 * rely on the value returned by size() when using an iterator from this map.
 * The iterators may return less entries than the amount reported by size().
 * 
 * @author Brian S O'Neill
 * @since 2.1
 */
public class WeakValuedHashMap<K, V> extends ReferencedValueHashMap<K, V> {

    /**
     * Constructs a new, empty map with the specified initial 
     * capacity and the specified load factor. 
     *
     * @param      initialCapacity   the initial capacity of the HashMap.
     * @param      loadFactor        the load factor of the HashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public WeakValuedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the HashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public WeakValuedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public WeakValuedHashMap() {
        super();
    }

    /**
     * Constructs a new map with the same mappings as the given map.  The
     * map is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor,
     * which is <tt>0.75</tt>.
     */
    public WeakValuedHashMap(Map<? extends K, ? extends V> t) {
        super(t);
    }

    Entry<K, V> newEntry(int hash, K key, V value, Entry<K, V> next) {
        return new WeakEntry<K, V>(hash, key, value, next);
    }

    static class WeakEntry<K, V> extends ReferencedValueHashMap.Entry<K, V> {

        WeakEntry(int hash, K key, V value, Entry<K, V> next) {
            super(hash, key, value, next);
        }

        WeakEntry(int hash, K key, Reference<V> value, Entry<K, V> next) {
            super(hash, key, value, next);
        }

        Entry newEntry(int hash, K key, Reference<V> value, Entry<K, V> next) {
            return new WeakEntry<K, V>(hash, key, value, next);
        }

        Reference<V> newReference(V value) {
            return new WeakReference<V>(value);
        }
    }
}
