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

import java.util.ArrayList;
import java.util.List;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import org.cojen.classfile.attribute.Annotation;
import org.cojen.classfile.attribute.AnnotationsAttr;
import org.cojen.classfile.attribute.ConstantValueAttr;
import org.cojen.classfile.attribute.DeprecatedAttr;
import org.cojen.classfile.attribute.RuntimeInvisibleAnnotationsAttr;
import org.cojen.classfile.attribute.RuntimeVisibleAnnotationsAttr;
import org.cojen.classfile.attribute.SignatureAttr;
import org.cojen.classfile.attribute.SyntheticAttr;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the field_info structure as defined in
 * section 4.5 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @see ClassFile
 */
public class FieldInfo {
    private ClassFile mParent;
    private ConstantPool mCp;

    private String mName;
    private TypeDesc mType;

    private Modifiers mModifiers;

    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    private List<Attribute> mAttributes = new ArrayList<Attribute>(2);

    private ConstantValueAttr mConstant;
    
    FieldInfo(ClassFile parent,
              Modifiers modifiers,
              String name,
              TypeDesc type) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = name;
        mType = type;

        mModifiers = modifiers;
        mNameConstant = mCp.addConstantUTF(name);
        mDescriptorConstant = mCp.addConstantUTF(type.getDescriptor());
    }
    
    private FieldInfo(ClassFile parent,
                      int modifier,
                      ConstantUTFInfo nameConstant,
                      ConstantUTFInfo descConstant) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = nameConstant.getValue();
        mType = TypeDesc.forDescriptor(descConstant.getValue());

        mModifiers = Modifiers.getInstance(modifier);
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;
    }

    /**
     * Returns the parent ClassFile for this FieldInfo.
     */
    public ClassFile getClassFile() {
        return mParent;
    }

    /**
     * Returns the name of this field.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the type of this field.
     */
    public TypeDesc getType() {
        return mType;
    }
    
    /**
     * Returns this field's modifiers.
     */
    public Modifiers getModifiers() {
        return mModifiers;
    }

    public void setModifiers(Modifiers modifiers) {
        mModifiers = modifiers;
    }

    /**
     * Returns a constant from the constant pool with this field's name.
     */
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Returns a constant from the constant pool with this field's type 
     * descriptor string.
     * @see TypeDesc
     */
    public ConstantUTFInfo getDescriptorConstant() {
        return mDescriptorConstant;
    }

    /**
     * Returns the constant value for this field or null if no constant set.
     */
    public ConstantInfo getConstantValue() {
        if (mConstant == null) {
            return null;
        } else {
            return mConstant.getConstant();
        }
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all the runtime invisible annotations defined for this class
     * file, or an empty array if none.
     */
    public Annotation[] getRuntimeInvisibleAnnotations() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof RuntimeInvisibleAnnotationsAttr) {
                return ((AnnotationsAttr) attr).getAnnotations();
            }
        }
        return new Annotation[0];
    }

    /**
     * Returns all the runtime visible annotations defined for this class file,
     * or an empty array if none.
     */
    public Annotation[] getRuntimeVisibleAnnotations() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof RuntimeVisibleAnnotationsAttr) {
                return ((AnnotationsAttr) attr).getAnnotations();
            }
        }
        return new Annotation[0];
    }

    /**
     * Add a runtime invisible annotation.
     */
    public Annotation addRuntimeInvisibleAnnotation(TypeDesc type) {
        AnnotationsAttr attr = null;
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute a = mAttributes.get(i);
            if (a instanceof RuntimeInvisibleAnnotationsAttr) {
                attr = (AnnotationsAttr) a;
            }
        }
        if (attr == null) {
            attr = new RuntimeInvisibleAnnotationsAttr(mCp);
            addAttribute(attr);
        }
        Annotation ann = new Annotation(mCp);
        ann.setType(type);
        attr.addAnnotation(ann);
        return ann;
    }

    /**
     * Add a runtime visible annotation.
     */
    public Annotation addRuntimeVisibleAnnotation(TypeDesc type) {
        AnnotationsAttr attr = null;
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute a = mAttributes.get(i);
            if (a instanceof RuntimeVisibleAnnotationsAttr) {
                attr = (AnnotationsAttr) a;
            }
        }
        if (attr == null) {
            attr = new RuntimeVisibleAnnotationsAttr(mCp);
            addAttribute(attr);
        }
        Annotation ann = new Annotation(mCp);
        ann.setType(type);
        attr.addAnnotation(ann);
        return ann;
    }

    /**
     * Returns the signature attribute of this field, or null if none is
     * defined.
     */
    // TODO: Eventually remove this method
    public SignatureAttr getSignatureAttr() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof SignatureAttr) {
                return (SignatureAttr) attr;
            }
        }
        return null;
    }

    /**
     * Set the constant value for this field as an int.
     */
    public void setConstantValue(int value) {
        addAttribute(new ConstantValueAttr(mCp, mCp.addConstantInteger(value)));
    }

    /**
     * Set the constant value for this field as a float.
     */
    public void setConstantValue(float value) {
        addAttribute(new ConstantValueAttr(mCp, mCp.addConstantFloat(value)));
    }

    /**
     * Set the constant value for this field as a long.
     */
    public void setConstantValue(long value) {
        addAttribute(new ConstantValueAttr(mCp, mCp.addConstantLong(value)));
    }

    /**
     * Set the constant value for this field as a double.
     */
    public void setConstantValue(double value) {
        addAttribute(new ConstantValueAttr(mCp, mCp.addConstantDouble(value)));
    }

    /**
     * Set the constant value for this field as a string.
     */
    public void setConstantValue(String value) {
        addAttribute(new ConstantValueAttr(mCp, mCp.addConstantString(value)));
    }
    
    /**
     * Mark this field as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this field as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof ConstantValueAttr) {
            if (mConstant != null) {
                mAttributes.remove(mConstant);
            }
            mConstant = (ConstantValueAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        return mAttributes.toArray(new Attribute[mAttributes.size()]);
    }
    
    /**
     * Returns the length (in bytes) of this object in the class file.
     */
    public int getLength() {
        int length = 8;
        
        int size = mAttributes.size();
        for (int i=0; i<size; i++) {
            length += mAttributes.get(i).getLength();
        }
        
        return length;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mModifiers.getBitmask());
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
        
        int size = mAttributes.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    public String toString() {
        String modStr = mModifiers.toString();
        String typeStr;
        if (modStr.length() == 0) {
            return mType.getFullName() + ' ' + getName();
        } else {
            return modStr + ' ' + mType.getFullName() + ' ' + getName();
        }
    }

    static FieldInfo readFrom(ClassFile parent, 
                              DataInput din,
                              AttributeFactory attrFactory)
        throws IOException
    {
        ConstantPool cp = parent.getConstantPool();

        int modifier = din.readUnsignedShort();
        int index = din.readUnsignedShort();
        ConstantUTFInfo nameConstant = (ConstantUTFInfo)cp.getConstant(index);
        index = din.readUnsignedShort();
        ConstantUTFInfo descConstant = (ConstantUTFInfo)cp.getConstant(index);

        FieldInfo info = new FieldInfo(parent, modifier, nameConstant, descConstant);

        // Read attributes.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            info.addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }

        return info;
    }
}
