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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A LRUcache that removes elements after a timeout 
 */
public class TimeoutLRUCache<K,V> extends LRUCache<K,V> {

    private static final long serialVersionUID = 1L;
    private Map<K,Long> timeoutMap = new HashMap<K,Long>();
    
    public TimeoutLRUCache(int maxSize, final int elementTimeout) {
        super(maxSize);
        final int period = elementTimeout/5 < 500 ? 500 : elementTimeout / 5;
        new Timer(true).scheduleAtFixedRate(new TimerTask(){
            public void run() {
                synchronized (timeoutMap) {
                    Iterator<Map.Entry<K, Long>> it = timeoutMap.entrySet().iterator();
                    while(it.hasNext()) {
                        Map.Entry<K, Long> entry = it.next();
                        if (System.currentTimeMillis() - entry.getValue() > elementTimeout) {
                            it.remove();
                            map.remove(entry.getKey());
                        }
                    }
                }
            }
        }, 0, period);
    }

    @Override
    public void clear() {
        synchronized (timeoutMap) {
            timeoutMap.clear();
            super.clear();
        }
    }

    @Override
    public V put(K key, V value) {
        synchronized (timeoutMap) {
            timeoutMap.put(key, System.currentTimeMillis());
            return super.put(key, value);
        }
    }

}
