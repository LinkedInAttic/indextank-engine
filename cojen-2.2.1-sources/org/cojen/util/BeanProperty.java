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

import java.lang.reflect.Method;

/**
 * Contains information regarding a Bean property.
 *
 * @author Brian S O'Neill
 * @see BeanIntrospector
 */
public interface BeanProperty {
    /**
     * Returns the name of this property.
     */
    String getName();

    /**
     * Returns the primary type of this property.
     */
    Class getType();

    /**
     * Returns additional types of this property, all of which are assignable
     * by the primary type.
     */
    Class[] getCovariantTypes();

    /**
     * Returns a no-arg method used to read the property value, or null if
     * reading is not allowed. The return type matches the type of this
     * property.
     */
    Method getReadMethod();

    /**
     * Returns a one argument method used to write the property value, or null
     * if writing is not allowed. The first argument is the value to set, which
     * is the type of this property.
     */
    Method getWriteMethod();

    /**
     * Returns the count of index types supported by this property.
     */
    int getIndexTypesCount();

    /**
     * Returns a specific index type supported by this property.
     */
    Class getIndexType(int index) throws IndexOutOfBoundsException;

    /**
     * Returns a one argument method used to read the indexed property value,
     * or null if indexed reading is not allowed. The first argument on the
     * returned method is the index value.
     */
    Method getIndexedReadMethod(int index) throws IndexOutOfBoundsException;

    /**
     * Returns a two argument method used to write the indexed property value,
     * or null if indexed writing is not allowed. The first argument on the
     * returned method is the index value. The second argument is the indexed
     * value to set, which is the type of this property.
     */
    Method getIndexedWriteMethod(int index) throws IndexOutOfBoundsException;

    String toString();
}
