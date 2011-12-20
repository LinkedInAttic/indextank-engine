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

/**
 * This class corresponds to the CONSTANT_String_info structure as defined in
 * section 4.4.3 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantStringInfo extends ConstantInfo {
    private final ConstantUTFInfo mStringConstant;
    
    public ConstantStringInfo(ConstantUTFInfo constant) {
        super(TAG_STRING);
        mStringConstant = constant;
    }

    public ConstantStringInfo(ConstantPool cp, String str) {
        super(TAG_STRING);
        mStringConstant = cp.addConstantUTF(str);
    }
    
    public String getValue() {
        return mStringConstant.getValue();
    }

    public int hashCode() {
        return mStringConstant.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ConstantStringInfo) {
            ConstantStringInfo other = (ConstantStringInfo)obj;
            return mStringConstant.equals(other.mStringConstant);
        }
        return false;
    }
    
    protected boolean hasPriority() {
        return true;
    }

    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mStringConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_String_info: " + getValue();
    }
}
