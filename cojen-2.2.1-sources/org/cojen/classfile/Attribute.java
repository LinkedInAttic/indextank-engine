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
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.attribute.CodeAttr;
import org.cojen.classfile.attribute.ConstantValueAttr;
import org.cojen.classfile.attribute.DeprecatedAttr;
import org.cojen.classfile.attribute.EnclosingMethodAttr;
import org.cojen.classfile.attribute.ExceptionsAttr;
import org.cojen.classfile.attribute.InnerClassesAttr;
import org.cojen.classfile.attribute.LineNumberTableAttr;
import org.cojen.classfile.attribute.LocalVariableTableAttr;
import org.cojen.classfile.attribute.RuntimeInvisibleAnnotationsAttr;
import org.cojen.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttr;
import org.cojen.classfile.attribute.RuntimeVisibleAnnotationsAttr;
import org.cojen.classfile.attribute.RuntimeVisibleParameterAnnotationsAttr;
import org.cojen.classfile.attribute.SignatureAttr;
import org.cojen.classfile.attribute.SourceFileAttr;
import org.cojen.classfile.attribute.StackMapTableAttr;
import org.cojen.classfile.attribute.SyntheticAttr;
import org.cojen.classfile.attribute.UnknownAttr;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the attribute_info structure defined in section
 * 4.7 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @see ClassFile
 */
public abstract class Attribute {
    final static Attribute[] NO_ATTRIBUTES = new Attribute[0];

    public static final String CODE = "Code";
    public static final String CONSTANT_VALUE = "ConstantValue";
    public static final String DEPRECATED = "Deprecated";
    public static final String EXCEPTIONS = "Exceptions";
    public static final String INNER_CLASSES = "InnerClasses";
    public static final String LINE_NUMBER_TABLE = "LineNumberTable";
    public static final String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
    public static final String SOURCE_FILE = "SourceFile";
    public static final String SYNTHETIC = "Synthetic";
    public static final String SIGNATURE = "Signature";
    public static final String ENCLOSING_METHOD = "EnclosingMethod";
    public static final String RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    public static final String RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";
    public static final String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS =
        "RuntimeVisibleParamaterAnnotations";
    public static final String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS =
        "RuntimeInvisibleParamaterAnnotations";
    public static final String STACK_MAP_TABLE = "StackMapTable";

    /** The ConstantPool that this attribute is defined against. */
    private final ConstantPool mCp;

    private String mName;
    private ConstantUTFInfo mNameConstant;
    
    protected Attribute(ConstantPool cp, String name) {
        mCp = cp;
        mName = name;
        mNameConstant = cp.addConstantUTF(name);
    }

    /**
     * Returns the ConstantPool that this attribute is defined against.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }
 
    /**
     * Returns the name of this attribute.
     */
    public String getName() {
        return mName;
    }
    
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Some attributes have sub-attributes. Default implementation returns an
     * empty array.
     */
    public Attribute[] getAttributes() {
        return NO_ATTRIBUTES;
    }

    /**
     * Returns the length (in bytes) of this attribute in the class file.
     */
    public abstract int getLength();
    
    /**
     * This method writes the 16 bit name constant index followed by the
     * 32 bit attribute length, followed by the attribute specific data.
     */
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mNameConstant.getIndex());
        dout.writeInt(getLength());
        writeDataTo(dout);
    }

    /**
     * Write just the attribute specific data. The default implementation
     * writes nothing.
     */
    public void writeDataTo(DataOutput dout) throws IOException {
    }

    /**
     * @param attrFactory optional factory for reading custom attributes
     */
    public static Attribute readFrom(ConstantPool cp,
                                     DataInput din,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        int index = din.readUnsignedShort();
        String name = ((ConstantUTFInfo)cp.getConstant(index)).getValue();
        int length = din.readInt();

        attrFactory = new Factory(attrFactory);
        return attrFactory.createAttribute(cp, name, length, din);
    }

    private static class Factory implements AttributeFactory {
        private final AttributeFactory mAttrFactory;

        public Factory(AttributeFactory attrFactory) {
            mAttrFactory = attrFactory;
        }

        public Attribute createAttribute(ConstantPool cp, 
                                         String name,
                                         int length,
                                         DataInput din) throws IOException {
            if (name.length() > 0) {
                switch (name.charAt(0)) {
                case 'C':
                    if (name.equals(CODE)) {
                        return new CodeAttr(cp, name, length, din, mAttrFactory);
                    } else if (name.equals(CONSTANT_VALUE)) {
                        return new ConstantValueAttr(cp, name, length, din);
                    }
                    break;
                case 'D':
                    if (name.equals(DEPRECATED)) {
                        return new DeprecatedAttr(cp, name, length, din);
                    }
                    break;
                case 'E':
                    if (name.equals(EXCEPTIONS)) {
                        return new ExceptionsAttr(cp, name, length, din);
                    } else if (name.equals(ENCLOSING_METHOD)) {
                        return new EnclosingMethodAttr(cp, name, length, din);
                    }
                    break;
                case 'I':
                    if (name.equals(INNER_CLASSES)) {
                        return new InnerClassesAttr(cp, name, length, din);
                    }
                    break;
                case 'L':
                    if (name.equals(LINE_NUMBER_TABLE)) {
                        return new LineNumberTableAttr(cp, name, length, din);
                    } else if (name.equals(LOCAL_VARIABLE_TABLE)) {
                        return new LocalVariableTableAttr(cp, name, length, din);
                    }
                    break;
                case 'R':
                    if (name.equals(RUNTIME_VISIBLE_ANNOTATIONS)) {
                        return new RuntimeVisibleAnnotationsAttr(cp, name, length, din);
                    } else if (name.equals(RUNTIME_INVISIBLE_ANNOTATIONS)) {
                        return new RuntimeInvisibleAnnotationsAttr(cp, name, length, din);
                    } else if (name.equals(RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)) {
                        return new RuntimeVisibleParameterAnnotationsAttr(cp, name, length, din);
                    } else if (name.equals(RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)) {
                        return new RuntimeInvisibleParameterAnnotationsAttr(cp, name, length, din);
                    }
                    break;
                case 'S':
                    if (name.equals(SOURCE_FILE)) {
                        return new SourceFileAttr(cp, name, length, din);
                    } else if (name.equals(SYNTHETIC)) {
                        return new SyntheticAttr(cp, name, length, din);
                    } else if (name.equals(SIGNATURE)) {
                        return new SignatureAttr(cp, name, length, din);
                    } else if (name.equals(STACK_MAP_TABLE)) {
                        return new StackMapTableAttr(cp, name, length, din);
                    }
                    break;
                }
            }

            if (mAttrFactory != null) {
                Attribute attr = mAttrFactory.createAttribute(cp, name, length, din);
                if (attr != null) {
                    return attr;
                }
            }

            // Default case, return attribute that captures the data, but
            // doesn't decode it.
            return new UnknownAttr(cp, name, length, din);
        }
    }
}
