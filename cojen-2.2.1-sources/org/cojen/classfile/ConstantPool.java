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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantDoubleInfo;
import org.cojen.classfile.constant.ConstantFieldInfo;
import org.cojen.classfile.constant.ConstantFloatInfo;
import org.cojen.classfile.constant.ConstantIntegerInfo;
import org.cojen.classfile.constant.ConstantInterfaceMethodInfo;
import org.cojen.classfile.constant.ConstantLongInfo;
import org.cojen.classfile.constant.ConstantMethodInfo;
import org.cojen.classfile.constant.ConstantNameAndTypeInfo;
import org.cojen.classfile.constant.ConstantStringInfo;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the constant_pool structure as defined in
 * section 4.4 of <i>The Java Virtual Machine Specification</i>.
 * 
 * <p>ConstantPool entries are not written out in the order in which they were
 * added to it. Instead, their ordering is changed such that String, Integer
 * and Float constants are written out first. This provides a slight 
 * optimization for referencing these constants from a code attribute.
 * It means that Opcode.LDC will more likely be used (one-byte index) than 
 * Opcode.LDC_W (two-byte index).
 * 
 * @author Brian S O'Neill
 * @see Opcode
 */
public class ConstantPool {
    // A set of ConstantInfo objects.
    private Map<ConstantInfo, ConstantInfo> mConstants = new HashMap<ConstantInfo, ConstantInfo>();
    // Indexed list of constants.
    private Vector<ConstantInfo> mIndexedConstants;
    private int mEntries;

    // Preserve the order only if the constant pool was read in.
    private boolean mPreserveOrder;

    ConstantPool() {
    }

    private ConstantPool(Vector<ConstantInfo> indexedConstants) {
        mIndexedConstants = indexedConstants;

        int size = indexedConstants.size();
        for (int i=1; i<size; i++) {
            ConstantInfo ci = indexedConstants.get(i);
            if (ci != null) {
                mConstants.put(ci, ci);
                mEntries += ci.getEntryCount();
            }
        }

        mPreserveOrder = true;
    }

    /**
     * Returns a constant from the pool by index, or null if not found. If this
     * constant pool has not yet been written or was not created by the read
     * method, indexes are not assigned, and an IllegalStateException is
     * thrown.
     *
     * @throws ArrayIndexOutOfBoundsException if index is out of range.
     * @throws IllegalStateException if indexes are not assigned
     */
    public ConstantInfo getConstant(int index) {
        if (mIndexedConstants == null) {
            throw new IllegalStateException
                ("Constant pool indexes have not been assigned");
        }

        return mIndexedConstants.get(index);
    }

    /**
     * Returns all the constants in the pool, in no particular order.
     */
    public Set<ConstantInfo> getAllConstants() {
        return Collections.unmodifiableSet(mConstants.keySet());
    }

    /**
     * Returns the number of constants in the pool.
     */    
    public int getSize() {
        return mEntries;
    }

    /**
     * Get or create a constant from the constant pool representing a class.
     */
    public ConstantClassInfo addConstantClass(String className) {
        return (ConstantClassInfo)addConstant(new ConstantClassInfo(this, className));
    }

    /**
     * Get or create a constant from the constant pool representing an array
     * class.
     *
     * @param dim Number of array dimensions.
     */
    public ConstantClassInfo addConstantClass(String className, int dim) {
        return (ConstantClassInfo)addConstant(new ConstantClassInfo(this, className, dim));
    }
    
    /**
     * Get or create a constant from the constant pool representing a class.
     */
    public ConstantClassInfo addConstantClass(TypeDesc type) {
        return (ConstantClassInfo)addConstant(new ConstantClassInfo(this, type));
    }

    /**
     * Get or create a constant from the constant pool representing a field in
     * any class.
     */
    public ConstantFieldInfo addConstantField(String className,
                                              String fieldName, 
                                              TypeDesc type) {
        ConstantInfo ci = new ConstantFieldInfo
            (addConstantClass(className), addConstantNameAndType(fieldName, type));
        return (ConstantFieldInfo)addConstant(ci);
    }

    /**
     * Get or create a constant from the constant pool representing a method
     * in any class. If the method returns void, set ret to null.
     */
    public ConstantMethodInfo addConstantMethod(String className,
                                                String methodName,
                                                TypeDesc ret,
                                                TypeDesc[] params) {
        
        MethodDesc md = MethodDesc.forArguments(ret, params);
        ConstantInfo ci = new ConstantMethodInfo
            (addConstantClass(className), addConstantNameAndType(methodName, md));
        return (ConstantMethodInfo)addConstant(ci);
    }
    
    /**
     * Get or create a constant from the constant pool representing an
     * interface method in any interface.
     */
    public ConstantInterfaceMethodInfo addConstantInterfaceMethod(String className,
                                                                  String methodName,
                                                                  TypeDesc ret,
                                                                  TypeDesc[] params) {
        
        MethodDesc md = MethodDesc.forArguments(ret, params);
        ConstantInfo ci = new ConstantInterfaceMethodInfo
            (addConstantClass(className), addConstantNameAndType(methodName, md));
        return (ConstantInterfaceMethodInfo)addConstant(ci);
    }

    /**
     * Get or create a constant from the constant pool representing a
     * constructor in any class.
     */
    public ConstantMethodInfo addConstantConstructor(String className,
                                                     TypeDesc[] params) {
        return addConstantMethod(className, "<init>", null, params);
    }

    /**
     * Get or create a constant integer from the constant pool.
     */
    public ConstantIntegerInfo addConstantInteger(int value) {
        return (ConstantIntegerInfo)addConstant(new ConstantIntegerInfo(value));
    }

    /**
     * Get or create a constant long from the constant pool.
     */
    public ConstantLongInfo addConstantLong(long value) {
        return (ConstantLongInfo)addConstant(new ConstantLongInfo(value));
    }

    /**
     * Get or create a constant float from the constant pool.
     */
    public ConstantFloatInfo addConstantFloat(float value) {
        return (ConstantFloatInfo)addConstant(new ConstantFloatInfo(value));
    }

    /**
     * Get or create a constant double from the constant pool.
     */
    public ConstantDoubleInfo addConstantDouble(double value) {
        return (ConstantDoubleInfo)addConstant(new ConstantDoubleInfo(value));
    }

    /**
     * Get or create a constant string from the constant pool.
     */
    public ConstantStringInfo addConstantString(String str) {
        return (ConstantStringInfo)addConstant(new ConstantStringInfo(this, str));
    }

    /**
     * Get or create a constant UTF string from the constant pool.
     */
    public ConstantUTFInfo addConstantUTF(String str) {
        return (ConstantUTFInfo)addConstant(new ConstantUTFInfo(str));
    }

    /**
     * Get or create a constant name and type structure from the constant pool.
     */
    public ConstantNameAndTypeInfo addConstantNameAndType(String name,
                                                          Descriptor type) {
        return (ConstantNameAndTypeInfo)addConstant(new ConstantNameAndTypeInfo(this, name, type));
    }

    /**
     * Get or create a constant name and type structure from the constant pool.
     */
    public ConstantNameAndTypeInfo addConstantNameAndType(ConstantUTFInfo nameConstant,
                                                          ConstantUTFInfo descConstant) {
        return (ConstantNameAndTypeInfo)addConstant
            (new ConstantNameAndTypeInfo(nameConstant, descConstant));
    }


    /** 
     * Will only insert into the pool if the constant is not already in the
     * pool. 
     *
     * @return The actual constant in the pool.
     */
    public ConstantInfo addConstant(ConstantInfo constant) {
        ConstantInfo info = mConstants.get(constant);
        if (info != null) {
            return info;
        }
        
        int entryCount = constant.getEntryCount();

        if (mIndexedConstants != null && mPreserveOrder) {
            int size = mIndexedConstants.size();
            mIndexedConstants.setSize(size + entryCount);
            mIndexedConstants.set(size, constant);
            constant.mIndex = size;
        }

        mConstants.put(constant, constant);
        mEntries += entryCount;

        return constant;
    }

    public void writeTo(DataOutput dout) throws IOException {
        // Write out the size (number of entries) of the constant pool.

        int size = getSize() + 1; // add one because constant 0 is reserved
        if (size >= 65535) {
            throw new IllegalStateException
                ("Constant pool entry count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);

        if (mIndexedConstants == null || !mPreserveOrder) {
            mIndexedConstants = new Vector<ConstantInfo>(size);
            mIndexedConstants.setSize(size);
            int index = 1; // one-based constant pool index
            
            // First write constants of higher priority -- String, Integer, 
            // Float.
            // This is a slight optimization. It means that Opcode.LDC will 
            // more likely be used (one-byte index) than Opcode.LDC_W (two-byte
            // index).
            
            Iterator it = mConstants.keySet().iterator();
            while (it.hasNext()) {
                ConstantInfo constant = (ConstantInfo)it.next();
                if (constant.hasPriority()) {
                    constant.mIndex = index;
                    mIndexedConstants.set(index, constant);
                    index += constant.getEntryCount();
                }
            }
            
            // Now write all non-priority constants.
            
            it = mConstants.keySet().iterator();
            while (it.hasNext()) {
                ConstantInfo constant = (ConstantInfo)it.next();
                if (!constant.hasPriority()) {
                    constant.mIndex = index;
                    mIndexedConstants.set(index, constant);
                    index += constant.getEntryCount();
                }
            }
        }

        // Now actually write out the constants since the indexes have been
        // resolved.

        for (int i=1; i<size; i++) {
            Object obj = mIndexedConstants.get(i);
            if (obj != null) {
                ((ConstantInfo)obj).writeTo(dout);
            }
        }
    }

    public static ConstantPool readFrom(DataInput din) throws IOException {
        int size = din.readUnsignedShort();
        Vector<ConstantInfo> constants = new Vector<ConstantInfo>(size);
        constants.setSize(size);

        int index = 1;
        while (index < size) {
            int tag = din.readByte();
            int entryCount = 1;
            ConstantInfo constant;

            switch (tag) {
            case ConstantInfo.TAG_UTF8:
                constant = new ConstantUTFInfo(din.readUTF());
                break;
            case ConstantInfo.TAG_INTEGER:
                constant = new ConstantIntegerInfo(din.readInt());
                break;
            case ConstantInfo.TAG_FLOAT:
                constant = new ConstantFloatInfo(din.readFloat());
                break;
            case ConstantInfo.TAG_LONG:
                constant = new ConstantLongInfo(din.readLong());
                entryCount++;
                break;
            case ConstantInfo.TAG_DOUBLE:
                constant = new ConstantDoubleInfo(din.readDouble());
                entryCount++;
                break;

            case ConstantInfo.TAG_CLASS:
            case ConstantInfo.TAG_STRING:
                constant = new TempEntry(tag, din.readUnsignedShort());
                break;

            case ConstantInfo.TAG_FIELD:
            case ConstantInfo.TAG_METHOD:
            case ConstantInfo.TAG_INTERFACE_METHOD:
            case ConstantInfo.TAG_NAME_AND_TYPE:
                constant = new TempEntry
                    (tag, (din.readShort() << 16) | (din.readUnsignedShort()));
                break;

            default:
                throw new IOException("Invalid constant pool tag: " + tag);
            }

            if (constant instanceof ConstantInfo) {
                ((ConstantInfo)constant).mIndex = index;
            }

            constants.set(index, constant);
            index += entryCount;
        }

        for (index = 1; index < size; index++) {
            resolve(constants, index);
        }

        return new ConstantPool(constants);
    }

    private static ConstantInfo resolve(List<ConstantInfo> constants, int index) {
        ConstantInfo constant = constants.get(index);
        if (constant == null) {
            return null;
        }
        
        if (!(constant instanceof TempEntry)) {
            return constant;
        }

        TempEntry entry = (TempEntry)constant;
        int data = entry.mData;
        int index1 = data & 0xffff;

        ConstantInfo ci1 = constants.get(index1);

        if (ci1 instanceof TempEntry) {
            ci1 = resolve(constants, index1);
        }

        ConstantInfo ci = null;

        switch (entry.mTag) {
        case ConstantInfo.TAG_CLASS:
            ci = new ConstantClassInfo((ConstantUTFInfo)ci1);
            break;
        case ConstantInfo.TAG_STRING:
            ci = new ConstantStringInfo((ConstantUTFInfo)ci1);
            break;

        case ConstantInfo.TAG_FIELD:
        case ConstantInfo.TAG_METHOD:
        case ConstantInfo.TAG_INTERFACE_METHOD:
        case ConstantInfo.TAG_NAME_AND_TYPE:
            int index2 = data >>> 16;
            
            ConstantInfo ci2 = constants.get(index2);
            
            if (ci2 instanceof TempEntry) {
                ci2 = resolve(constants, index2);
            }

            switch (entry.mTag) {
            case ConstantInfo.TAG_FIELD:
                ci = new ConstantFieldInfo
                    ((ConstantClassInfo)ci2, (ConstantNameAndTypeInfo)ci1);
                break;
            case ConstantInfo.TAG_METHOD:
                ci = new ConstantMethodInfo
                    ((ConstantClassInfo)ci2, (ConstantNameAndTypeInfo)ci1);
                break;
            case ConstantInfo.TAG_INTERFACE_METHOD:
                ci = new ConstantInterfaceMethodInfo
                    ((ConstantClassInfo)ci2, (ConstantNameAndTypeInfo)ci1);
                break;
            case ConstantInfo.TAG_NAME_AND_TYPE:
                ci = new ConstantNameAndTypeInfo
                    ((ConstantUTFInfo)ci2, (ConstantUTFInfo)ci1);
                break;
            }

            break;
        }

        ci.mIndex = index;
        constants.set(index, ci);

        return ci;
    }

    private static class TempEntry extends ConstantInfo {
        public int mTag;
        public int mData;

        public TempEntry(int tag, int data) {
            super(-1);
            mTag = tag;
            mData = data;
        }
    }
}
