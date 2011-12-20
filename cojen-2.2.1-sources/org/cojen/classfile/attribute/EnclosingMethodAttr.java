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

package org.cojen.classfile.attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantNameAndTypeInfo;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class EnclosingMethodAttr extends Attribute {
    private final ConstantClassInfo mClass;
    private final ConstantNameAndTypeInfo mMethod;

    public EnclosingMethodAttr(ConstantPool cp,
                               ConstantClassInfo enclosingClass,
                               ConstantNameAndTypeInfo enclosingMethod) {
        this(cp, ENCLOSING_METHOD, enclosingClass, enclosingMethod);
    }

    public EnclosingMethodAttr(ConstantPool cp, String name,
                               ConstantClassInfo enclosingClass,
                               ConstantNameAndTypeInfo enclosingMethod) {
        super(cp, name);
        mClass = enclosingClass;
        mMethod = enclosingMethod;
    }
    
    public EnclosingMethodAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);
        mClass = (ConstantClassInfo)cp.getConstant(din.readUnsignedShort());
        mMethod = (ConstantNameAndTypeInfo)cp.getConstant(din.readUnsignedShort());
        if ((length -= 4) > 0) {
            din.skipBytes(length);
        }
    }

    public ConstantClassInfo getEnclosingClass() {
        return mClass;
    }

    public ConstantNameAndTypeInfo getEnclosingMethod() {
        return mMethod;
    }

    public int getLength() {
        return 4;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mClass.getIndex());
        dout.writeShort(mMethod.getIndex());
    }
}
