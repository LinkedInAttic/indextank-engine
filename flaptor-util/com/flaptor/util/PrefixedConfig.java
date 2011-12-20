package com.flaptor.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is a wrapper of Config that behaves like a Config object 
 * except for the file related functionality (which is unsupported by this class)
 * that adds a prefix to the wrapped config.
 * 
 * So for instance, if you have:
 * 
 * PrefixedConfig pc = new PrefixedConfig(myconfig, "some.prefix.");
 * 
 * then pc.getString("a") will behave like myconfig.getString("some.prefix.a")
 * also, pc.getKeys() will return only the keys that begin with the given prefix
 * and with that prefix removed. So if c.getKeys() is ["some.prefix.a", "some.prefix.b", 
 * "something.else"] then pc.getKeys() will be ["a", "b"]
 * 
 * @author Santiago Perez (santip)
 */
public class PrefixedConfig extends Config {

    private static final long serialVersionUID = 1L;

    private Config delegate;
    private final String prefix;
    
    public PrefixedConfig(Config delegate, String prefix) {
        this.delegate = delegate;
        this.prefix = prefix;
    }
    
    @Override
    protected synchronized String get(String key) {
        return delegate.get(prefix + key);
    }
    
    @Override
    public synchronized Config set(String key, String value) {
        delegate.set(prefix + key, value);
        return this;
    }
    
    @Override
    public synchronized boolean isDefined(String key) {
        return delegate.isDefined(prefix + key);
    }
    
    @Override
    public Set<String> getKeys() {
        Set<String> allKeys = delegate.getKeys();
        
        Set<String> keys = Sets.newHashSet();
        for (String key : allKeys) {
            if (key.startsWith(prefix)) {
                keys.add(key.substring(prefix.length()));
            }
        }
        return keys;
    }

    @Override
    public Config prefix(String prefix) {
        return new PrefixedConfig(this.delegate, this.prefix + prefix);
    }
    
    @Override
    public Map<String, String> asMap() {
        Set<String> keys = getKeys();
        Map<String, String> map = Maps.newHashMapWithExpectedSize(keys.size());
        for (String key : keys) {
            map.put(key, get(key));
        }
        return map;
    }
    
    @Override
    public String toString() {
        Properties p = new Properties();
        for (Entry<String, String> entry : asMap().entrySet()) {
            p.setProperty(entry.getKey(), entry.getValue());
        }
        String properties = null;
        try {
            OutputStream os = new ByteArrayOutputStream();
            p.store(os, delegate.getFilename() + " prefixed with " + this.prefix);
            properties = os.toString(); 
            os.close();
        } catch (IOException e) {
           properties = p.toString();
        }

        return properties;

    }
    
    @Override
    public void saveToDisk() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void modifyOnDisk(File propertiesDestFile) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String getFilename() {
        throw new UnsupportedOperationException();
    }
    
}
