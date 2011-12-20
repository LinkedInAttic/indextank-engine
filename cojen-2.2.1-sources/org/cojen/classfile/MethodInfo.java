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
import org.cojen.classfile.attribute.CodeAttr;
import org.cojen.classfile.attribute.DeprecatedAttr;
import org.cojen.classfile.attribute.EnclosingMethodAttr;
import org.cojen.classfile.attribute.ExceptionsAttr;
import org.cojen.classfile.attribute.RuntimeInvisibleAnnotationsAttr;
import org.cojen.classfile.attribute.RuntimeVisibleAnnotationsAttr;
import org.cojen.classfile.attribute.SignatureAttr;
import org.cojen.classfile.attribute.SourceFileAttr;
import org.cojen.classfile.attribute.SyntheticAttr;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the method_info data structure as defined in
 * section 4.6 of <i>The Java Virtual Machine Specification</i>. 
 * To make it easier to create bytecode for a method's CodeAttr, the 
 * CodeBuilder class is provided.
 * 
 * @author Brian S O'Neill
 * @see ClassFile
 * @see CodeBuilder
 */
public class MethodInfo {
    private ClassFile mParent;
    private ConstantPool mCp;

    private String mName;
    private MethodDesc mDesc;
    
    private Modifiers mModifiers;

    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    private List<Attribute> mAttributes = new ArrayList<Attribute>(2);

    private CodeAttr mCode;
    private ExceptionsAttr mExceptions;

    private int mAnonymousInnerClassCount = 0;

    MethodInfo(ClassFile parent,
               Modifiers modifiers,
               String name,
               MethodDesc desc) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = name;
        mDesc = desc;
        
        mModifiers = modifiers;
        mNameConstant = mCp.addConstantUTF(name);
        mDescriptorConstant = mCp.addConstantUTF(desc.getDescriptor());

        if (!modifiers.isAbstract() && !modifiers.isNative()) {
            addAttribute(new CodeAttr(mCp));
        }
    }

    private MethodInfo(ClassFile parent,
                       int modifier,
                       ConstantUTFInfo nameConstant,
                       ConstantUTFInfo descConstant) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = nameConstant.getValue();
        mDesc = MethodDesc.forDescriptor(descConstant.getValue());

        mModifiers = Modifiers.getInstance(modifier);
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;
    }
    
    /**
     * Returns the parent ClassFile for this MethodInfo.
     */
    public ClassFile getClassFile() {
        return mParent;
    }

    /**
     * Returns the name of this method.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns a MethodDesc which describes return and parameter types
     * of this method.
     */
    public MethodDesc getMethodDescriptor() {
        return mDesc;
    }

    /**
     * Returns this method's modifiers.
     */
    public Modifiers getModifiers() {
        return mModifiers;
    }

    public void setModifiers(Modifiers modifiers) {
        mModifiers = modifiers;
    }
    
    /**
     * Returns a constant from the constant pool with this method's name.
     */
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Returns a constant from the constant pool with this method's type 
     * descriptor string.
     * @see MethodDesc
     */
    public ConstantUTFInfo getDescriptorConstant() {
        return mDescriptorConstant;
    }

    /**
     * Returns the exceptions that this method is declared to throw.
     */
    public TypeDesc[] getExceptions() {
        if (mExceptions == null) {
            return new TypeDesc[0];
        }

        ConstantClassInfo[] classes = mExceptions.getExceptions();
        TypeDesc[] types = new TypeDesc[classes.length];
        for (int i=0; i<types.length; i++) {
            types[i] = classes[i].getType();
        }

        return types;
    }

    /**
     * Returns a CodeAttr object used to manipulate the method code body, or
     * null if this method is abstract or native.
     */
    public CodeAttr getCodeAttr() {
        return mCode;
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
     * Returns the signature attribute of this method, or null if none is
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
     * Add a declared exception that this method may throw.
     */
    public void addException(TypeDesc type) {
        if (mExceptions == null) {
            addAttribute(new ExceptionsAttr(mCp));
        }
        // TODO: Special handling for generics
        ConstantClassInfo cci = mCp.addConstantClass(type);
        mExceptions.addException(cci);
    }

    /**
     * Add an inner class to this method.
     *
     * @param innerClassName Optional short inner class name.
     */
    public ClassFile addInnerClass(String innerClassName) {
        return addInnerClass(innerClassName, (String)null);
    }

    /**
     * Add an inner class to this method.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClass Super class.
     */
    public ClassFile addInnerClass(String innerClassName, Class superClass) {
        return addInnerClass(innerClassName, superClass.getName());
    }

    /**
     * Add an inner class to this method.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClassName Full super class name.
     */
    public ClassFile addInnerClass(String innerClassName, String superClassName) {
        ClassFile inner;
        if (innerClassName == null) {
            inner = mParent.addInnerClass(null, null, superClassName);
        } else {
            String fullInnerClassName = mParent.getClassName() + '$' +
                (++mAnonymousInnerClassCount) + innerClassName;
            inner = mParent.addInnerClass(fullInnerClassName, innerClassName, superClassName);
        }

        if (mParent.getMajorVersion() >= 49) {
            inner.addAttribute(new EnclosingMethodAttr
                               (mCp, mCp.addConstantClass(mParent.getClassName()),
                                mCp.addConstantNameAndType(mNameConstant, mDescriptorConstant)));
        }

        return inner;
    }

    /**
     * Mark this method as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this method as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof CodeAttr) {
            if (mCode != null) {
                mAttributes.remove(mCode);
            }
            mCode = (CodeAttr)attr;
            mCode.initialStackMapFrame(this);
        } else if (attr instanceof ExceptionsAttr) {
            if (mExceptions != null) {
                mAttributes.remove(mExceptions);
            }
            mExceptions = (ExceptionsAttr)attr;
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
            try {
                attr.writeTo(dout);
            } catch (IllegalStateException e) {
                IllegalStateException e2 = 
                    new IllegalStateException(e.getMessage() + ": " + toString());
                try {
                    e2.initCause(e);
                } catch (NoSuchMethodError e3) {
                }
                throw e2;
            }
        }
    }

    public String toString() {
        String str = mDesc.toMethodSignature(getName(), mModifiers.isVarArgs());
        String modStr = mModifiers.toString();
        if (modStr.length() > 0) {
            str = modStr + ' ' + str;
        }
        return str;
    }

    static MethodInfo readFrom(ClassFile parent, 
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

        MethodInfo info = new MethodInfo(parent, modifier,
                                         nameConstant, descConstant);

        // Read attributes.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            info.addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }

        return info;
    }
}
