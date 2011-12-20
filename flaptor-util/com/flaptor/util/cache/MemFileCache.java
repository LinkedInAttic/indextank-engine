package com.flaptor.util.cache;

import java.util.Iterator;

import com.flaptor.util.LRUCache;
import com.flaptor.util.Pair;

/**
 * Cache that first looks in memory and if it doesnt find it goes to disk
 * @author Martin Massera
 *
 * @param <T>
 */
public class MemFileCache<T> implements Iterable<Pair<String, T>>{
   
    final private FileCache<T> fileCache;
    final private LRUCache<String, T> memCache;
    
    public MemFileCache(int memCacheMaxSize, String fileCacheDir) {
        this(memCacheMaxSize, fileCacheDir, 2);
    }
    public MemFileCache(int memCacheMaxSize, String fileCacheDir, int fileCacheLevels) {
        memCache = new LRUCache<String, T>(memCacheMaxSize);
        fileCache = new FileCache<T>(fileCacheDir, fileCacheLevels);
    }
    
    public void remove(String key) {
    	memCache.put(key, null);
    	fileCache.removeItem(key);
    }
    
    public T get(String key) {
        T val = memCache.get(key);
        if (val != null) return val;
        else return fileCache.getItem(key);
    }

    public void put(String key, T value) {
        memCache.put(key, value);
        fileCache.addItem(key, value);
    }

    public Iterator<Pair<String, T>> iterator() {
        final Iterator<String> it = fileCache.iterator();
        return new Iterator<Pair<String,T>>() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Pair<String, T> next() {
                String k = it.next();
                if (k == null) return new Pair<String, T>(null, null);
                else return new Pair<String, T>(k, fileCache.getItem(k));
            }
            public void remove() {
                throw new UnsupportedOperationException("remove not implemented");
            }
        };
    }
    
    
}
