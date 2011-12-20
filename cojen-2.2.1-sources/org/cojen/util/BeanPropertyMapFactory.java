/*
 *  Copyright 2007-2010 Brian S O'Neill
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

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides a simple and efficient means of reading and writing bean properties
 * via a map. Properties which declare throwing checked exceptions are
 * excluded as are properties which are read-only or write-only.
 *
 * @author Brian S O'Neill
 * @see BeanPropertyAccessor
 * @since 2.1
 */
public abstract class BeanPropertyMapFactory<B> {
    private static final Map<Class, SoftReference<BeanPropertyMapFactory>> cFactories =
        new WeakIdentityMap<Class, SoftReference<BeanPropertyMapFactory>>();

    /**
     * Returns a new or cached BeanPropertyMapFactory for the given class.
     */
    public static <B> BeanPropertyMapFactory<B> forClass(Class<B> clazz) {
        synchronized (cFactories) {
            BeanPropertyMapFactory factory;
            SoftReference<BeanPropertyMapFactory> ref = cFactories.get(clazz);
            if (ref != null) {
                factory = ref.get();
                if (factory != null) {
                    return factory;
                }
            }

            final Map<String, BeanProperty> properties = BeanIntrospector.getAllProperties(clazz);
            Map<String, BeanProperty> supportedProperties = properties;

            // Determine which properties are to be excluded.
            for (Map.Entry<String, BeanProperty> entry : properties.entrySet()) {
                BeanProperty property = entry.getValue();
                if (property.getReadMethod() == null ||
                    property.getWriteMethod() == null ||
                    BeanPropertyAccessor.throwsCheckedException(property.getReadMethod()) ||
                    BeanPropertyAccessor.throwsCheckedException(property.getWriteMethod()))
                {
                    // Exclude property.
                    if (supportedProperties == properties) {
                        supportedProperties = new HashMap<String, BeanProperty>(properties);
                    }
                    supportedProperties.remove(entry.getKey());
                }
            }

            if (supportedProperties.size() == 0) {
                factory = Empty.INSTANCE;
            } else {
                factory = new Standard<B>
                    (BeanPropertyAccessor.forClass
                     (clazz, BeanPropertyAccessor.PropertySet.READ_WRITE_UNCHECKED_EXCEPTIONS),
                     supportedProperties);
            }

            cFactories.put(clazz, new SoftReference<BeanPropertyMapFactory>(factory));
            return factory;
        }
    }

    /**
     * Returns a fixed-size map backed by the given bean. Map remove operations
     * are unsupported, as is access to excluded properties.
     *
     * @throws IllegalArgumentException if bean is null
     */
    public static SortedMap<String, Object> asMap(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException();
        }
        BeanPropertyMapFactory factory = forClass(bean.getClass());
        return factory.createMap(bean);
    }

    /**
     * Returns a fixed-size map backed by the given bean. Map remove operations
     * are unsupported, as are put operations on non-existent properties.
     *
     * @throws IllegalArgumentException if bean is null
     */
    public abstract SortedMap<String, Object> createMap(B bean);

    private static class Empty extends BeanPropertyMapFactory {
        static final Empty INSTANCE = new Empty();
        static final SortedMap<String, Object> EMPTY_MAP =
            Collections.unmodifiableSortedMap(new TreeMap<String, Object>());

        private Empty() {
        }

        public SortedMap<String, Object> createMap(Object bean) {
            if (bean == null) {
                throw new IllegalArgumentException();
            }
            return EMPTY_MAP;
        }
    }

    private static class Standard<B> extends BeanPropertyMapFactory<B> {
        final BeanPropertyAccessor mAccessor;
        final SortedSet<String> mPropertyNames;

        public Standard(BeanPropertyAccessor<B> accessor, Map<String, BeanProperty> properties) {
            mAccessor = accessor;

            // Only reveal readable properties.
            SortedSet<String> propertyNames = new TreeSet<String>();
            for (BeanProperty property : properties.values()) {
                if (property.getReadMethod() != null) {
                    propertyNames.add(property.getName());
                }
            }

            mPropertyNames = Collections.unmodifiableSortedSet(propertyNames);
        }

        public SortedMap<String, Object> createMap(B bean) {
            if (bean == null) {
                throw new IllegalArgumentException();
            }
            return new BeanMap<B>(bean, mAccessor, mPropertyNames);
        }
    }

    private static class BeanMap<B> extends AbstractMap<String, Object>
        implements SortedMap<String, Object>
    {
        final B mBean;
        final BeanPropertyAccessor mAccessor;
        final SortedSet<String> mPropertyNames;

        BeanMap(B bean, BeanPropertyAccessor<B> accessor, SortedSet<String> propertyNames) {
            mBean = bean;
            mAccessor = accessor;
            mPropertyNames = propertyNames;
        }

        public Comparator<? super String> comparator() {
            return null;
        }

        public SortedMap<String, Object> subMap(String fromKey, String toKey) {
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.subSet(fromKey, toKey),
                                 fromKey, toKey);
        }

        public SortedMap<String, Object> headMap(String toKey) {
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.headSet(toKey),
                                 null, toKey);
        }

        public SortedMap<String, Object> tailMap(String fromKey) {
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.tailSet(fromKey),
                                 fromKey, null);
        }

        public String firstKey() {
            return mPropertyNames.first();
        }

        public String lastKey() {
            return mPropertyNames.last();
        }

        @Override
        public int size() {
            return mPropertyNames.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return mAccessor.hasReadableProperty((String) key);
        }

        @Override
        public boolean containsValue(Object value) {
            return mAccessor.hasPropertyValue(mBean, value);
        }

        @Override
        public Object get(Object key) {
            return mAccessor.tryGetPropertyValue(mBean, (String) key);
        }

        @Override
        public Object put(String key, Object value) {
            Object old = mAccessor.tryGetPropertyValue(mBean, key);
            mAccessor.setPropertyValue(mBean, key, value);
            return old;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet() {
            return mPropertyNames;
        }

        @Override
        public Collection<Object> values() {
            return new AbstractCollection<Object>() {
                @Override
                public Iterator<Object> iterator() {
                    return new Iterator<Object>() {
                        private final Iterator<String> mPropIterator = keySet().iterator();

                        public boolean hasNext() {
                            return mPropIterator.hasNext();
                        }

                        public Object next() {
                            return get(mPropIterator.next());
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
                
                @Override
                public int size() {
                    return BeanMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean contains(Object v) {
                    return containsValue(v);
                }

                @Override
                public boolean remove(Object e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return new AbstractSet<Map.Entry<String, Object>>() {
                @Override
                public Iterator<Map.Entry<String, Object>> iterator() {
                    return new Iterator<Map.Entry<String, Object>>() {
                        private final Iterator<String> mPropIterator = keySet().iterator();

                        public boolean hasNext() {
                            return mPropIterator.hasNext();
                        }

                        public Map.Entry<String, Object> next() {
                            final String property = mPropIterator.next();
                            final Object value = get(property);

                            return new Map.Entry<String, Object>() {
                                Object mutableValue = value;

                                public String getKey() {
                                    return property;
                                }

                                public Object getValue() {
                                    return mutableValue;
                                }

                                public Object setValue(Object value) {
                                    Object old = BeanMap.this.put(property, value);
                                    mutableValue = value;
                                    return old;
                                }

                                @Override
                                public boolean equals(Object obj) {
                                    if (this == obj) {
                                        return true;
                                    }

                                    if (obj instanceof Map.Entry) {
                                        Map.Entry other = (Map.Entry) obj;

                                        return
                                            (this.getKey() == null ?
                                             other.getKey() == null
                                             : this.getKey().equals(other.getKey()))
                                            &&
                                            (this.getValue() == null ?
                                             other.getValue() == null
                                             : this.getValue().equals(other.getValue()));
                                    }

                                    return false;
                                }

                                @Override
                                public int hashCode() {
                                    return (getKey() == null ? 0 : getKey().hashCode()) ^
                                        (getValue() == null ? 0 : getValue().hashCode());
                                }

                                @Override
                                public String toString() {
                                    return property + "=" + mutableValue;
                                }
                            };
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    return BeanMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean contains(Object e) {
                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) e;
                    String key = entry.getKey();
                    if (BeanMap.this.containsKey(key)) {
                        Object value = BeanMap.this.get(key);
                        return value == null ? entry.getValue() == null
                            : value.equals(entry.getValue());
                    }
                    return false;
                }

                @Override
                public boolean add(Map.Entry<String, Object> e) {
                    BeanMap.this.put(e.getKey(), e.getValue());
                    return true;
                }

                @Override
                public boolean remove(Object e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private static class SubMap<B> extends BeanMap<B> {
        final String mFromKey;
        final String mToKey;

        SubMap(B bean, BeanPropertyAccessor<B> accessor, SortedSet<String> propertyNames,
               String fromKey, String toKey)
        {
            super(bean, accessor, propertyNames);
            mFromKey = fromKey;
            mToKey = toKey;
        }

        @Override
        public SortedMap<String, Object> subMap(String fromKey, String toKey) {
            if (mFromKey != null && mFromKey.compareTo(fromKey) > 0) {
                fromKey = mFromKey;
            }
            if (mToKey != null && mToKey.compareTo(toKey) < 0) {
                toKey = mToKey;
            }
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.subSet(fromKey, toKey),
                                 fromKey, toKey);
        }

        @Override
        public SortedMap<String, Object> headMap(String toKey) {
            if (mToKey != null && mToKey.compareTo(toKey) < 0) {
                toKey = mToKey;
            }
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.headSet(toKey),
                                 mFromKey, toKey);
        }

        @Override
        public SortedMap<String, Object> tailMap(String fromKey) {
            if (mFromKey != null && mFromKey.compareTo(fromKey) > 0) {
                fromKey = mFromKey;
            }
            return new SubMap<B>(mBean, mAccessor, mPropertyNames.tailSet(fromKey),
                                 fromKey, mToKey);
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                return false;
            }
            String strKey = (String) key;
            if (mFromKey != null && mFromKey.compareTo(strKey) > 0) {
                return false;
            }
            if (mToKey != null && mToKey.compareTo(strKey) <= 0) {
                return false;
            }
            return mAccessor.hasReadableProperty(strKey);
        }

        @Override
        public boolean containsValue(Object value) {
            for (String key : keySet()) {
                Object propValue = mAccessor.getPropertyValue(mBean, key);
                if (propValue == null) {
                    if (value == null) {
                        return true;
                    }
                } else if (propValue.equals(value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object get(Object key) {
            if (key == null) {
                return null;
            }
            String strKey = (String) key;
            if (mFromKey != null && mFromKey.compareTo(strKey) > 0) {
                return null;
            }
            if (mToKey != null && mToKey.compareTo(strKey) <= 0) {
                return null;
            }
            return mAccessor.tryGetPropertyValue(mBean, strKey);
        }

        @Override
        public Object put(String key, Object value) {
            if (key != null) {
                if (mFromKey != null && mFromKey.compareTo(key) > 0) {
                    throw rangeError(key);
                }
                if (mToKey != null && mToKey.compareTo(key) <= 0) {
                    throw rangeError(key);
                }
            }
            Object old = mAccessor.tryGetPropertyValue(mBean, key);
            mAccessor.setPropertyValue(mBean, key, value);
            return old;
        }

        private IllegalArgumentException rangeError(String key) {
            String range;
            if (mFromKey == null) {
                range = "," + mToKey;
            } else if (mToKey == null) {
                range = mFromKey + ",";
            } else {
                range = mFromKey + "," + mToKey;
            }

            return new IllegalArgumentException
                ("Key out of range: key=" + key + ", range=[" + range + ')');
        }
    }
}
