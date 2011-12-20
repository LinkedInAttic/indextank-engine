package com.flaptor.util.collect;

import java.util.LinkedHashMap;
import java.util.Map;

public class CacheMap<K,V> extends LinkedHashMap<K,V> {
    private int maxSize;
    
    public CacheMap(int maxSize) {
        this.maxSize = maxSize;
    }
    
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
