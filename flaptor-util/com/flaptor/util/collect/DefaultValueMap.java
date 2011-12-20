package com.flaptor.util.collect;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

/**
 * <p>This abstract class is intended for extension. It behaves like a Map and it 
 * relies in a another Map for the basic map functionality except that if a key
 * is provided through the {@link DefaultValueMap#get(Object)} method that doesn't
 * exist in this map, the abstract method {@link DefaultValueMap#getDefaultValue(Object)}
 * is called to obtain a default value that gets inserted in the map and is returned.
 * 
 * Possible uses are the following:
 *  - To have Float value type and default it to 0.0f so any key can be incremented without
 *    the need to check for existence.
 *  - To lazily load values for keys in any given way. 
 * 
 * @author Santiago Perez (santip)
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public abstract class DefaultValueMap<K, V> extends ForwardingMap<K, V> {

    private static final long serialVersionUID = 1L;
    private Class<K> keyType;
    private final Map<K, V> baseMap;

    /**
     * Creatas a {@link DefaultValueMap} based on an empty {@link HashMap}
     * 
     * @param keyType the class object of the key type, needed to ensure type
     * safety
     */
    public DefaultValueMap(Class<K> keyType) {
        this(keyType, Maps.<K, V>newHashMap());
    }

    /**
     * Creatas a {@link DefaultValueMap} based on the given {@code baseMap}
     * 
     * @param keyType the class object of the key type, needed to ensure type
     * safety
     * @param baseMap the backing map
     */
    public DefaultValueMap(Class<K> keyType, Map<K, V> baseMap) {
        super();
        this.baseMap = baseMap; 
        this.keyType = keyType;
    }
    
    @Override
    protected Map<K, V> delegate() {
        return baseMap;
    }
    
    @Override
    public V get(Object key) {
        if (!containsKey(key) && keyType.isInstance(key)) {
            K castedKey = keyType.cast(key);
            try {
                put(castedKey, getDefaultValue(castedKey));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.get(key);
    }

    /**
     * This method should return the default value for any given
     * key of type K. Any exception thrown will be wrapped in a 
     * RuntimeException and be re-thrown by the {@link DefaultValueMap#get(Object)}
     * method.
     * 
     * @param key the key
     * @return the default value for the given key
     * 
     * @throws Exception
     */
    protected abstract V getDefaultValue(K key) throws Exception;

}
