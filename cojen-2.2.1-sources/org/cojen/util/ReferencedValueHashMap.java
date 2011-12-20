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
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Map that references its values and can be used as a simple cache.
 * Instances are not thread-safe and must be wrapped with
 * Collections.synchronizedMap to be made thread-safe.
 * <p>
 * Note: Referenced entries may be automatically removed during
 * either accessor or mutator operations, possibly causing a concurrent
 * modification to be detected. Therefore, even if multiple threads are only
 * accessing this map, be sure to synchronize this map first. Also, do not
 * rely on the value returned by size() when using an iterator from this map.
 * The iterators may return less entries than the amount reported by size().
 * 
 * @author Brian S O'Neill
 */
public abstract class ReferencedValueHashMap<K, V> extends AbstractMap<K, V>
    implements Map<K, V>, Cloneable
{
    private transient Entry<K, V>[] table;
    private transient int count;
    private int threshold;
    private final float loadFactor;
    private transient volatile int modCount;

    // Views

    private transient Set<K> keySet;
    private transient Set<Map.Entry<K, V>> entrySet;
    private transient Collection<V> values;

    /**
     * Constructs a new, empty map with the specified initial 
     * capacity and the specified load factor. 
     *
     * @param      initialCapacity   the initial capacity of the HashMap.
     * @param      loadFactor        the load factor of the HashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public ReferencedValueHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        }

        if (initialCapacity == 0) {
            initialCapacity = 1;
        }

        this.loadFactor = loadFactor;
        this.table = new Entry[initialCapacity];
        this.threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the HashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public ReferencedValueHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public ReferencedValueHashMap() {
        this(11, 0.75f);
    }

    /**
     * Constructs a new map with the same mappings as the given map.  The
     * map is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor,
     * which is <tt>0.75</tt>.
     */
    public ReferencedValueHashMap(Map<? extends K, ? extends V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    public int size() {
        return this.count;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public boolean containsValue(Object value) {
        if (value == null) {
            value = KeyFactory.NULL;
        }

        Entry[] tab = this.table;

        for (int i = tab.length ; i-- > 0 ;) {
            for (Entry e = tab[i], prev = null; e != null; e = e.next) {
                Object entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[i] = e.next;
                    }
                    this.count--;
                } else if (value.equals(entryValue)) {
                    return true;
                } else {
                    prev = e;
                }
            }
        }

        return false;
    }

    public boolean containsKey(Object key) {
        Entry<K, V>[] tab = this.table;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7fffffff) % tab.length;
            for (Entry<K, V> e = tab[index], prev = null; e != null; e = e.next) {
                if (e.get() == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else if (e.hash == hash && key.equals(e.key)) {
                    return true;
                } else {
                    prev = e;
                }
            }
        } else {
            for (Entry<K, V> e = tab[0], prev = null; e != null; e = e.next) {
                if (e.get() == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[0] = e.next;
                    }
                    this.count--;
                } else if (e.key == null) {
                    return true;
                } else {
                    prev = e;
                }
            }
        }

        return false;
    }

    public V get(Object key) {
        Entry<K, V>[] tab = this.table;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7fffffff) % tab.length;

            for (Entry<K, V> e = tab[index], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else if (e.hash == hash && key.equals(e.key)) {
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        } else {
            for (Entry<K, V> e = tab[0], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    }
                    else {
                        tab[0] = e.next;
                    }
                    this.count--;
                } else if (e.key == null) {
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        }

        return null;
    }

    /**
     * Scans the contents of this map, removing all entries that have a
     * cleared soft value.
     */
    private void cleanup() {
        Entry<K, V>[] tab = this.table;

        for (int i = tab.length ; i-- > 0 ;) {
            for (Entry<K, V> e = tab[i], prev = null; e != null; e = e.next) {
                if (e.get() == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[i] = e.next;
                    }
                    this.count--;
                } else {
                    prev = e;
                }
            }
        }
    }

    /**
     * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     */
    private void rehash() {
        int oldCapacity = this.table.length;
        Entry<K, V>[] oldMap = this.table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry<K, V>[] newMap = new Entry[newCapacity];

        this.modCount++;
        this.threshold = (int)(newCapacity * this.loadFactor);
        this.table = newMap;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry<K, V> old = oldMap[i] ; old != null ; ) {
                Entry<K, V> e = old;
                old = old.next;

                // Only copy entry if its value hasn't been cleared.
                if (e.get() == null) {
                    this.count--;
                } else {
                    int index = (e.hash & 0x7fffffff) % newCapacity;
                    e.next = newMap[index];
                    newMap[index] = e;
                }
            }
        }
    }

    public V put(K key, V value) {
        if (value == null) {
            value = (V) KeyFactory.NULL;
        }

        // Makes sure the key is not already in the HashMap.
        Entry<K, V>[] tab = this.table;
        int hash;
        int index;

        if (key != null) {
            hash = key.hashCode();
            index = (hash & 0x7fffffff) % tab.length;
            for (Entry<K, V> e = tab[index], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else if (e.hash == hash && key.equals(e.key)) {
                    e.setValue(value);
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        } else {
            hash = 0;
            index = 0;
            for (Entry<K, V> e = tab[0], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[0] = e.next;
                    }
                    this.count--;
                } else if (e.key == null) {
                    e.setValue(value);
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        }

        this.modCount++;

        if (this.count >= this.threshold) {
            // Cleanup the table if the threshold is exceeded.
            cleanup();
        }

        if (this.count >= this.threshold) {
            // Rehash the table if the threshold is still exceeded.
            rehash();
            tab = this.table;
            index = (hash & 0x7fffffff) % tab.length;
        }

        // Creates the new entry.
        Entry<K, V> e = newEntry(hash, key, (V)value, tab[index]);
        tab[index] = e;
        this.count++;
        return null;
    }

    public V remove(Object key) {
        Entry<K, V>[] tab = this.table;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7fffffff) % tab.length;

            for (Entry<K, V> e = tab[index], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else if (e.hash == hash && key.equals(e.key)) {
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;

                    e.setValue(null);
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        } else {
            for (Entry<K, V> e = tab[0], prev = null; e != null; e = e.next) {
                V entryValue = e.get();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[0] = e.next;
                    }
                    this.count--;
                } else if (e.key == null) {
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[0] = e.next;
                    }
                    this.count--;

                    e.setValue(null);
                    return (entryValue == KeyFactory.NULL) ? null : entryValue;
                } else {
                    prev = e;
                }
            }
        }

        return null;
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        Iterator i = t.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<K, V> e = (Map.Entry<K, V>) i.next();
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        Entry[] tab = this.table;
        this.modCount++;
        for (int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        this.count = 0;
    }

    public Object clone() {
        try { 
            ReferencedValueHashMap t = (ReferencedValueHashMap)super.clone();
            t.table = new Entry[this.table.length];
            for (int i = this.table.length ; i-- > 0 ; ) {
                t.table[i] = (this.table[i] != null) 
                    ? (Entry)this.table[i].clone() : null;
            }
            t.keySet = null;
            t.entrySet = null;
            t.values = null;
            t.modCount = 0;
            return t;
        } catch (CloneNotSupportedException e) { 
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = new AbstractSet<K>() {
                public Iterator iterator() {
                    return createHashIterator(WeakIdentityMap.KEYS);
                }
                public int size() {
                    return ReferencedValueHashMap.this.count;
                }
                public boolean contains(Object o) {
                    return containsKey(o);
                }
                public boolean remove(Object o) {
                    if (o == null) {
                        if (ReferencedValueHashMap.this.containsKey(null)) {
                            ReferencedValueHashMap.this.remove(null);
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return ReferencedValueHashMap.this.remove(o) != null;
                    }
                }
                public void clear() {
                    ReferencedValueHashMap.this.clear();
                }
                public String toString() {
                    return WeakIdentityMap.toString(this);
                }
            };
        }
        return this.keySet;
    }

    public Collection<V> values() {
        if (this.values==null) {
            this.values = new AbstractCollection<V>() {
                public Iterator iterator() {
                    return createHashIterator(WeakIdentityMap.VALUES);
                }
                public int size() {
                    return ReferencedValueHashMap.this.count;
                }
                public boolean contains(Object o) {
                    return containsValue(o);
                }
                public void clear() {
                    ReferencedValueHashMap.this.clear();
                }
                public String toString() {
                    return WeakIdentityMap.toString(this);
                }
            };
        }
        return this.values;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        if (this.entrySet==null) {
            this.entrySet = new AbstractSet<Map.Entry<K, V>>() {
                public Iterator iterator() {
                    return createHashIterator(WeakIdentityMap.ENTRIES);
                }

                public boolean contains(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)o;
                    Object key = entry.getKey();

                    Entry[] tab = ReferencedValueHashMap.this.table;
                    int hash = key == null ? 0 : key.hashCode();
                    int index = (hash & 0x7fffffff) % tab.length;

                    for (Entry e = tab[index], prev = null; e != null; e = e.next) {
                        Object entryValue = e.get();
                        
                        if (entryValue == null) {
                            // Clean up after a cleared Reference.
                            ReferencedValueHashMap.this.modCount++;
                            if (prev != null) {
                                prev.next = e.next;
                            } else {
                                tab[index] = e.next;
                            }
                            ReferencedValueHashMap.this.count--;
                        } else if (e.hash == hash && e.equals(entry)) {
                            return true;
                        } else {
                            prev = e;
                        }
                    }

                    return false;
                }

                public boolean remove(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)o;
                    Object key = entry.getKey();
                    Entry[] tab = ReferencedValueHashMap.this.table;
                    int hash = key == null ? 0 : key.hashCode();
                    int index = (hash & 0x7fffffff) % tab.length;

                    for (Entry e = tab[index], prev = null; e != null; e = e.next) {
                        Object entryValue = e.get();

                        if (entryValue == null) {
                            // Clean up after a cleared Reference.
                            ReferencedValueHashMap.this.modCount++;
                            if (prev != null) {
                                prev.next = e.next;
                            } else {
                                tab[index] = e.next;
                            }
                            ReferencedValueHashMap.this.count--;
                        } else if (e.hash == hash && e.equals(entry)) {
                            ReferencedValueHashMap.this.modCount++;
                            if (prev != null) {
                                prev.next = e.next;
                            } else {
                                tab[index] = e.next;
                            }
                            ReferencedValueHashMap.this.count--;

                            e.setValue(null);
                            return true;
                        } else {
                            prev = e;
                        }
                    }
                    return false;
                }

                public int size() {
                    return ReferencedValueHashMap.this.count;
                }

                public void clear() {
                    ReferencedValueHashMap.this.clear();
                }

                public String toString() {
                    return WeakIdentityMap.toString(this);
                }
            };
        }

        return this.entrySet;
    }

    public String toString() {
        // Cleanup stale entries first, so as not to allocate a larger than
        // necessary StringBuffer.
        cleanup();
        return WeakIdentityMap.toString(this);
    }

    abstract Entry<K, V> newEntry(int hash, K key, V value, Entry<K, V> next);

    private Iterator createHashIterator(int type) {
        if (this.count == 0) {
            return Collections.EMPTY_SET.iterator();
        } else {
            return new HashIterator(type);
        }
    }

    /**
     * Collision list entry.
     */
    abstract static class Entry<K, V> implements Map.Entry<K, V> {
        int hash;
        K key;
        Entry<K, V> next;

        private Reference<V> value;

        Entry(int hash, K key, V value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = newReference(value);
            this.next = next;
        }

        Entry(int hash, K key, Reference<V> value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        // Map.Entry Ops 

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            V value = this.value.get();
            return value == KeyFactory.NULL ? null : value;
        }

        public V setValue(V value) {
            V oldValue = getValue();
            this.value = newReference(value == null ? ((V) KeyFactory.NULL) : value);
            return oldValue;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            return equals((Map.Entry)obj);
        }
        
        boolean equals(Map.Entry e) {
            Object thisValue = get();
            if (thisValue == null) {
                return false;
            } else if (thisValue == KeyFactory.NULL) {
                thisValue = null;
            }
            return (this.key == null ? e.getKey() == null : this.key.equals(e.getKey())) &&
                (thisValue == null ? e.getValue() == null : thisValue.equals(e.getValue()));
        }

        public int hashCode() {
            return this.hash ^ get().hashCode();
        }

        public String toString() {
            return this.key + "=" + getValue();
        }

        protected Object clone() {
            return newEntry(this.hash, this.key, (Reference)this.value, 
                            (this.next == null ? null : (Entry)this.next.clone()));
        }

        abstract Entry newEntry(int hash, K key, Reference<V> value, Entry<K, V> next);

        abstract Reference<V> newReference(V value);

        // Like getValue(), except does not convert NULL to null.
        V get() {
            return this.value.get();
        }
    }

    private class HashIterator implements Iterator {
        private final int type;
        private final Entry[] table;

        private int index;

        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the value. Its existence will prevent the soft
        // value from being cleared.
        private Object entryValue;
        private Entry entry;

        private Entry last;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = ReferencedValueHashMap.this.modCount;

        HashIterator(int type) {
            this.table = ReferencedValueHashMap.this.table;
            this.type = type;
            this.index = table.length;
        }

        public boolean hasNext() {
            while (this.entry == null || (this.entryValue = this.entry.get()) == null) {
                if (this.entry != null) {
                    // Clean up after a cleared Reference.
                    remove(this.entry);
                    this.entry = this.entry.next;
                }

                if (this.entry == null) {
                    if (this.index <= 0) {
                        return false;
                    } else {
                        this.entry = this.table[--this.index];
                    }
                }
            }

            return true;
        }

        public Object next() {
            if (ReferencedValueHashMap.this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            this.last = this.entry;
            this.entry = this.entry.next;

            return this.type == WeakIdentityMap.KEYS ? this.last.getKey() :
                (this.type == WeakIdentityMap.VALUES ? this.last.getValue() : this.last);
        }

        public void remove() {
            if (this.last == null) {
                throw new IllegalStateException();
            }
            if (ReferencedValueHashMap.this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            remove(this.last);
            this.last = null;
        }

        private void remove(Entry toRemove) {
            Entry[] tab = this.table;
            int index = (toRemove.hash & 0x7fffffff) % tab.length;

            for (Entry e = tab[index], prev = null; e != null; e = e.next) {
                if (e == toRemove) {
                    ReferencedValueHashMap.this.modCount++;
                    expectedModCount++;
                    if (prev == null) {
                        tab[index] = e.next;
                    } else {
                        prev.next = e.next;
                    }
                    ReferencedValueHashMap.this.count--;
                    return;
                } else {
                    prev = e;
                }
            }
            throw new ConcurrentModificationException();
        }
    }
}
