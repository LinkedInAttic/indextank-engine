package com.flaptor.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * Utility methods for the {@link Function} class.
 * 
 * @author santip
 */
public class FunctionUtils {

    /**
     * @return a function that will return the toString of its parameter
     */
    public static Function<Object, String> getToString() {
        return new Function<Object, String>() {
            public String apply(Object from) {
                return String.valueOf(from);
            }
        };
    }

    /**
     * @return a function that will invoke the static valueOf method of the given type on
     * its String parameter and will cast the return value to the given type.
     */
    public static <T> Function<String, T> getValueOf(final Class<T> type) {
        return getStaticMethod(type, String.class, type, "valueOf");
    }
    
    /**
     * @return a function that will invoke a static method using the function argument as 
     * a parameter and will cast the return value to the given toType
     */
    public static <F, T> Function<F, T> getStaticMethod(Class<?> type, Class<F> fromType, final Class<T> toType, String methodName) {
        try {
            final Method method = type.getMethod(methodName, fromType);
            return new Function<F, T>() {
                public T apply(F from) {
                    try {
                        return toType.cast(method.invoke(null, from));
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * invokes the given method to the given target using reflection.

     * @return the value returned by the invoked method.
     */
    public static Object invokeMethod(Object target, String method) {
        return getMethodInvoker(method).apply(target);
    }
    
    /**
     * @return a function that invokes the given method using reflection.
     */
    public static Function<Object, Object> getMethodInvoker(final String methodName) {
        return getMethodInvoker(Object.class, methodName);
    }

    /**
     * @return a function that invokes the given method using reflection and will cast it to the given return type.
     */
    public static <T> Function<Object, T> getMethodInvoker(final Class<T> returnType, final String methodName) {
        return new Function<Object, T>() {
            public T apply(Object from) {
                if (from == null) return null;
                try {
                    Method method = from.getClass().getMethod(methodName, new Class[0]);
                    return returnType.cast(method.invoke(from, new Object[0]));
                } catch (SecurityException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Wraps the given function in such a way that it will call the function only 
     * once for each different key and cache the return value for future requests. 
     * 
     * @param function the function to be cached.
     * @return a wrapper function that will cache the inner function's return values
     */
    public static <K,V> Function<K,V> cachedFunction(final Function<K, V> function) {
        return new Function<K, V>() {
            Map<K, V> cache = Maps.newHashMap();
            public V apply(K key) {
                if (!cache.containsKey(key)) {
                    cache.put(key, function.apply(key));
                }
                return cache.get(key);
            }
        };
    }
    
    
}
