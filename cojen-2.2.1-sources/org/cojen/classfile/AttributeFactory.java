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

import java.io.DataInput;
import java.io.IOException;

/**
 * Allows custom {@link Attribute Attributes} to be constructed when a
 * {@link ClassFile} is read from a stream. The factory will be invoked only
 * when an unknown type of attribute type is read.
 *
 * @author Brian S O'Neill
 */
public interface AttributeFactory {
    /**
     * Create an attribute, using the provided name to determine which type.
     * Return null if attribute type is unknown.
     *
     * @param cp ConstantPool, needed for constructing attributes
     * @param name Name of attribute
     * @param length Attribute length, in bytes
     * @param din Attribute data source
     */
    Attribute createAttribute(ConstantPool cp, 
                              String name,
                              int length,
                              DataInput din) throws IOException;
}
