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

package org.cojen.classfile.constant;

import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.ConstantInfo;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.Descriptor;

/**
 * This class corresponds to the CONSTANT_NameAndType_info structure as defined
 * in section 4.4.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantNameAndTypeInfo extends ConstantInfo {
    private final ConstantUTFInfo mNameConstant;
    private final ConstantUTFInfo mDescriptorConstant;
    
    private final Descriptor mType;
    
    public ConstantNameAndTypeInfo(ConstantUTFInfo nameConstant,
                                   ConstantUTFInfo descConstant) {
        super(TAG_NAME_AND_TYPE);
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;
        mType = Descriptor.parse(descConstant.getValue());
    }

    public ConstantNameAndTypeInfo(ConstantPool cp, 
                                   String name, 
                                   Descriptor type) {
        super(TAG_NAME_AND_TYPE);
        mNameConstant = cp.addConstantUTF(name);
        mDescriptorConstant = cp.addConstantUTF(type.getDescriptor());
        mType = type;
    }
    
    public String getName() {
        return mNameConstant.getValue();
    }

    public Descriptor getType() {
        return mType;
    }

    public int hashCode() {
        return mNameConstant.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantNameAndTypeInfo) {
            ConstantNameAndTypeInfo other = (ConstantNameAndTypeInfo)obj;
            return mNameConstant.equals(other.mNameConstant) && 
                mType.getDescriptor().equals(other.mType.getDescriptor());
        }
        
        return false;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_NameAndType_info: " + getName() + ", " + getType();
    }
}
