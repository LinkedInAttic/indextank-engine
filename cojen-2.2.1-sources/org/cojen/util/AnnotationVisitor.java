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

/*
 * Copyright 2006 Amazon Technologies, Inc. or its affiliates.
 * Amazon, Amazon.com and Carbonado are trademarks or registered trademarks
 * of Amazon Technologies, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cojen.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Generic annotation visitor. Override methods to capture specific elements.
 *
 * @author Brian S O'Neill
 * @since 2.1
 */
public class AnnotationVisitor<R, P> {
    private final Comparator<Method> mMethodComparator;

    /**
     * @param sort when true, sort annotation members by name (case sensitive)
     */
    public AnnotationVisitor(boolean sort) {
        if (sort) {
            mMethodComparator = BeanComparator
                .forClass(Method.class).orderBy("name").caseSensitive();
        } else {
            mMethodComparator = null;
        }
    }

    /**
     * Visits an annotation by breaking it down into its components and calling
     * various other visit methods.
     *
     * @param value Initial Annotation to visit
     * @param param custom parameter
     * @return custom result, null by default
     */
    public final R visit(Annotation value, P param) {
        return visit(null, 0, value, param);
    }

    /**
     * Visits an annotation by breaking it down into its components and calling
     * various other visit methods.
     *
     * @param name member name, or null if array member or not part of an annotation
     * @param pos position of member in list or array
     * @param value Annotation visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Annotation value, P param) {
        Class<? extends Annotation> annotationType = value.annotationType();

        Method[] methods = annotationType.getMethods();

        if (mMethodComparator != null) {
            Arrays.sort(methods, mMethodComparator);
        }

        int i = 0;
        for (Method m : methods) {
            if (m.getDeclaringClass() != annotationType || m.getParameterTypes().length > 0) {
                continue;
            }

            Class propType = m.getReturnType();
            if (propType == void.class) {
                continue;
            }

            String propName = m.getName();
            Object propValue;
            try {
                propValue = m.invoke(value);
            } catch (Exception e) {
                ThrowUnchecked.fireRootCause(e);
                return null;
            }

            if (propType.isArray()) {
                if (propType.isPrimitive()) {
                    propType = propType.getComponentType();
                    if (propType == int.class) {
                        visit(propName, i, (int[]) propValue, param);
                    } else if (propType == long.class) {
                        visit(propName, i, (long[]) propValue, param);
                    } else if (propType == float.class) {
                        visit(propName, i, (float[]) propValue, param);
                    } else if (propType == double.class) {
                        visit(propName, i, (double[]) propValue, param);
                    } else if (propType == boolean.class) {
                        visit(propName, i, (boolean[]) propValue, param);
                    } else if (propType == byte.class) {
                        visit(propName, i, (byte[]) propValue, param);
                    } else if (propType == short.class) {
                        visit(propName, i, (short[]) propValue, param);
                    } else if (propType == char.class) {
                        visit(propName, i, (char[]) propValue, param);
                    }
                } else if (propValue instanceof String[]) {
                    visit(propName, i, (String[]) propValue, param);
                } else if (propValue instanceof Class[]) {
                    visit(propName, i, (Class[]) propValue, param);
                } else if (propValue instanceof Enum[]) {
                    visit(propName, i, (Enum[]) propValue, param);
                } else if (propValue instanceof Annotation[]) {
                    visit(propName, i, (Annotation[]) propValue, param);
                }
            } else if (propType.isPrimitive()) {
                if (propType == int.class) {
                    visit(propName, i, (Integer) propValue, param);
                } else if (propType == long.class) {
                    visit(propName, i, (Long) propValue, param);
                } else if (propType == float.class) {
                    visit(propName, i, (Float) propValue, param);
                } else if (propType == double.class) {
                    visit(propName, i, (Double) propValue, param);
                } else if (propType == boolean.class) {
                    visit(propName, i, (Boolean) propValue, param);
                } else if (propType == byte.class) {
                    visit(propName, i, (Byte) propValue, param);
                } else if (propType == short.class) {
                    visit(propName, i, (Short) propValue, param);
                } else if (propType == char.class) {
                    visit(propName, i, (Character) propValue, param);
                }
            } else if (propValue instanceof String) {
                visit(propName, i, (String) propValue, param);
            } else if (propValue instanceof Class) {
                visit(propName, i, (Class) propValue, param);
            } else if (propValue instanceof Enum) {
                visit(propName, i, (Enum) propValue, param);
            } else if (propValue instanceof Annotation) {
                visit(propName, i, (Annotation) propValue, param);
            }

            i++;
        }

        return null;
    }

    /**
     * Override to visit ints.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value int visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, int value, P param) {
        return null;
    }

    /**
     * Override to visit longs.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value long visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, long value, P param) {
        return null;
    }

    /**
     * Override to visit floats.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value float visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, float value, P param) {
        return null;
    }

    /**
     * Override to visit doubles.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value double visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, double value, P param) {
        return null;
    }

    /**
     * Override to visit booleans.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value boolean visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, boolean value, P param) {
        return null;
    }

    /**
     * Override to visit bytes.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value byte visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, byte value, P param) {
        return null;
    }

    /**
     * Override to visit shorts.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value short visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, short value, P param) {
        return null;
    }

    /**
     * Override to visit chars.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value char visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, char value, P param) {
        return null;
    }

    /**
     * Override to visit Strings.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value String visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, String value, P param) {
        return null;
    }

    /**
     * Override to visit Classes.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value Class visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Class value, P param) {
        return null;
    }

    /**
     * Override to visit Enums.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value Enum visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Enum value, P param) {
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value Annotation array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Annotation[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value int array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, int[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value long array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, long[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value float array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, float[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value double array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, double[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value boolean array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, boolean[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value byte array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, byte[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value short array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, short[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value char array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, char[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value String array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, String[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value Class array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Class[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }

    /**
     * Visits each array element.
     *
     * @param name member name, or null if array member
     * @param pos position of member in list or array
     * @param value Enum array visited
     * @param param custom parameter
     * @return custom result, null by default
     */
    public R visit(String name, int pos, Enum[] value, P param) {
        for (int i=0; i<value.length; i++) {
            visit(null, i, value[i], param);
        }
        return null;
    }
}
