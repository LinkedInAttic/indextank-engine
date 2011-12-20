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

import java.util.ArrayList;
import java.util.List;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.constant.ConstantClassInfo;

/**
 * This class corresponds to the Exceptions_attribute structure as defined in
 * section 4.7.5 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ExceptionsAttr extends Attribute {

    private List<ConstantClassInfo> mExceptions = new ArrayList<ConstantClassInfo>(2);
    
    public ExceptionsAttr(ConstantPool cp) {
        super(cp, EXCEPTIONS);
    }

    public ExceptionsAttr(ConstantPool cp, String name) {
        super(cp, name);
    }
    
    public ExceptionsAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);
        
        int size = din.readUnsignedShort();
        length -= 2;

        for (int i=0; i<size; i++) {
            int index = din.readUnsignedShort();
            length -= 2;
            ConstantClassInfo info = (ConstantClassInfo)cp.getConstant(index);
            addException(info);
        }

        if (length > 0) {
            din.skipBytes(length);
        }
    }

    public ConstantClassInfo[] getExceptions() {
        return mExceptions.toArray(new ConstantClassInfo[mExceptions.size()]);
    }

    public void addException(ConstantClassInfo type) {
        mExceptions.add(type);
    }
    
    public int getLength() {
        return 2 + 2 * mExceptions.size();
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mExceptions.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            ConstantClassInfo info = mExceptions.get(i);
            dout.writeShort(info.getIndex());
        }
    }
}
