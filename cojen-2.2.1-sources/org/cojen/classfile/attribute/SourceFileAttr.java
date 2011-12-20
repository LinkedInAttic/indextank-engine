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
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the SourceFile_attribute structure as defined 
 * in section 4.7.2 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @see org.cojen.classfile.ClassFile
 */
public class SourceFileAttr extends Attribute {

    private final ConstantUTFInfo mSourcefile;
    
    public SourceFileAttr(ConstantPool cp, String fileName) {
        this(cp, SOURCE_FILE, fileName);
    }

    public SourceFileAttr(ConstantPool cp, String name, String fileName) {
        super(cp, name);
        mSourcefile = cp.addConstantUTF(fileName);
    }
    
    public SourceFileAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);
        int index = din.readUnsignedShort();
        if ((length -= 2) > 0) {
            din.skipBytes(length);
        }
        mSourcefile = (ConstantUTFInfo)cp.getConstant(index);
    }

    /**
     * Returns the source file name.
     */
    public ConstantUTFInfo getFileName() {
        return mSourcefile;
    }
    
    /**
     * Returns the length of the source file attribute, which is 2 bytes.
     */
    public int getLength() {
        return 2;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSourcefile.getIndex());
    }
}
