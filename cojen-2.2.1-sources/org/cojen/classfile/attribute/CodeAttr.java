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
import org.cojen.classfile.AttributeFactory;
import org.cojen.classfile.CodeBuffer;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.ExceptionHandler;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.Location;
import org.cojen.classfile.MethodInfo;

/**
 * This class corresponds to the Code_attribute structure as defined in
 * section 4.7.4 of <i>The Java Virtual Machine Specification</i>.
 * To make it easier to create bytecode for the CodeAttr, use the 
 * CodeBuilder.
 *
 * @author Brian S O'Neill
 * @see org.cojen.classfile.Opcode
 * @see org.cojen.classfile.CodeBuilder
 */
public class CodeAttr extends Attribute {

    private CodeBuffer mCodeBuffer;
    private List<Attribute> mAttributes = new ArrayList<Attribute>(2);
    
    private LineNumberTableAttr mLineNumberTable;
    private LocalVariableTableAttr mLocalVariableTable;

    private LineNumberTableAttr mOldLineNumberTable;
    private LocalVariableTableAttr mOldLocalVariableTable;

    private StackMapTableAttr mOldStackMapTable;
    private StackMapTableAttr mStackMapTable;

    public CodeAttr(ConstantPool cp) {
        super(cp, CODE);
    }

    public CodeAttr(ConstantPool cp, String name) {
        super(cp, name);
    }
    
    public CodeAttr(ConstantPool cp, String name, int length,
                    DataInput din, AttributeFactory attrFactory)
        throws IOException
    {
        super(cp, name);

        final int maxStackDepth = din.readUnsignedShort();
        final int maxLocals = din.readUnsignedShort();

        final byte[] byteCodes = new byte[din.readInt()];
        din.readFully(byteCodes);

        int exceptionHandlerCount = din.readUnsignedShort();
        final ExceptionHandler[] handlers = 
            new ExceptionHandler[exceptionHandlerCount];

        for (int i=0; i<exceptionHandlerCount; i++) {
            handlers[i] = ExceptionHandler.readFrom(cp, din);
        }
        
        mCodeBuffer = new CodeBuffer() {
            public int getMaxStackDepth() {
                return maxStackDepth;
            }
            
            public int getMaxLocals() {
                return maxLocals;
            }
            
            public byte[] getByteCodes() {
                return byteCodes.clone();
            }
            
            public ExceptionHandler[] getExceptionHandlers() {
                return handlers.clone();
            }
        };

        int attributeCount = din.readUnsignedShort();
        for (int i=0; i<attributeCount; i++) {
            addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }
    }

    /**
     * Returns null if no CodeBuffer is defined for this CodeAttr.
     */
    public CodeBuffer getCodeBuffer() {
        return mCodeBuffer;
    }

    /**
     * As a side effect of calling this method, new line number and local
     * variable tables are created.
     */
    public void setCodeBuffer(CodeBuffer code) {
        mCodeBuffer = code;
        mOldLineNumberTable = mLineNumberTable;
        mOldLocalVariableTable = mLocalVariableTable;
        mOldStackMapTable = mStackMapTable;
        mAttributes.remove(mLineNumberTable);
        mAttributes.remove(mLocalVariableTable);
        mAttributes.remove(mStackMapTable);
        mLineNumberTable = null;
        mLocalVariableTable = null;
        mStackMapTable = null;
    }
    
    /**
     * Returns the line number in the source code from the given bytecode
     * address (start_pc).
     *
     * @return -1 if no line number is mapped for the start_pc.
     */
    public int getLineNumber(Location start) {
        LineNumberTableAttr table = mOldLineNumberTable;
        if (table == null) {
            table = mLineNumberTable;
        }

        if (table == null || start.getLocation() < 0) {
            return -1;
        } else {
            return table.getLineNumber(start);
        }
    }

    /**
     * Returns local variable info at the given location, for the given number.
     *
     * @return null if unknown
     */
    public LocalVariable getLocalVariable(Location useLocation, int number) {
        int useLoc = useLocation.getLocation();
        if (useLoc < 0) {
            return null;
        } else {
            return getLocalVariable(useLoc, number);
        }
    }

    /**
     * Returns local variable info at the given location, for the given number.
     *
     * @return null if unknown
     */
    public LocalVariable getLocalVariable(int useLocation, int number) {
        LocalVariableTableAttr table = mOldLocalVariableTable;
        if (table == null) {
            table = mLocalVariableTable;
        }

        if (table == null) {
            return null;
        } else {
            return table.getLocalVariable(useLocation, number);
        }
    }

    /**
     * Map a bytecode address (start_pc) to a line number in the source code
     * as a debugging aid.
     */
    public void mapLineNumber(Location start, int line_number) {
        if (mLineNumberTable == null) {
            addAttribute(new LineNumberTableAttr(getConstantPool()));
        }
        mLineNumberTable.addEntry(start, line_number);
    }

    /**
     * Indicate a local variable's use information be recorded in the
     * ClassFile as a debugging aid. If the LocalVariable doesn't provide
     * both a start and end location, then its information is not recorded.
     * This method should be called at most once per LocalVariable instance.
     */
    public void localVariableUse(LocalVariable localVar) {
        if (mLocalVariableTable == null) {
            addAttribute(new LocalVariableTableAttr(getConstantPool()));
        }
        mLocalVariableTable.addEntry(localVar);
    }

    public StackMapTableAttr getStackMapTable() {
        return mStackMapTable;
    }

    public void initialStackMapFrame(MethodInfo method) {
        if (mStackMapTable == null) {
            // FIXME: add one?
        } else {
            mStackMapTable.initialStackMapFrame(method);
        }
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof LineNumberTableAttr) {
            if (mLineNumberTable != null) {
                mAttributes.remove(mLineNumberTable);
            }
            mLineNumberTable = (LineNumberTableAttr)attr;
        } else if (attr instanceof LocalVariableTableAttr) {
            if (mLocalVariableTable != null) {
                mAttributes.remove(mLocalVariableTable);
            }
            mLocalVariableTable = (LocalVariableTableAttr)attr;
        } else if (attr instanceof StackMapTableAttr) {
            if (mStackMapTable != null) {
                mAttributes.remove(mStackMapTable);
            }
            mStackMapTable = (StackMapTableAttr)attr;
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
        int length = 12;

        if (mCodeBuffer != null) {
            length += mCodeBuffer.getByteCodes().length;
            ExceptionHandler[] handlers = mCodeBuffer.getExceptionHandlers();
            if (handlers != null) {
                length += 8 * handlers.length;
            }
        }
        
        int size = mAttributes.size();
        for (int i=0; i<size; i++) {
            length += mAttributes.get(i).getLength();
            length += 6; // attributes have an intial 6 byte length
        }
        
        return length;
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        if (mCodeBuffer == null) {
            throw new IllegalStateException("CodeAttr has no CodeBuffer set");
        }

        ExceptionHandler[] handlers = mCodeBuffer.getExceptionHandlers();
        
        dout.writeShort(mCodeBuffer.getMaxStackDepth());
        dout.writeShort(mCodeBuffer.getMaxLocals());
        
        byte[] byteCodes = mCodeBuffer.getByteCodes();
        dout.writeInt(byteCodes.length);
        dout.write(byteCodes);

        if (handlers != null) {
            int exceptionHandlerCount = handlers.length;
            dout.writeShort(exceptionHandlerCount);

            for (int i=0; i<exceptionHandlerCount; i++) {
                handlers[i].writeTo(dout);
            }
        } else {
            dout.writeShort(0);
        }
        
        int size = mAttributes.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = mAttributes.get(i);
            attr.writeTo(dout);
        }

        mOldLineNumberTable = null;
        mOldLocalVariableTable = null;
        mOldStackMapTable = null;
    }
}
