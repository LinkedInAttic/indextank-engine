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

package org.cojen.classfile;

/**
 * 
 * @author Brian S O'Neill
 * @see CodeBuilder#createLocalVariable
 */
public interface LocalVariable {
    /**
     * May return null if this LocalVariable is unnamed.
     */
    String getName();

    void setName(String name);

    TypeDesc getType();

    boolean isDoubleWord();

    /**
     * Returns the number used by this LocalVariable, or -1 if not yet 
     * resolved.
     */
    int getNumber();

    /**
     * Returns the ranges for which this variable is used. This is optional
     * information, and null may be returned.
     *
     * @return an unmodifiable set of {@link LocationRange} objects.
     */
    java.util.Set<LocationRange> getLocationRangeSet();
}
