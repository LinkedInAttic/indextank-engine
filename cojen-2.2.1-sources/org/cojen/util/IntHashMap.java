/*
 *  Copyright 2005-2010 Brian S O'Neill
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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import java.io.IOException;
import java.io.Serializable;

/**
 * A Map that accepts int or Integer keys only. This class is not thread-safe.
 *
 * @author Brian S O'Neill
 */
public class IntHashMap<V> extends AbstractMap<Integer, V>
    implements Map<Integer, V>, Cloneable, Serializable
{
    /**
     * The hash table data.
     */
    private transient Entry<V> table[];

    /**
     * The total number of mappings in the hash table.
     */
    private transient int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     *
     * @serial
     */
    private float loadFactor;

    /**
     * The number of times this IntHashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the IntHashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the IntHashMap fail-fast.  (See ConcurrentModificationException).
     */
    private transient int modCount = 0;

    /**
     * Constructs a new, empty map with the specified initial 
     * capacity and the specified load factor. 
     *
     * @param      initialCapacity   the initial capacity of the IntHashMap.
     * @param      loadFactor        the load factor of the IntHashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public IntHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        }

        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        }

        if (initialCapacity==0) {
            initialCapacity = 1;
        }

        this.loadFactor = loadFactor;
        table = new Entry[initialCapacity];
        threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the IntHashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public IntHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public IntHashMap() {
        this(101, 0.75f);
    }

    /**
     * Constructs a new map with the same mappings as the given map.  The
     * map is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor,
     * which is <tt>0.75</tt>.
     */
    public IntHashMap(Map<? extends Integer, ? extends V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }
    
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return count;
    }
    
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return count == 0;
    }
    
    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
        Entry tab[] = table;
        
        if (value == null) {
            for (int i = tab.length ; i-- > 0 ;) {
                for (Entry e = tab[i] ; e != null ; e = e.next) {
                    if (e.value == null) {
                        return true;
                    }
                }
            }
        } else {
            for (int i = tab.length ; i-- > 0 ;) {
                for (Entry e = tab[i] ; e != null ; e = e.next) {
                    if (value.equals(e.value)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * 
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @param key key whose presence in this Map is to be tested.
     */
    public boolean containsKey(Integer key) {
        return containsKey(key.intValue());
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * 
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @param key key whose presence in this Map is to be tested.
     */
    public boolean containsKey(int key) {
        Entry tab[] = table;

        int index = (key & 0x7fffffff) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.key == key) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @return the value to which this map maps the specified key.
     * @param key key whose associated value is to be returned.
     */
    public V get(Integer key) {
        return get(key.intValue());
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @return the value to which this map maps the specified key.
     * @param key key whose associated value is to be returned.
     */
    public V get(int key) {
        Entry<V> tab[] = table;
        
        int index = (key & 0x7fffffff) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.key == key) {
                return e.value;
            }
        }
        
        return null;
    }
    
    /**
     * Rehashes the contents of this map into a new <tt>IntHashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     */
    private void rehash() {
        int oldCapacity = table.length;
        Entry oldMap[] = table;
        
        int newCapacity = oldCapacity * 2 + 1;
        Entry<V> newMap[] = new Entry[newCapacity];
        
        modCount++;
        threshold = (int)(newCapacity * loadFactor);
        table = newMap;
        
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry old = oldMap[i] ; old != null ; ) {
                Entry e = old;
                old = old.next;
                
                int index = (e.key & 0x7fffffff) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the IntHashMap previously associated
     *         <tt>null</tt> with the specified key.
     */
    public V put(Integer key, V value) {
        return put(key.intValue(), value);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the IntHashMap previously associated
     *         <tt>null</tt> with the specified key.
     */
    public V put(int key, V value) {
        // Makes sure the key is not already in the IntHashMap.
        Entry<V> tab[] = table;
        int index = 0;
        
        index = (key & 0x7fffffff) % tab.length;
        for (Entry<V> e = tab[index] ; e != null ; e = e.next) {
            if (e.key == key) {
                V old = e.value;
                e.value = value;
                return old;
            }
        }

        modCount++;
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            
            tab = table;
            index = (key & 0x7fffffff) % tab.length;
        }
        
        // Creates the new entry.
        Entry<V> e = new Entry<V>(key, value, tab[index]);
        tab[index] = e;
        count++;
        return null;
    }
    
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key.
     */
    public V remove(Integer key) {
        return remove(key.intValue());
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key.
     */
    public V remove(int key) {
        Entry<V> tab[] = table;
        
        int index = (key & 0x7fffffff) % tab.length;
        
        for (Entry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.key == key) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                
                count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        
        return null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        Entry tab[] = table;
        modCount++;
        for (int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        count = 0;
    }
    
    /**
     * Returns a shallow copy of this <tt>IntHashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    public Object clone() {
        try { 
            IntHashMap t = (IntHashMap) super.clone();
            t.table = new Entry[table.length];
            for (int i = table.length ; i-- > 0 ; ) {
                t.table[i] = (table[i] != null) ? (Entry) table[i].clone() : null;
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
    
    // Views
    
    private transient Set keySet = null;
    private transient Set entrySet = null;
    private transient Collection values = null;
    
    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set<Integer> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<Integer>() {
                public Iterator iterator() {
                    return new IntHashIterator(KEYS);
                }
                public int size() {
                    return count;
                }
                public boolean contains(Object o) {
                    return containsKey(o);
                }
                public boolean remove(Object o) {
                    return IntHashMap.this.remove(o) != null;
                }
                public void clear() {
                    IntHashMap.this.clear();
                }
            };
        }
        return keySet;
    }
    
    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection<V> values() {
        if (values==null) {
            values = new AbstractCollection<V>() {
                public Iterator iterator() {
                    return (Iterator) new IntHashIterator(VALUES);
                }
                public int size() {
                    return count;
                }
                public boolean contains(Object o) {
                    return containsValue(o);
                }
                public void clear() {
                    IntHashMap.this.clear();
                }
            };
        }
        return values;
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     */
    public Set<Map.Entry<Integer, V>> entrySet() {
        if (entrySet==null) {
            entrySet = new AbstractSet<Map.Entry<Integer, V>>() {
                public Iterator iterator() {
                    return (Iterator) new IntHashIterator(ENTRIES);
                }
                
                public boolean contains(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry) o;
                    Integer key = (Integer) entry.getKey();
                    Entry tab[] = table;
                    int hash = (key == null ? 0 : key.hashCode());
                    int index = (hash & 0x7fffffff) % tab.length;
                    
                    for (Entry e = tab[index]; e != null; e = e.next) {
                        if (e.key == hash && e.equals(entry)) {
                            return true;
                        }
                    }
                    return false;
                }
                
                public boolean remove(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry) o;
                    Integer key = (Integer) entry.getKey();
                    Entry tab[] = table;
                    int hash = (key == null ? 0 : key.hashCode());
                    int index = (hash & 0x7fffffff) % tab.length;
                    
                    for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
                        if (e.key == hash && e.equals(entry)) {
                            modCount++;
                            if (prev != null) {
                                prev.next = e.next;
                            } else {
                                tab[index] = e.next;
                            }
                            
                            count--;
                            e.value = null;
                            return true;
                        }
                    }
                    return false;
                }
                
                public int size() {
                    return count;
                }
                
                public void clear() {
                    IntHashMap.this.clear();
                }
            };
        }
        
        return entrySet;
    }
    
    /**
     * IntHashMap collision list entry.
     */
    private static class Entry<V> implements Map.Entry<Integer, V> {
        int key;
        V value;
        Entry<V> next;
        private Integer objectKey;
        
        Entry(int key, V value, Entry<V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
        
        protected Object clone() {
            return new Entry<V>(key, value, (next == null ? null : (Entry<V>) next.clone()));
        }
        
        // Map.Entry Ops 
        
        public Integer getKey() {
            return (objectKey != null) ? objectKey : (objectKey = Integer.valueOf(key));
        }
        
        public V getValue() {
            return value;
        }
        
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            
            return (getKey().equals(e.getKey())) &&
                (value==null ? e.getValue()==null : value.equals(e.getValue()));
        }
        
        public int hashCode() {
            return key ^ (value==null ? 0 : value.hashCode());
        }
        
        public String toString() {
            return String.valueOf(key) + "=" + value;
        }
    }
    
    // Types of Iterators
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;
    
    private class IntHashIterator implements Iterator {
        Entry[] table = IntHashMap.this.table;
        int index = table.length;
        Entry entry;
        Entry lastReturned;
        int type;
        
        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = modCount;
        
        IntHashIterator(int type) {
            this.type = type;
        }
        
        public boolean hasNext() {
            while (entry == null && index > 0) {
                entry = table[--index];
            }
            
            return entry != null;
        }
        
        public Object next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            while (entry == null && index > 0) {
                entry = table[--index];
            }
            
            if (entry != null) {
                Entry e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? e.getKey() : (type == VALUES ? e.value : e);
            }
            throw new NoSuchElementException();
        }
        
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            Entry<V>[] tab = IntHashMap.this.table;
            int index = (lastReturned.key & 0x7fffffff) % tab.length;
            
            for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
                if (e == lastReturned) {
                    modCount++;
                    expectedModCount++;
                    if (prev == null) {
                        tab[index] = e.next;
                    } else {
                        prev.next = e.next;
                    }
                    count--;
                    lastReturned = null;
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }
    }
    
    /**
     * Save the state of the <tt>IntHashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the IntHashMap (the length of the
     *         bucket array) is emitted (int), followed  by the
     *         <i>size</i> of the IntHashMap (the number of key-value
     *         mappings), followed by the key (Object) and value (Object)
     *         for each key-value mapping represented by the IntHashMap
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        
        // Write out number of buckets
        s.writeInt(table.length);
        
        // Write out size (number of Mappings)
        s.writeInt(count);
        
        // Write out keys and values (alternating)
        for (int index = table.length - 1; index >= 0; index--) {
            Entry entry = table[index];
            
            while (entry != null) {
                s.writeInt(entry.key);
                s.writeObject(entry.value);
                entry = entry.next;
            }
        }
    }
    
    /**
     * Reconstitute the <tt>IntHashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        // Read in the threshold, loadfactor, and any hidden stuff
        s.defaultReadObject();
        
        // Read in number of buckets and allocate the bucket array;
        int numBuckets = s.readInt();
        table = new Entry[numBuckets];
        
        // Read in size (number of Mappings)
        int size = s.readInt();
        
        // Read the keys and values, and put the mappings in the IntHashMap
        for (int i=0; i<size; i++) {
            int key = s.readInt();
            V value = (V) s.readObject();
            put(key, value);
        }
    }
    
    int capacity() {
        return table.length;
    }
    
    float loadFactor() {
        return loadFactor;
    }
}
