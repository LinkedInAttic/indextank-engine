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
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the InnerClasses_attribute structure introduced in
 * JDK1.1. It is not defined in the first edition of 
 * <i>The Java Virual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class InnerClassesAttr extends Attribute {

    private List<Info> mInnerClasses = new ArrayList<Info>();

    public InnerClassesAttr(ConstantPool cp) {
        super(cp, INNER_CLASSES);
    }

    public InnerClassesAttr(ConstantPool cp, String name) {
        super(cp, name);
    }

    public InnerClassesAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int inner_index = din.readUnsignedShort();
            int outer_index = din.readUnsignedShort();
            int name_index = din.readUnsignedShort();
            int af = din.readUnsignedShort();
            
            ConstantClassInfo inner;
            if (inner_index == 0) {
                inner = null;
            } else {
                inner = (ConstantClassInfo)cp.getConstant(inner_index);
            }

            ConstantClassInfo outer;
            if (outer_index == 0) {
                outer = null;
            } else {
                outer = (ConstantClassInfo)cp.getConstant(outer_index);
            }
            
            ConstantUTFInfo innerName;
            if (name_index == 0) {
                innerName = null;
            } else {
                innerName = (ConstantUTFInfo)cp.getConstant(name_index);
            }

            mInnerClasses.add(new Info(inner, outer, innerName, Modifiers.getInstance(af)));
        }
    }

    /**
     * @param inner The full inner class name
     * @param outer The full outer class name
     * @param name The simple name of the inner class, or null if anonymous
     * @param modifiers Modifiers for the inner class
     */
    public void addInnerClass(String inner,
                              String outer,
                              String name,
                              Modifiers modifiers) {
        
        ConstantClassInfo innerInfo = getConstantPool().addConstantClass(inner);
        ConstantClassInfo outerInfo; 
        if (outer == null) {
            outerInfo = null;
        } else {
            outerInfo = getConstantPool().addConstantClass(outer);
        }

        ConstantUTFInfo nameInfo;
        if (name == null) {
            nameInfo = null;
        } else {
            nameInfo = getConstantPool().addConstantUTF(name);
        }

        mInnerClasses.add(new Info(innerInfo, outerInfo, nameInfo, modifiers));
    }

    public Info[] getInnerClassesInfo() {
        return mInnerClasses.toArray(new Info[mInnerClasses.size()]);
    }

    public int getLength() {
        return 2 + 8 * mInnerClasses.size();
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mInnerClasses.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            mInnerClasses.get(i).writeTo(dout);
        }
    }

    public static class Info {
        private ConstantClassInfo mInner;
        private ConstantClassInfo mOuter;
        private ConstantUTFInfo mName;
        private Modifiers mModifiers;

        Info(ConstantClassInfo inner,
             ConstantClassInfo outer,
             ConstantUTFInfo name,
             Modifiers modifiers) {

            mInner = inner;
            mOuter = outer;
            mName = name;
            mModifiers = modifiers;
        }

        /**
         * Returns null if no inner class specified.
         */
        public ConstantClassInfo getInnerClass() {
            return mInner;
        }

        /**
         * Returns null if no outer class specified.
         */
        public ConstantClassInfo getOuterClass() {
            return mOuter;
        }

        /**
         * Returns null if no inner class specified or is anonymous.
         */
        public ConstantUTFInfo getInnerClassName() {
            return mName;
        }

        /**
         * Returns the modifiers.
         */
        public Modifiers getModifiers() {
            return mModifiers;
        }

        public void writeTo(DataOutput dout) throws IOException {
            if (mInner == null) {
                dout.writeShort(0);
            } else {
                dout.writeShort(mInner.getIndex());
            }

            if (mOuter == null) {
                dout.writeShort(0);
            } else {
                dout.writeShort(mOuter.getIndex());
            }

            if (mName == null) {
                dout.writeShort(0);
            } else {
                dout.writeShort(mName.getIndex());
            }
            
            dout.writeShort(mModifiers.getBitmask());
        }
    }
}
