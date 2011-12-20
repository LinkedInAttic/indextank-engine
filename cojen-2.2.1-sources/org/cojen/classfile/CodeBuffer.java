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
 * An interface that abstracts bytecode from a
 * {@link org.cojen.classfile.attribute.CodeAttr}.
 *
 * @author Brian S O'Neill
 * @see Opcode
 */
public interface CodeBuffer {
    int getMaxStackDepth();

    int getMaxLocals();

    byte[] getByteCodes();

    ExceptionHandler[] getExceptionHandlers();
}
