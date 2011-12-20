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
 * This class represents a descriptor. See section 4.3 of
 * <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 */
public abstract class Descriptor {
    /**
     * Returns a descriptor string, excluding generics.
     */
    public abstract String getDescriptor();

    /**
     * Returns a descriptor string, including any generics.
     */
    // TODO
    //public abstract String getGenericDescriptor();

    public static Descriptor parse(String desc) 
        throws IllegalArgumentException {
        
        if (desc != null && desc.startsWith("(")) {
            return MethodDesc.forDescriptor(desc);
        } else {
            return TypeDesc.forDescriptor(desc);
        }
    }
}
