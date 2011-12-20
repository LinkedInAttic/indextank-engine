/*
 *  Copyright 2004-2010 Brian S O'Neill
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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Alternative to the standard Bean introspector. One key difference is that
 * this introspector ensures interface properties are properly
 * discovered. Also, indexed properties can have an index of any type.
 *
 * @author Brian S O'Neill
 */
public class BeanIntrospector {
    // Weakly maps Class objects to softly referenced BeanProperty maps.
    private static Map<Class, SoftReference<Map<String, BeanProperty>>> cPropertiesCache =
        new WeakIdentityMap<Class, SoftReference<Map<String, BeanProperty>>>();

    public static void main(String[] args) throws Exception {
        System.out.println(getAllProperties(Class.forName(args[0])));
    }

    /**
     * Returns a Map of all the available properties on a given class including
     * write-only and indexed properties.
     * 
     * @return Map<String, BeanProperty> an unmodifiable mapping of property
     * names (Strings) to BeanProperty objects.
     *
     */
    public static Map<String, BeanProperty> getAllProperties(Class clazz) {
        synchronized (cPropertiesCache) {
            Map<String, BeanProperty> properties;
            SoftReference<Map<String, BeanProperty>> ref = cPropertiesCache.get(clazz);
            if (ref != null) {
                properties = ref.get();
                if (properties != null) {
                    return properties;
                }
            }
            properties = createProperties(clazz);
            cPropertiesCache.put(clazz, new SoftReference<Map<String, BeanProperty>>(properties));
            return properties;
        }
    }

    private static Map<String, BeanProperty> createProperties(Class clazz) {
        if (clazz == null || clazz.isPrimitive()) {
            return Collections.emptyMap();
        }

        Map<String, BeanProperty> properties = new HashMap<String, BeanProperty>();
        fillInProperties(clazz, properties);

        // Properties defined in Object are also available to interfaces.
        if (clazz.isInterface()) {
            fillInProperties(Object.class, properties);
        }

        // Ensure that all implemented interfaces are properly analyzed.
        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            fillInProperties(interfaces[i], properties);
        }

        return Collections.unmodifiableMap(properties);
    }

    /**
     * @param clazz Class to introspect
     * @param properties Receives properties as name->property entries
     */
    private static void fillInProperties(Class clazz, Map<String, BeanProperty> properties) {
        Method[] methods = clazz.getMethods();

        Method method;
        String name;
        Class type;
        Class[] params;
        SimpleProperty property;
        IndexedProperty indexedProperty;

        // Gather non-conflicting "get" accessors
        for (int i=0; i<methods.length; i++) {
            method = methods[i];
            if (Modifier.isStatic(method.getModifiers()) ||
                (type = method.getReturnType()) == void.class ||
                method.getParameterTypes().length > 0 ||
                (name = extractPropertyName(method, "get")) == null) {
                continue;
            }
            if (properties.containsKey(name)) {
                property = (SimpleProperty) properties.get(name);
                SimpleProperty withCovariant = property.addCovariantType(type);
                if (withCovariant == null) {
                    if (property.getReadMethod() != null) {
                        continue;
                    }
                } else {
                    properties.put(name, property = withCovariant);
                    if (type != property.getType() && property.getReadMethod() != null) {
                        // Primary type not changed so don't change read method.
                        continue;
                    }
                }
            } else {
                property = new SimpleProperty(name, type);
                properties.put(name, property);
            }
            property.setReadMethod(method);
        }

        // Gather non-conflicting "is" accessors.
        for (int i=0; i<methods.length; i++) {
            method = methods[i];
            if (Modifier.isStatic(method.getModifiers()) ||
                (type = method.getReturnType()) != boolean.class ||
                method.getParameterTypes().length > 0 ||
                (name = extractPropertyName(method, "is")) == null) {
                continue;
            }
            if (properties.containsKey(name)) {
                property = (SimpleProperty) properties.get(name);
                if (type != property.getType() || property.getReadMethod() != null) {
                    continue;
                }
            } else {
                property = new SimpleProperty(name, type);
                properties.put(name, property);
            }
            property.setReadMethod(method);
        }

        // Gather non-conflicting mutators.
        for (int i=0; i<methods.length; i++) {
            method = methods[i];
            if (Modifier.isStatic(method.getModifiers()) ||
                method.getReturnType() != void.class ||
                (params = method.getParameterTypes()).length != 1 ||
                (name = extractPropertyName(method, "set")) == null) {
                continue;
            }
            type = params[0];
            if (properties.containsKey(name)) {
                property = (SimpleProperty) properties.get(name);
                SimpleProperty withCovariant = property.addCovariantType(type);
                if (withCovariant == null) {
                    if (property.getWriteMethod() != null) {
                        continue;
                    }
                } else {
                    properties.put(name, property = withCovariant);
                    if (type != property.getType() && property.getWriteMethod() != null) {
                        // Primary type not changed so don't change write method.
                        continue;
                    }
                }
            } else {
                property = new SimpleProperty(name, type);
                properties.put(name, property);
            }
            property.setWriteMethod(method);
        }

        // Gather non-conflicting indexed property accessors.
        for (int i=0; i<methods.length; i++) {
            method = methods[i];
            if (Modifier.isStatic(method.getModifiers()) ||
                (type = method.getReturnType()) == void.class ||
                (params = method.getParameterTypes()).length != 1 ||
                (name = extractPropertyName(method, "get")) == null) {
                continue;
            }
            if (properties.containsKey(name)) {
                property = (SimpleProperty) properties.get(name);
                if (type != property.getType()) {
                    continue;
                }
                if (property instanceof IndexedProperty) {
                    indexedProperty = (IndexedProperty) property;
                } else {
                    indexedProperty = new IndexedProperty(property);
                    properties.put(name, indexedProperty);
                }
            } else {
                indexedProperty = new IndexedProperty(name, type);
                properties.put(name, indexedProperty);
            }
            indexedProperty.addIndexedReadMethod(method);
        }

        // Gather non-conflicting indexed property mutators.
        for (int i=0; i<methods.length; i++) {
            method = methods[i];
            if (Modifier.isStatic(method.getModifiers()) ||
                method.getReturnType() != void.class ||
                (params = method.getParameterTypes()).length != 2 ||
                (name = extractPropertyName(method, "set")) == null) {
                continue;
            }
            type = params[1];
            if (properties.containsKey(name)) {
                property = (SimpleProperty) properties.get(name);
                if (type != property.getType()) {
                    continue;
                }
                if (property instanceof IndexedProperty) {
                    indexedProperty = (IndexedProperty) property;
                } else {
                    indexedProperty = new IndexedProperty(property);
                    properties.put(name, indexedProperty);
                }
            } else {
                indexedProperty = new IndexedProperty(name, type);
                properties.put(name, indexedProperty);
            }
            indexedProperty.addIndexedWriteMethod(method);
        }
    }

    /**
     * Returns null if prefix pattern doesn't match
     */
    private static String extractPropertyName(Method method, String prefix) {
        String name = method.getName();
        if (!name.startsWith(prefix)) {
            return null;
        }
        if (name.length() == prefix.length()) {
            return "";
        }
        name = name.substring(prefix.length());
        if (!Character.isUpperCase(name.charAt(0)) || name.indexOf('$') >= 0) {
            return null;
        }

        // Decapitalize the name only if it doesn't begin with two uppercase
        // letters.

        if (name.length() == 1 || !Character.isUpperCase(name.charAt(1))) {
            char chars[] = name.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            name = new String(chars);
        }

        return name.intern();
    }

    private static class SimpleProperty implements BeanProperty {
        private static final Class[] EMPTY = new Class[0];

        private final String mName;
        private final Class mType;
        private final Class[] mCovariantTypes;

        private Method mReadMethod;
        private Method mWriteMethod;

        SimpleProperty(String name, Class type) {
            this(name, type, null);
        }

        SimpleProperty(String name, Class type, Class[] covariantTypes) {
            mName = name;
            mType = type;
            if (covariantTypes == null || covariantTypes.length == 0) {
                mCovariantTypes = EMPTY;
            } else {
                mCovariantTypes = covariantTypes;
            }
        }

        public String getName() {
            return mName;
        }

        public Class getType() {
            return mType;
        }

        public Class[] getCovariantTypes() {
            if (mCovariantTypes.length == 0) {
                return EMPTY;
            }
            return mCovariantTypes.clone();
        }

        public Method getReadMethod() {
            return mReadMethod;
        }

        public Method getWriteMethod() {
            return mWriteMethod;
        }

        public int getIndexTypesCount() {
            return 0;
        }

        public Class getIndexType(int index) {
            throw new IndexOutOfBoundsException();
        }

        public Method getIndexedReadMethod(int index) {
            throw new IndexOutOfBoundsException();
        }

        public Method getIndexedWriteMethod(int index) {
            throw new IndexOutOfBoundsException();
        }

        public String toString() {
            String str = "BeanProperty{name=" + getName() + ", type=" + getType().getName();
            Class[] covariantTypes = getCovariantTypes();
            if (covariantTypes.length > 0) {
                str = str + ", covariantTypes=" + Arrays.toString(covariantTypes);
            }
            return str + '}';
        }

        /**
         * @return null if no change
         */
        SimpleProperty addCovariantType(Class type) {
            if (mType == type) {
                return null;
            }
            for (Class covariant : mCovariantTypes) {
                if (covariant == type) {
                    return null;
                }
            }

            // Find most specialized type.

            Class newType = mType;

            if (mType.isAssignableFrom(type)) {
                newType = type;
            }
            for (Class covariant : mCovariantTypes) {
                if (covariant.isAssignableFrom(type)) {
                    newType = type;
                }
            }

            Class[] newCovariant = new Class[1 + mCovariantTypes.length];
            System.arraycopy(mCovariantTypes, 0, newCovariant, 1, mCovariantTypes.length);

            newCovariant[0] = newType == mType ? type : mType;

            SimpleProperty property = new SimpleProperty(mName, newType, newCovariant);

            property.mReadMethod = mReadMethod;
            property.mWriteMethod = mWriteMethod;

            return property;
        }

        void setReadMethod(Method method) {
            mReadMethod = method;
        }

        void setWriteMethod(Method method) {
            mWriteMethod = method;
        }
    }

    private static class IndexedProperty extends SimpleProperty {
        private Method[] mIndexedReadMethods;
        private Method[] mIndexedWriteMethods;

        IndexedProperty(String name, Class type) {
            super(name, type);
        }

        IndexedProperty(String name, Class type, Class[] covariantTypes) {
            super(name, type, covariantTypes);
        }

        IndexedProperty(BeanProperty property) {
            super(property.getName(), property.getType(), property.getCovariantTypes());
            setReadMethod(property.getReadMethod());
            setWriteMethod(property.getWriteMethod());
        }

        public int getIndexTypesCount() {
            Method[] methods = mIndexedReadMethods;
            if (methods != null) {
                return methods.length;
            }
            methods = mIndexedWriteMethods;
            return methods == null ? 0 : methods.length;
        }

        public Class getIndexType(int index) {
            Method[] methods = mIndexedReadMethods;
            if (methods != null) {
                Method method = methods[0];
                if (method != null) {
                    return method.getParameterTypes()[0];
                }
            }
            methods = mIndexedWriteMethods;
            if (methods != null) {
                Method method = methods[index];
                if (method != null) {
                    return method.getParameterTypes()[0];
                }
            }
            if (index >= getIndexTypesCount()) {
                throw new IndexOutOfBoundsException();
            }
            return null;
        }

        public Method getIndexedReadMethod(int index) {
            return mIndexedReadMethods[index];
        }

        public Method getIndexedWriteMethod(int index) {
            return mIndexedWriteMethods[index];
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("BeanProperty{name=");
            buf.append(getName());
            buf.append(", type=");
            buf.append(getType().getName());
            Class[] covariantTypes = getCovariantTypes();
            if (covariantTypes.length > 0) {
                buf.append(", covariantTypes=");
                buf.append(Arrays.toString(covariantTypes));
            }
            buf.append(", ");
            int count = getIndexTypesCount();
            for (int i=0; i<count; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append("indexType[");
                buf.append(i);
                buf.append("]=");
                buf.append(getIndexType(0));
            }
            buf.append('}');
            return buf.toString();
        }

        IndexedProperty addCovariantType(Class type) {
            SimpleProperty property = super.addCovariantType(type);
            if (property == null) {
                return null;
            }

            IndexedProperty ix = new IndexedProperty(property);
            ix.mIndexedReadMethods = mIndexedReadMethods;
            ix.mIndexedWriteMethods = mIndexedWriteMethods;

            return ix;
        }

        void addIndexedReadMethod(Method method) {
            Class indexType = method.getParameterTypes()[0];
            int count = getIndexTypesCount();
            int i;
            for (i=0; i<count; i++) {
                if (getIndexType(i) == indexType) {
                    break;
                }
            }
            if (i >= count) {
                expandCapactity();
            }
            if (mIndexedReadMethods[i] == null) {
                mIndexedReadMethods[i] = method;
            }
        }

        void addIndexedWriteMethod(Method method) {
            Class indexType = method.getParameterTypes()[0];
            int count = getIndexTypesCount();
            int i;
            for (i=0; i<count; i++) {
                if (getIndexType(i) == indexType) {
                    break;
                }
            }
            if (i >= count) {
                expandCapactity();
            }
            if (mIndexedWriteMethods[i] == null) {
                mIndexedWriteMethods[i] = method;
            }
        }

        private void expandCapactity() {
            int count = getIndexTypesCount();
            Method[] methods = new Method[count + 1];
            if (mIndexedReadMethods != null) {
                System.arraycopy(mIndexedReadMethods, 0, methods, 0, count);
            }
            mIndexedReadMethods = methods;
            methods = new Method[count + 1];
            if (mIndexedWriteMethods != null) {
                System.arraycopy(mIndexedWriteMethods, 0, methods, 0, count);
            }
            mIndexedWriteMethods = methods;
        }
    }
}
