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

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * This class implements a simple cache based on the least recently used replacement algorithm.
 * It simply uses LinkedHashMap as it was designed for this purpose.
 * This class is thread-safe.
 */
public class LRUCache<K,V> extends Cache<K,V> implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final LinkedHashMap<K,V> map;

    /**
     * @param maxSize the maximum size of the cache, in number of entries. Must be greater than 0.
     */
	public LRUCache(final int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be greater than 0.");
		map = new LinkedHashMap<K,V>(maxSize, 0.75f, true) {
            private static final long serialVersionUID = 1L;
            //see documentation for LinkedHashMap,
			//this is done so that it knows when to remove the eldest entry
			protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest) {
				return size() > maxSize;
			}

		};
	}

	public synchronized void clear() {
		super.clear();
		map.clear();
	}

	public synchronized V put(K key, V value) {
		return map.put(key, value);
	}

	public synchronized V get(K key) {
		V value = map.get(key);
		if (null == value) {
			markMiss();
		} else {
			markHit();
		}
		return value;
	}
}

