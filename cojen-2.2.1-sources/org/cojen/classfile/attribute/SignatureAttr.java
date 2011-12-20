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
 * This class corresponds to the signature attribute structure, which is used
 * to support generics.
 *
 * @author Brian S O'Neill
 */
public class SignatureAttr extends Attribute {
    private final ConstantUTFInfo mSignature;
    
    public SignatureAttr(ConstantPool cp, ConstantUTFInfo signature) {
        this(cp, SIGNATURE, signature);
    }

    public SignatureAttr(ConstantPool cp, String name, ConstantUTFInfo signature) {
        super(cp, name);
        mSignature = signature;
    }
    
    public SignatureAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);
        int index = din.readUnsignedShort();
        if ((length -= 2) > 0) {
            din.skipBytes(length);
        }
        mSignature = (ConstantUTFInfo) cp.getConstant(index);
    }

    /**
     * Returns the signature.
     */
    public ConstantUTFInfo getSignature() {
        return mSignature;
    }
    
    /**
     * Returns the length of the signature attribute, which is 2 bytes.
     */
    public int getLength() {
        return 2;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSignature.getIndex());
    }
}
