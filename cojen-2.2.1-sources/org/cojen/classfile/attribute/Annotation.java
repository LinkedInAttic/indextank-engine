/*
 *  Copyright 2005-2010 Brian S O'Neill
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.cojen.classfile.ConstantInfo;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.TypeDesc;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantDoubleInfo;
import org.cojen.classfile.constant.ConstantFloatInfo;
import org.cojen.classfile.constant.ConstantIntegerInfo;
import org.cojen.classfile.constant.ConstantLongInfo;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * Defines the annotation structure used by Java 5 annotations attributes.
 *
 * @author Brian S O'Neill
 */
public class Annotation {

    /** Member value is represented by a ConstantIntegerInfo */
    public static final char MEMBER_TAG_BOOLEAN = 'Z';
    
    /** Member value is represented by a ConstantIntegerInfo */
    public static final char MEMBER_TAG_BYTE = 'B';
    
    /** Member value is represented by a ConstantIntegerInfo */
    public static final char MEMBER_TAG_SHORT = 'S';
    
    /** Member value is represented by a ConstantIntegerInfo */
    public static final char MEMBER_TAG_CHAR = 'C';
    
    /** Member value is represented by a ConstantIntegerInfo */
    public static final char MEMBER_TAG_INT = 'I';
    
    /** Member value is represented by a ConstantLongInfo */
    public static final char MEMBER_TAG_LONG = 'J';
        
    /** Member value is represented by a ConstantFloatInfo */
    public static final char MEMBER_TAG_FLOAT = 'F';

    /** Member value is represented by a ConstantDoubleInfo */
    public static final char MEMBER_TAG_DOUBLE = 'D';

    /** Member value is represented by a ConstantUTFInfo */
    public static final char MEMBER_TAG_STRING = 's';

    /** Member value is represented by a ConstantClassInfo */
    public static final char MEMBER_TAG_CLASS = 'c';

    /** Member value is represented by an EnumConstValue */
    public static final char MEMBER_TAG_ENUM = 'e';

    /** Member value is represented by a MemberValue array */
    public static final char MEMBER_TAG_ARRAY = '[';

    /** Member value is represented by an Annotation */
    public static final char MEMBER_TAG_ANNOTATION = '@';

    private final ConstantPool mCp;
    private ConstantUTFInfo mType;
    private final Map<String, MemberValue> mMemberValues;
    
    public Annotation(ConstantPool cp) {
        mCp = cp;
        mMemberValues = new LinkedHashMap<String, MemberValue>(2);
    }
    
    public Annotation(ConstantPool cp, DataInput din) throws IOException {
        mCp = cp;
        mType = (ConstantUTFInfo)cp.getConstant(din.readUnsignedShort());

        int memberCount = din.readUnsignedShort();
        mMemberValues = new LinkedHashMap<String, MemberValue>(memberCount);
        
        for (int i=0; i<memberCount; i++) {
            String name = ((ConstantUTFInfo)cp.getConstant(din.readUnsignedShort())).getValue();
            mMemberValues.put(name, new MemberValue(cp, din));
        }
    }
    
    public ConstantUTFInfo getTypeConstant() {
        return mType;
    }

    public TypeDesc getType() {
        return TypeDesc.forDescriptor(mType.getValue());
    }
    
    public void setTypeConstant(ConstantUTFInfo type) {
        mType = type;
    }
    
    public void setType(TypeDesc type) {
        setTypeConstant(mCp.addConstantUTF(type.getDescriptor()));
    }

    /**
     * Returns an unmodifiable map of member names (String) to MemberValue
     * objects.
     */
    public Map<String, MemberValue> getMemberValues() {
        return Collections.unmodifiableMap(mMemberValues);
    }
    
    public void putMemberValue(String name, MemberValue mv) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, mv);
    }

    public void putMemberValue(String name, boolean value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, byte value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }
    
    public void putMemberValue(String name, short value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, char value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, int value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, long value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, float value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, double value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, String value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, TypeDesc value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, MemberValue[] value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public void putMemberValue(String name, TypeDesc enumType, String enumName) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(enumType, enumName));
    }

    /**
     * @see #makeAnnotation
     */
    public void putMemberValue(String name, Annotation value) {
        mCp.addConstantUTF(name);
        mMemberValues.put(name, makeMemberValue(value));
    }

    public MemberValue makeMemberValue(boolean value) {
        return new MemberValue(MEMBER_TAG_BOOLEAN, mCp.addConstantInteger(value ? 1 : 0));
    }

    public MemberValue makeMemberValue(byte value) {
        return new MemberValue(MEMBER_TAG_BYTE, mCp.addConstantInteger(value));
    }
    
    public MemberValue makeMemberValue(short value) {
        return new MemberValue(MEMBER_TAG_SHORT, mCp.addConstantInteger(value));
    }

    public MemberValue makeMemberValue(char value) {
        return new MemberValue(MEMBER_TAG_CHAR, mCp.addConstantInteger(value));
    }
    
    public MemberValue makeMemberValue(int value) {
        return new MemberValue(MEMBER_TAG_INT, mCp.addConstantInteger(value));
    }
    
    public MemberValue makeMemberValue(long value) {
        return new MemberValue(MEMBER_TAG_LONG, mCp.addConstantLong(value));
    }
    
    public MemberValue makeMemberValue(float value) {
        return new MemberValue(MEMBER_TAG_FLOAT, mCp.addConstantFloat(value));
    }
    
    public MemberValue makeMemberValue(double value) {
        return new MemberValue(MEMBER_TAG_DOUBLE, mCp.addConstantDouble(value));
    }
    
    public MemberValue makeMemberValue(String value) {
        return new MemberValue(MEMBER_TAG_STRING, mCp.addConstantUTF(value));
    }

    public MemberValue makeMemberValue(TypeDesc value) {
        return new MemberValue(MEMBER_TAG_CLASS, mCp.addConstantUTF(value.getDescriptor()));
    }
    
    public MemberValue makeMemberValue(TypeDesc enumType, String enumName) {
        return new MemberValue(MEMBER_TAG_ENUM,
                               new EnumConstValue(mCp.addConstantUTF(enumType.getDescriptor()),
                                                  mCp.addConstantUTF(enumName)));
    }

    public MemberValue makeMemberValue(MemberValue[] value) {
        return new MemberValue(MEMBER_TAG_ARRAY, value);
    }

    /**
     * @see #makeAnnotation
     */
    public MemberValue makeMemberValue(Annotation value) {
        return new MemberValue(MEMBER_TAG_ANNOTATION, value);
    }

    public Annotation makeAnnotation() {
        return new Annotation(mCp);
    }

    public int getLength() {
        int length = 4;
        for (MemberValue mv : mMemberValues.values()) {
            length += 2 + mv.getLength();
        }
        return length;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mType.getIndex());
        int memberCount = mMemberValues.size();
        dout.writeShort(memberCount);
        for (Map.Entry<String, MemberValue> entry : mMemberValues.entrySet()) {
            dout.writeShort(mCp.addConstantUTF(entry.getKey()).getIndex());
            entry.getValue().writeTo(dout);
        }
    }

    public static class MemberValue {
        private final char mTag;
        private final Object mValue;
        
        public MemberValue(char tag, Object value) {
            switch (mTag = tag) {
            default:
                throw new IllegalArgumentException
                    ("Illegal annotation member value tag: " + mTag);
                
            case MEMBER_TAG_BOOLEAN:
            case MEMBER_TAG_BYTE:
            case MEMBER_TAG_SHORT:
            case MEMBER_TAG_CHAR:
            case MEMBER_TAG_INT:
                if (value instanceof ConstantIntegerInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantIntegerInfo");
                }
                break;

            case MEMBER_TAG_LONG:
                if (value instanceof ConstantLongInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantLongInfo");
                }
                break;

            case MEMBER_TAG_FLOAT:
                if (value instanceof ConstantFloatInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantFloatInfo");
                }
                break;

            case MEMBER_TAG_DOUBLE:
                if (value instanceof ConstantDoubleInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantDoubleInfo");
                }
                break;

            case MEMBER_TAG_CLASS:
                if (value instanceof ConstantUTFInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantUTFInfo");
                }
                break;

            case MEMBER_TAG_STRING:
                if (value instanceof ConstantUTFInfo) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be ConstantUTFInfo");
                }
                break;
                
            case MEMBER_TAG_ENUM:
                if (value instanceof EnumConstValue) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be EnumConstValue");
                }
                break;
                
            case MEMBER_TAG_ARRAY:
                if (value instanceof MemberValue[]) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be MemberValue[]");
                }
                break;
                
            case MEMBER_TAG_ANNOTATION:
                if (value instanceof Annotation) {
                    mValue = value;
                } else {
                    throw new IllegalArgumentException("Value must be Annotation");
                }
                break;
            }
        }
        
        public MemberValue(ConstantPool cp, DataInput din) throws IOException {
            switch (mTag = (char)din.readUnsignedByte()) {
            default:
                throw new IllegalStateException
                    ("Illegal annotation member value tag: " + mTag);
                
            case MEMBER_TAG_BOOLEAN:
            case MEMBER_TAG_BYTE:
            case MEMBER_TAG_SHORT:
            case MEMBER_TAG_CHAR:
            case MEMBER_TAG_INT:
            case MEMBER_TAG_LONG:
            case MEMBER_TAG_FLOAT:
            case MEMBER_TAG_DOUBLE:
            case MEMBER_TAG_CLASS:
            case MEMBER_TAG_STRING:
                mValue = cp.getConstant(din.readUnsignedShort());
                break;
                
            case MEMBER_TAG_ENUM:
                mValue = new EnumConstValue(cp, din);
                break;
                
            case MEMBER_TAG_ARRAY:
                int length = din.readUnsignedShort();
                MemberValue[] values = new MemberValue[length];
                for (int i=0; i<length; i++) {
                    values[i] = new MemberValue(cp, din);
                }
                mValue = values;
                break;
                
            case MEMBER_TAG_ANNOTATION:
                mValue = new Annotation(cp, din);
                break;
            }
        }
        
        public char getTag() {
            return mTag;
        }
        
        public Object getValue() {
            return mValue;
        }
        
        public int getLength() {
            switch (mTag) {
            default:
                return 1;

            case MEMBER_TAG_BOOLEAN:
            case MEMBER_TAG_BYTE:
            case MEMBER_TAG_SHORT:
            case MEMBER_TAG_CHAR:
            case MEMBER_TAG_INT:
            case MEMBER_TAG_LONG:
            case MEMBER_TAG_FLOAT:
            case MEMBER_TAG_DOUBLE:
            case MEMBER_TAG_CLASS:
            case MEMBER_TAG_STRING:
                return 3;
                
            case MEMBER_TAG_ENUM:
                return 1 + ((EnumConstValue)mValue).getLength();
                
            case MEMBER_TAG_ARRAY: {
                MemberValue[] values = (MemberValue[])mValue;
                int length = 3;
                for (int i=0; i<values.length; i++) {
                    length += values[i].getLength();
                }
                return length;
            }
                
            case MEMBER_TAG_ANNOTATION:
                return 1 + ((Annotation)mValue).getLength();
            }
        }
        
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(mTag);

            switch (mTag) {
            case MEMBER_TAG_BOOLEAN:
            case MEMBER_TAG_BYTE:
            case MEMBER_TAG_SHORT:
            case MEMBER_TAG_CHAR:
            case MEMBER_TAG_INT:
            case MEMBER_TAG_LONG:
            case MEMBER_TAG_FLOAT:
            case MEMBER_TAG_DOUBLE:
            case MEMBER_TAG_CLASS:
            case MEMBER_TAG_STRING:
                dout.writeShort(((ConstantInfo)mValue).getIndex());
                break;
                
            case MEMBER_TAG_ENUM:
                ((EnumConstValue)mValue).writeTo(dout);
                break;
                
            case MEMBER_TAG_ARRAY:
                MemberValue[] values = (MemberValue[])mValue;
                dout.writeShort(values.length);
                for (int i=0; i<values.length; i++) {
                    values[i].writeTo(dout);
                }
                break;
                
            case MEMBER_TAG_ANNOTATION:
                ((Annotation)mValue).writeTo(dout);
                break;
            }
        }
    }
    
    public static class EnumConstValue {
        private final ConstantUTFInfo mTypeName;
        private final ConstantUTFInfo mConstName;

        public EnumConstValue(ConstantUTFInfo typeName, ConstantUTFInfo constName) {
            mTypeName = typeName;
            mConstName = constName;
        }

        public EnumConstValue(ConstantPool cp, DataInput din) throws IOException {
            mTypeName = (ConstantUTFInfo)cp.getConstant(din.readUnsignedShort());
            mConstName = (ConstantUTFInfo)cp.getConstant(din.readUnsignedShort());
        }

        public ConstantUTFInfo getTypeName() {
            return mTypeName;
        }

        public ConstantUTFInfo getConstName() {
            return mConstName;
        }

        public int getLength() {
            return 4;
        }
        
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeShort(mTypeName.getIndex());
            dout.writeShort(mConstName.getIndex());
        }
    }
}
