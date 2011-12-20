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

import org.cojen.classfile.attribute.CodeAttr;
import org.cojen.classfile.constant.ConstantClassInfo;
import org.cojen.classfile.constant.ConstantFieldInfo;

/**
 * This class is used as an aid in generating code for a method.
 * It controls the max stack, local variable allocation, labels and bytecode.
 * 
 * @author Brian S O'Neill
 */
public class CodeBuilder extends AbstractCodeAssembler implements CodeBuffer, CodeAssembler {
    private final CodeAttr mCodeAttr;
    private final ClassFile mClassFile;
    private final ConstantPool mCp;

    private final InstructionList mInstructions;

    private final LocalVariable mThisReference;
    private final LocalVariable[] mParameters;

    private final int mTarget;

    private final boolean mSaveLineNumberInfo;
    private final boolean mSaveLocalVariableInfo;

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     */
    public CodeBuilder(MethodInfo info) {
        this(info, true, false);
    }

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     *
     * @param saveLineNumberInfo When set false, all calls to mapLineNumber
     * are ignored. By default, this value is true.
     * @param saveLocalVariableInfo When set true, all local variable
     * usage information is saved in the ClassFile. By default, this value
     * is false.
     * @see #mapLineNumber
     */
    public CodeBuilder(MethodInfo info, boolean saveLineNumberInfo,
                       boolean saveLocalVariableInfo) {

        String target = info.getClassFile().getTarget();
        if ("1.0".equals(target)) {
            mTarget = 0x00010000;
        } else if ("1.1".equals(target)) {
            mTarget = 0x00010001;
        } else if ("1.2".equals(target)) {
            mTarget = 0x00010002;
        } else if ("1.3".equals(target)) {
            mTarget = 0x00010003;
        } else if ("1.4".equals(target)) {
            mTarget = 0x00010004;
        } else if ("1.5".equals(target)) {
            mTarget = 0x00010005;
        } else if ("1.6".equals(target)) {
            mTarget = 0x00010006;
        } else {
            mTarget = 0x00010000;
        }

        mCodeAttr = info.getCodeAttr();
        mClassFile = info.getClassFile();
        mCp = mClassFile.getConstantPool();
        mInstructions = new InstructionList(saveLocalVariableInfo);

        mCodeAttr.setCodeBuffer(this);

        mSaveLineNumberInfo = saveLineNumberInfo;
        mSaveLocalVariableInfo = saveLocalVariableInfo;

        // Create LocalVariable references for "this" reference and other
        // passed in parameters.

        LocalVariable localVar;

        if (info.getModifiers().isStatic()) {
            mThisReference = null;
        } else {
            localVar = mInstructions.createLocalParameter("this", mClassFile.getType());
            mThisReference = localVar;

            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }

        TypeDesc[] paramTypes = info.getMethodDescriptor().getParameterTypes();
        int paramSize = paramTypes.length;

        mParameters = new LocalVariable[paramSize];

        for (int i = 0; i<paramTypes.length; i++) {
            localVar = mInstructions.createLocalParameter(null, paramTypes[i]);
            mParameters[i] = localVar;
            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }
    }

    public int getMaxStackDepth() {
        return mInstructions.getMaxStackDepth();
    }

    public int getMaxLocals() {
        return mInstructions.getMaxLocals();
    }

    public byte[] getByteCodes() {
        return mInstructions.getByteCodes();
    }

    public ExceptionHandler[] getExceptionHandlers() {
        return mInstructions.getExceptionHandlers();
    }

    /**
     * @param pushed type of argument pushed to operand stack after instruction
     * executes; pass TypeDesc.VOID if nothing
     */
    private void addInstruction(int stackAdjust, TypeDesc pushed, byte opcode) {
        mInstructions.new SimpleInstruction(stackAdjust, pushed, new byte[] {opcode});
    }

    /**
     * @param pushed type of argument pushed to operand stack after instruction
     * executes; pass TypeDesc.VOID if nothing
     */
    private void addInstruction(int stackAdjust, TypeDesc pushed, byte opcode, byte operand) {
        mInstructions.new SimpleInstruction(stackAdjust, pushed, new byte[] {opcode, operand});
    }

    /**
     * @param pushed type of argument pushed to operand stack after instruction
     * executes; pass TypeDesc.VOID if nothing
     */
    private void addInstruction(int stackAdjust, TypeDesc pushed, byte opcode, short operand) {
        mInstructions.new SimpleInstruction
            (stackAdjust, pushed,
             new byte[] {opcode, (byte)(operand >> 8), (byte)operand});
    }

    /**
     * @param pushed type of argument pushed to operand stack after instruction
     * executes; pass TypeDesc.VOID if nothing
     */
    private void addInstruction(int stackAdjust, TypeDesc pushed, byte opcode,
                                ConstantInfo operand) {
        mInstructions.new ConstantOperandInstruction
            (stackAdjust, pushed,
             // The zeros get filled in later, when the ConstantInfo index is
             // resolved.
             new byte[] {opcode, (byte)0, (byte)0}, operand);
    }

    private String getClassName(TypeDesc classDesc) throws IllegalArgumentException {
        if (classDesc.isPrimitive()) {
            throw new IllegalArgumentException("Primitive type not allowed");
        }
        if (classDesc.isArray()) {
            throw new IllegalArgumentException("Array type not allowed");
        }
        return classDesc.getRootName();
    }

    public int getParameterCount() {
        return mParameters.length;
    }

    public LocalVariable getParameter(int index) {
        return mParameters[index];
    }

    public LocalVariable createLocalVariable(String name, TypeDesc type) {
        LocalVariable localVar = mInstructions.createLocalVariable(name, type);

        if (mSaveLocalVariableInfo) {
            mCodeAttr.localVariableUse(localVar);
        }

        return localVar;
    }

    public Label createLabel() {
        return mInstructions.new LabelInstruction();
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName)
    {
        if (!(startLocation instanceof InstructionList.LabelInstruction)) {
            throw new IllegalArgumentException("Start location is not a label instruction");
        }
        if (!(endLocation instanceof InstructionList.LabelInstruction)) {
            throw new IllegalArgumentException("End location is not a label instruction");
        }

        Location catchLocation = createLabel().setLocation();

        ConstantClassInfo catchClass;
        if (catchClassName == null) {
            catchClass = null;
        } else {
            catchClass = mCp.addConstantClass(catchClassName);
        }

        ExceptionHandler<InstructionList.LabelInstruction> handler = 
            new ExceptionHandler<InstructionList.LabelInstruction>
            ((InstructionList.LabelInstruction)startLocation,
             (InstructionList.LabelInstruction)endLocation,
             (InstructionList.LabelInstruction)catchLocation,
             catchClass);

        mInstructions.addExceptionHandler(handler);
    }
    
    public void mapLineNumber(int lineNumber) {
        if (mSaveLineNumberInfo) {
            mCodeAttr.mapLineNumber(createLabel().setLocation(), lineNumber);
        }
    }

    // load-constant-to-stack style instructions

    public void loadNull() {
        addInstruction(1, null, Opcode.ACONST_NULL);
    }

    public void loadConstant(String value) {
        if (value == null) {
            loadNull();
            return;
        }

        int strlen = value.length();

        if (strlen <= (65535 / 3)) {
            // Guaranteed to fit in a Java UTF encoded string.
            ConstantInfo info = mCp.addConstantString(value);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.STRING, info);
            return;
        }

        // Compute actual UTF length.

        int utflen = 0;

        for (int i=0; i<strlen; i++) {
            int c = value.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen <= 65535) {
            ConstantInfo info = mCp.addConstantString(value);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.STRING, info);
            return;
        }

        // Break string up into chunks and construct in a StringBuffer.

        TypeDesc stringBufferDesc;
        if (mTarget >= 0x00010005) {
            stringBufferDesc = TypeDesc.forClass("java.lang.StringBuilder");
        } else {
            stringBufferDesc = TypeDesc.forClass(StringBuffer.class);
        }
        
        TypeDesc intDesc = TypeDesc.INT;
        TypeDesc stringDesc = TypeDesc.STRING;
        TypeDesc[] stringParam = new TypeDesc[] {stringDesc};
        
        newObject(stringBufferDesc);
        dup();
        loadConstant(strlen);
        invokeConstructor(stringBufferDesc, new TypeDesc[] {intDesc});
        
        int beginIndex;
        int endIndex = 0;
        
        while (endIndex < strlen) {
            beginIndex = endIndex;

            // Make each chunk as large as possible.
            utflen = 0;
            for (; endIndex < strlen; endIndex++) {
                int c = value.charAt(endIndex);
                int size;
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    size = 1;
                } else if (c > 0x07FF) {
                    size = 3;
                } else {
                    size = 2;
                }

                if ((utflen + size) > 65535) {
                    break;
                } else {
                    utflen += size;
                }
            }

            String substr = value.substring(beginIndex, endIndex);

            ConstantInfo info = mCp.addConstantString(substr);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.STRING, info);

            invokeVirtual(stringBufferDesc, "append",
                          stringBufferDesc, stringParam);
        }
        
        invokeVirtual(stringBufferDesc, "toString", stringDesc, null);
    }

    public void loadConstant(TypeDesc type) throws IllegalStateException {
        if (type == null) {
            loadNull();
            return;
        }

        if (type.isPrimitive()) {
            if (mTarget < 0x00010001) {
                throw new IllegalStateException
                    ("Loading constant primitive classes not supported below target version 1.1");
            }
            loadStaticField(type.toObjectType(), "TYPE", TypeDesc.forClass(Class.class));
        } else {
            if (mTarget < 0x00010005) {
                throw new IllegalStateException
                    ("Loading constant object classes not supported below target version 1.5");
            }
            ConstantInfo info = mCp.addConstantClass(type);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.forClass(Class.class), info);
        }
    }

    public void loadConstant(boolean value) {
        loadConstant(value ? 1 : 0);
    }

    public void loadConstant(int value) {
        if (-1 <= value && value <= 5) {
            byte op;

            switch(value) {
            case -1:
                op = Opcode.ICONST_M1;
                break;
            case 0:
                op = Opcode.ICONST_0;
                break;
            case 1:
                op = Opcode.ICONST_1;
                break;
            case 2:
                op = Opcode.ICONST_2;
                break;
            case 3:
                op = Opcode.ICONST_3;
                break;
            case 4:
                op = Opcode.ICONST_4;
                break;
            case 5:
                op = Opcode.ICONST_5;
                break;
            default:
                op = Opcode.NOP;
            }

            addInstruction(1, TypeDesc.INT, op);
        } else if (-128 <= value && value <= 127) {
            addInstruction(1, TypeDesc.INT, Opcode.BIPUSH, (byte)value);
        } else if (-32768 <= value && value <= 32767) {
            addInstruction(1, TypeDesc.INT, Opcode.SIPUSH, (short)value);
        } else {
            ConstantInfo info = mCp.addConstantInteger(value);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.INT, info);
        }
    }

    public void loadConstant(long value) {
        if (value == 0) {
            addInstruction(2, TypeDesc.LONG, Opcode.LCONST_0);
        } else if (value == 1) {
            addInstruction(2, TypeDesc.LONG, Opcode.LCONST_1);
        } else {
            ConstantInfo info = mCp.addConstantLong(value);
            mInstructions.new LoadConstantInstruction(2, TypeDesc.LONG, info, true);
        }
    }

    public void loadConstant(float value) {
        if (value == 0) {
            addInstruction(1, TypeDesc.FLOAT, Opcode.FCONST_0);
        } else if (value == 1) {
            addInstruction(1, TypeDesc.FLOAT, Opcode.FCONST_1);
        } else if (value == 2) {
            addInstruction(1, TypeDesc.FLOAT, Opcode.FCONST_2);
        } else {
            ConstantInfo info = mCp.addConstantFloat(value);
            mInstructions.new LoadConstantInstruction(1, TypeDesc.FLOAT, info);
        }
    }

    public void loadConstant(double value) {
        if (value == 0) {
            addInstruction(2, TypeDesc.DOUBLE, Opcode.DCONST_0);
        } else if (value == 1) {
            addInstruction(2, TypeDesc.DOUBLE, Opcode.DCONST_1);
        } else {
            ConstantInfo info = mCp.addConstantDouble(value);
            mInstructions.new LoadConstantInstruction(2, TypeDesc.DOUBLE, info, true);
        }
    }

    // load-local-to-stack style instructions

    public void loadLocal(LocalVariable local) {
        if (local == null) {
            throw new IllegalArgumentException("No local variable specified");
        }
        int stackAdjust = local.getType().isDoubleWord() ? 2 : 1;
        mInstructions.new LoadLocalInstruction(stackAdjust, local);
    }

    public void loadThis() {
        if (mThisReference != null) {
            loadLocal(mThisReference);
        } else {
            throw new IllegalStateException
                ("Attempt to load \"this\" reference in a static method");
        }
    }

    // store-from-stack-to-local style instructions

    public void storeLocal(LocalVariable local) {
        if (local == null) {
            throw new IllegalArgumentException("No local variable specified");
        }
        int stackAdjust = local.getType().isDoubleWord() ? -2 : -1;
        mInstructions.new StoreLocalInstruction(stackAdjust, local);
    }

    // load-to-stack-from-array style instructions

    public void loadFromArray(TypeDesc type) {
        byte op;
        int stackAdjust = -1;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
            op = Opcode.IALOAD;
            break;
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
            op = Opcode.BALOAD;
            break;
        case TypeDesc.SHORT_CODE:
            op = Opcode.SALOAD;
            break;
        case TypeDesc.CHAR_CODE:
            op = Opcode.CALOAD;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FALOAD;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = 0;
            op = Opcode.LALOAD;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = 0;
            op = Opcode.DALOAD;
            break;
        default:
            op = Opcode.AALOAD;
            break;
        }

        addInstruction(stackAdjust, type, op);
    }

    // store-to-array-from-stack style instructions

    public void storeToArray(TypeDesc type) {
        byte op;
        int stackAdjust = -3;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
            op = Opcode.IASTORE;
            break;
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
            op = Opcode.BASTORE;
            break;
        case TypeDesc.SHORT_CODE:
            op = Opcode.SASTORE;
            break;
        case TypeDesc.CHAR_CODE:
            op = Opcode.CASTORE;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FASTORE;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = -4;
            op = Opcode.LASTORE;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = -4;
            op = Opcode.DASTORE;
            break;
        default:
            op = Opcode.AASTORE;
            break;
        }

        addInstruction(stackAdjust, TypeDesc.VOID, op);
    }

    // load-field-to-stack style instructions

    public void loadField(String fieldName,
                          TypeDesc type) {
        getfield(0, Opcode.GETFIELD, constantField(fieldName, type), type);
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDesc type) {

        getfield(0, Opcode.GETFIELD,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    public void loadField(TypeDesc classDesc,
                          String fieldName,
                          TypeDesc type) {

        loadField(getClassName(classDesc), fieldName, type);
    }

    public void loadStaticField(String fieldName,
                                TypeDesc type) {

        getfield(1, Opcode.GETSTATIC, constantField(fieldName, type), type);
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDesc type) {

        getfield(1, Opcode.GETSTATIC,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    public void loadStaticField(TypeDesc classDesc,
                                String fieldName,
                                TypeDesc type) {

        loadStaticField(getClassName(classDesc), fieldName, type);
    }

    private void getfield(int stackAdjust, byte opcode, ConstantInfo info, TypeDesc type) {
        if (type.isDoubleWord()) {
            stackAdjust++;
        }
        addInstruction(stackAdjust, type, opcode, info);
    }

    private ConstantFieldInfo constantField(String fieldName, TypeDesc type) {
        return mCp.addConstantField(mClassFile.getClassName(), fieldName, type);
    }

    // store-to-field-from-stack style instructions

    public void storeField(String fieldName, TypeDesc type) {
        putfield(-1, Opcode.PUTFIELD, constantField(fieldName, type), type);
    }

    public void storeField(String className, String fieldName, TypeDesc type) {
        putfield(-1, Opcode.PUTFIELD, 
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    public void storeField(TypeDesc classDesc, String fieldName, TypeDesc type) {
        storeField(getClassName(classDesc), fieldName, type);
    }

    public void storeStaticField(String fieldName, TypeDesc type) {
        putfield(0, Opcode.PUTSTATIC, constantField(fieldName, type), type);
    }

    public void storeStaticField(String className, String fieldName, TypeDesc type) {
        putfield(0, Opcode.PUTSTATIC,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    public void storeStaticField(TypeDesc classDesc, String fieldName, TypeDesc type) {
        storeStaticField(getClassName(classDesc), fieldName, type);
    }

    private void putfield(int stackAdjust, byte opcode, ConstantInfo info, TypeDesc type) {
        if (type.isDoubleWord()) {
            stackAdjust -= 2;
        } else {
            stackAdjust--;
        }
        addInstruction(stackAdjust, TypeDesc.VOID, opcode, info);
    }

    // return style instructions

    public void returnVoid() {
        addInstruction(0, TypeDesc.VOID, Opcode.RETURN);
    }

    public void returnValue(TypeDesc type) {
        int stackAdjust = -1;
        byte op;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.CHAR_CODE:
            op = Opcode.IRETURN;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FRETURN;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = -2;
            op = Opcode.LRETURN;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = -2;
            op = Opcode.DRETURN;
            break;
        case TypeDesc.VOID_CODE:
            stackAdjust = 0;
            op = Opcode.RETURN;
            break;
        default:
            op = Opcode.ARETURN;
            break;
        }

        addInstruction(stackAdjust, TypeDesc.VOID, op);
    }

    // numerical conversion style instructions

    public void convert(TypeDesc fromType, TypeDesc toType) {
        convert(fromType, toType, CONVERT_FP_NORMAL);
    }

    public void convert(TypeDesc fromType, TypeDesc toType, int fpConvertMode) {
        if (fpConvertMode < 0 || fpConvertMode > CONVERT_FP_RAW_BITS) {
            throw new IllegalArgumentException("Illegal floating point conversion mode");
        }

        if (toType == TypeDesc.OBJECT) {
            if (fromType.isPrimitive()) {
                toType = fromType.toObjectType();
            } else {
                return;
            }
        }

        if (fromType == toType) {
            return;
        }

        TypeDesc fromPrimitiveType = fromType.toPrimitiveType();
        if (fromPrimitiveType == null) {
            if (!toType.isPrimitive()) {
                Class fromClass = fromType.toClass();
                if (fromClass != null) {
                    Class toClass = toType.toClass();
                    if (toClass != null && toClass.isAssignableFrom(fromClass)) {
                        return;
                    }
                }
            }
            throw invalidConversion(fromType, toType);
        }
        int fromTypeCode = fromPrimitiveType.getTypeCode();

        if (toType.toClass() == Number.class) {
            switch (fromTypeCode) {
            case TypeDesc.INT_CODE:
            case TypeDesc.BYTE_CODE:
            case TypeDesc.SHORT_CODE:
            case TypeDesc.LONG_CODE:
            case TypeDesc.FLOAT_CODE:
            case TypeDesc.DOUBLE_CODE:
                if (fromType.isPrimitive()) {
                    toType = fromType.toObjectType();
                } else {
                    return;
                }
            }
        }

        TypeDesc toPrimitiveType = toType.toPrimitiveType();
        if (toPrimitiveType == null) {
            throw invalidConversion(fromType, toType);
        }
        int toTypeCode = toPrimitiveType.getTypeCode();

        // Location is set at end, after doConversion.
        Label end = createLabel();

        doConversion: {
            int stackAdjust = 0;
            byte op;
            
            switch (fromTypeCode) {
            case TypeDesc.INT_CODE:
            case TypeDesc.BYTE_CODE:
            case TypeDesc.SHORT_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.BOOLEAN_CODE:
                switch (toTypeCode) {
                case TypeDesc.BYTE_CODE:
                    op = (fromTypeCode == TypeDesc.BYTE_CODE) ?
                        Opcode.NOP : Opcode.I2B;
                    break;
                case TypeDesc.SHORT_CODE:
                    op = (fromTypeCode == TypeDesc.SHORT_CODE) ?
                        Opcode.NOP : Opcode.I2S;
                    break;
                case TypeDesc.CHAR_CODE:
                    op = (fromTypeCode == TypeDesc.CHAR_CODE) ?
                        Opcode.NOP : Opcode.I2C;
                    break;
                case TypeDesc.FLOAT_CODE:
                    op = Opcode.I2F;
                    break;
                case TypeDesc.LONG_CODE:
                    stackAdjust = 1;
                    op = Opcode.I2L;
                    break;
                case TypeDesc.DOUBLE_CODE:
                    stackAdjust = 1;
                    op = Opcode.I2D;
                    break;
                case TypeDesc.INT_CODE:
                    op = Opcode.NOP;
                    break;
                case TypeDesc.BOOLEAN_CODE:
                    if (!fromType.isPrimitive()) {
                        if (!toType.isPrimitive()) {
                            nullConvert(end);
                        }
                        unbox(fromType, fromPrimitiveType);
                    }
                    toBoolean(!toType.isPrimitive());
                    break doConversion;
                default:
                    throw invalidConversion(fromType, toType);
                }
                break;
                
            case TypeDesc.LONG_CODE:
                switch (toTypeCode) {
                case TypeDesc.INT_CODE:
                    stackAdjust = -1;
                    op = Opcode.L2I;
                    break;
                case TypeDesc.FLOAT_CODE:
                    stackAdjust = -1;
                    op = Opcode.L2F;
                    break;
                case TypeDesc.DOUBLE_CODE:
                    op = Opcode.L2D;
                    break;
                case TypeDesc.BYTE_CODE:
                case TypeDesc.CHAR_CODE:
                case TypeDesc.SHORT_CODE:
                    addInstruction(-1, TypeDesc.INT, Opcode.L2I);
                    convert(TypeDesc.INT, toPrimitiveType);
                    // fall through
                case TypeDesc.LONG_CODE:
                    op = Opcode.NOP;
                    break;
                case TypeDesc.BOOLEAN_CODE:
                    if (!fromType.isPrimitive()) {
                        if (!toType.isPrimitive()) {
                            nullConvert(end);
                        }
                        unbox(fromType, fromPrimitiveType);
                    }
                    loadConstant(0L);
                    math(Opcode.LCMP);
                    toBoolean(!toType.isPrimitive());
                    break doConversion;
                default:
                    throw invalidConversion(fromType, toType);
                }
                break;
                
            case TypeDesc.FLOAT_CODE:
                switch (toTypeCode) {
                case TypeDesc.INT_CODE:
                    op = Opcode.F2I;
                    break;
                case TypeDesc.LONG_CODE:
                    stackAdjust = 1;
                    op = Opcode.F2L;
                    break;
                case TypeDesc.DOUBLE_CODE:
                    stackAdjust = 1;
                    op = Opcode.F2D;
                    break;
                case TypeDesc.BYTE_CODE:
                case TypeDesc.CHAR_CODE:
                case TypeDesc.SHORT_CODE:
                    addInstruction(0, TypeDesc.INT, Opcode.F2I);
                    convert(TypeDesc.INT, toPrimitiveType);
                    // fall through
                case TypeDesc.FLOAT_CODE:
                    op = Opcode.NOP;
                    break;
                case TypeDesc.BOOLEAN_CODE:
                    if (!fromType.isPrimitive()) {
                        if (!toType.isPrimitive()) {
                            nullConvert(end);
                        }
                        unbox(fromType, fromPrimitiveType);
                    }
                    loadConstant(0.0f);
                    math(Opcode.FCMPG);
                    toBoolean(!toType.isPrimitive());
                    break doConversion;
                default:
                    throw invalidConversion(fromType, toType);
                }
                break;
                
            case TypeDesc.DOUBLE_CODE:
                switch (toTypeCode) {
                case TypeDesc.INT_CODE:
                    stackAdjust = -1;
                    op = Opcode.D2I;
                    break;
                case TypeDesc.FLOAT_CODE:
                    stackAdjust = -1;
                    op = Opcode.D2F;
                    break;
                case TypeDesc.LONG_CODE:
                    op = Opcode.D2L;
                    break;
                case TypeDesc.BYTE_CODE:
                case TypeDesc.CHAR_CODE:
                case TypeDesc.SHORT_CODE:
                    addInstruction(-1, TypeDesc.INT, Opcode.D2I);
                    convert(TypeDesc.INT, toPrimitiveType);
                    // fall through
                case TypeDesc.DOUBLE_CODE:
                    op = Opcode.NOP;
                    break;
                case TypeDesc.BOOLEAN_CODE:
                    if (!fromType.isPrimitive()) {
                        if (!toType.isPrimitive()) {
                            nullConvert(end);
                        }
                        unbox(fromType, fromPrimitiveType);
                    }
                    loadConstant(0.0d);
                    math(Opcode.DCMPG);
                    toBoolean(!toType.isPrimitive());
                    break doConversion;
                default:
                    throw invalidConversion(fromType, toType);
                }
                break;
                
            default:
                throw invalidConversion(fromType, toType);
            }
            
            if (!fromType.isPrimitive()) {
                if (!toType.isPrimitive()) {
                    nullConvert(end);
                }
                unbox(fromType, fromPrimitiveType);
            }

            if (toType.isPrimitive()) {
                if (op != Opcode.NOP) {
                    convertPrimitive(stackAdjust, op, fpConvertMode);
                }
            } else {
                if (op == Opcode.NOP) {
                    prebox(toPrimitiveType, toType);
                } else if (!fromPrimitiveType.isDoubleWord() &&
                           toPrimitiveType.isDoubleWord()) {
                    // Slight optimization here. Perform prebox on single word value,
                    // depending on what conversion is being applied.
                    prebox(fromPrimitiveType, toType);
                    convertPrimitive(stackAdjust, op, fpConvertMode);
                } else {
                    convertPrimitive(stackAdjust, op, fpConvertMode);
                    prebox(toPrimitiveType, toType);
                }
                box(toPrimitiveType, toType);
            }
        }

        end.setLocation();
    }

    /**
     * @param from must be an object type
     * @param to must be a primitive type
     */
    private void unbox(TypeDesc from, TypeDesc to) {
        String methodName;

        switch (to.getTypeCode()) {
        case TypeDesc.BOOLEAN_CODE:
            methodName = "booleanValue";
            break;
        case TypeDesc.CHAR_CODE:
            methodName = "charValue";
            break;
        case TypeDesc.FLOAT_CODE:
            methodName = "floatValue";
            break;
        case TypeDesc.DOUBLE_CODE:
            methodName = "doubleValue";
            break;
        case TypeDesc.BYTE_CODE:
            methodName = "byteValue";
            break;
        case TypeDesc.SHORT_CODE:
            methodName = "shortValue";
            break;
        case TypeDesc.INT_CODE:
            methodName = "intValue";
            break;
        case TypeDesc.LONG_CODE:
            methodName = "longValue";
            break;
        default:
            return;
        }

        invokeVirtual(from.getRootName(), methodName, to, null);
    }

    /**
     * @param from must be a primitive type
     * @param to must be an object type
     */
    private void prebox(TypeDesc from, TypeDesc to) {
        if (mTarget >= 0x00010005) {
            // No need to prebox since static valueOf method can be called
            // instead.
            return;
        }

        // Wouldn't it be cool if I could walk backwards in the instruction
        // list and insert the new-dup pair before the value to box was even
        // put on the stack?

        switch (from.getTypeCode()) {
        default:
            break;
        case TypeDesc.BOOLEAN_CODE:
            if (to.toPrimitiveType().getTypeCode() == TypeDesc.BOOLEAN_CODE) {
                break;
            }
            // fall through
        case TypeDesc.CHAR_CODE:
        case TypeDesc.FLOAT_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.INT_CODE:
            newObject(to);
            dupX1();
            swap();
            break;
        case TypeDesc.DOUBLE_CODE:
        case TypeDesc.LONG_CODE:
            newObject(to);
            dupX2();
            dupX2();
            pop();
            break;
        }
    }

    /**
     * @param from must be a primitive type
     * @param to must be an object type
     */
    private void box(TypeDesc from, TypeDesc to) {
        if (mTarget >= 0x00010005) {
            // Call the new valueOf method.
            invokeStatic(to.getRootName(), "valueOf", to, new TypeDesc[] {from});
            return;
        }

        switch (from.getTypeCode()) {
        case TypeDesc.BOOLEAN_CODE:
            toBoolean(true);
            break;
        case TypeDesc.CHAR_CODE:
        case TypeDesc.FLOAT_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.INT_CODE:
        case TypeDesc.DOUBLE_CODE:
        case TypeDesc.LONG_CODE:
            invokeConstructor(to.getRootName(), new TypeDesc[]{from});
            break;
        }
    }

    // Converts an int on the stack to a boolean.
    private void toBoolean(boolean box) {
        if (box && mTarget >= 0x00010004) {
            // Call the new valueOf method.
            invokeStatic("java.lang.Boolean", "valueOf",
                         TypeDesc.BOOLEAN.toObjectType(),
                         new TypeDesc[] {TypeDesc.BOOLEAN});
            return;
        }

        Label nonZero = createLabel();
        Label done = createLabel();
        ifZeroComparisonBranch(nonZero, "!=");
        if (box) {
            TypeDesc newType = TypeDesc.BOOLEAN.toObjectType();
            loadStaticField(newType.getRootName(), "FALSE", newType);
            branch(done);
            nonZero.setLocation();
            loadStaticField(newType.getRootName(), "TRUE", newType);
        } else {
            loadConstant(false);
            branch(done);
            nonZero.setLocation();
            loadConstant(true);
        }
        done.setLocation();
    }

    private void convertPrimitive(int stackAdjust, byte op, int fpConvertMode) {
        if (fpConvertMode != CONVERT_FP_NORMAL) {
            switch (op) {
            case Opcode.I2F:
                invokeStatic("java.lang.Float", "intBitsToFloat", TypeDesc.FLOAT,
                             new TypeDesc[] {TypeDesc.INT});
                return;

            case Opcode.L2D:
                invokeStatic("java.lang.Double", "longBitsToDouble", TypeDesc.DOUBLE,
                             new TypeDesc[] {TypeDesc.LONG});
                return;

            case Opcode.F2I:
                if (fpConvertMode == CONVERT_FP_RAW_BITS) {
                    invokeStatic("java.lang.Float", "floatToRawIntBits", TypeDesc.INT,
                                 new TypeDesc[] {TypeDesc.FLOAT});
                } else {
                    invokeStatic("java.lang.Float", "floatToIntBits", TypeDesc.INT,
                                 new TypeDesc[] {TypeDesc.FLOAT});
                }
                return;

            case Opcode.D2L:
                if (fpConvertMode == CONVERT_FP_RAW_BITS) {
                    invokeStatic("java.lang.Double", "doubleToRawLongBits", TypeDesc.LONG,
                                 new TypeDesc[] {TypeDesc.DOUBLE});
                } else {
                    invokeStatic("java.lang.Double", "doubleToLongBits", TypeDesc.LONG,
                                 new TypeDesc[] {TypeDesc.DOUBLE});
                }
                return;
            }
        }

        TypeDesc pushed;
        switch (op) {
        case Opcode.I2F:
            pushed = TypeDesc.FLOAT;
            break;
        case Opcode.L2D:
            pushed = TypeDesc.DOUBLE;
            break;
        case Opcode.F2I:
            pushed = TypeDesc.INT;
            break;
        case Opcode.D2L:
            pushed = TypeDesc.LONG;
            break;
        default:
            pushed = TypeDesc.VOID;
            break;
        }

        addInstruction(stackAdjust, pushed, op);
    }    

    // Check if from object is null. If so, no need to convert, and don't throw
    // a NullPointerException. Assumes that from type and to type are objects.
    private void nullConvert(Label end) {
        LocalVariable temp = createLocalVariable("temp", TypeDesc.OBJECT);
        storeLocal(temp);
        loadLocal(temp);
        Label notNull = createLabel();
        ifNullBranch(notNull, false);
        loadNull();
        branch(end);
        notNull.setLocation();
        loadLocal(temp);
    }

    private IllegalArgumentException invalidConversion
        (TypeDesc from, TypeDesc to)
    {
        throw new IllegalArgumentException
            ("Invalid conversion: " + from.getFullName() + " to " +
             to.getFullName());
    }

    // invocation style instructions

    public void invokeVirtual(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        invokeVirtual(mClassFile.getClassName(), methodName, ret, params);
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstructions.new InvokeInstruction
            (Opcode.INVOKEVIRTUAL,
             mCp.addConstantMethod(className, methodName, ret, params),
             ret, params);
    }

    public void invokeVirtual(TypeDesc classDesc,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        invokeVirtual(getClassName(classDesc), methodName, ret, params);
    }

    public void invokeStatic(String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        invokeStatic(mClassFile.getClassName(), methodName, ret, params);
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mInstructions.new InvokeInstruction
            (Opcode.INVOKESTATIC,
             mCp.addConstantMethod(className, methodName, ret, params),
             ret, params);
    }

    public void invokeStatic(TypeDesc classDesc,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        invokeStatic(getClassName(classDesc), methodName, ret, params);
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        mInstructions.new InvokeInstruction
            (Opcode.INVOKEINTERFACE,
             mCp.addConstantInterfaceMethod(className, methodName, ret, params),
             ret, params);
    }

    public void invokeInterface(TypeDesc classDesc,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        invokeInterface(getClassName(classDesc), methodName, ret, params);
    }

    public void invokePrivate(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstructions.new InvokeInstruction
            (Opcode.INVOKESPECIAL,
             mCp.addConstantMethod(mClassFile.getClassName(), methodName, ret, params),
             ret, params);
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        mInstructions.new InvokeInstruction
            (Opcode.INVOKESPECIAL,
             mCp.addConstantMethod(superClassName, methodName, ret, params),
             ret, params);
    }

    public void invokeSuper(TypeDesc superClassDesc,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        invokeSuper(getClassName(superClassDesc), methodName, ret, params);
    }

    public void invokeConstructor(TypeDesc[] params) {
        invokeConstructor(mClassFile.getClassName(), mClassFile.getType(), params);
    }

    public void invokeConstructor(String className, TypeDesc[] params) {
        invokeConstructor(className, TypeDesc.forClass(className), params);
    }

    public void invokeConstructor(TypeDesc classDesc, TypeDesc[] params) {
        invokeConstructor(getClassName(classDesc), classDesc, params);
    }

    private void invokeConstructor(String className, TypeDesc classDesc, TypeDesc[] params) {
        mInstructions.new InvokeConstructorInstruction
            (mCp.addConstantConstructor(className, params), classDesc, params);
    }

    public void invokeSuperConstructor(TypeDesc[] params) {
        invokeConstructor(mClassFile.getSuperClassName(), params);
    }

    // creation style instructions

    public void newObject(TypeDesc type) {
        if (type.isArray()) {
            newObject(type, 1);
        } else {
            mInstructions.new NewObjectInstruction(mCp.addConstantClass(type));
        }
    }

    public void newObject(TypeDesc type, int dimensions) {
        if (dimensions <= 0) {
            // If type refers to an array, then this code is bogus.
            mInstructions.new NewObjectInstruction(mCp.addConstantClass(type));
            return;
        }

        TypeDesc componentType = type.getComponentType();

        if (dimensions == 1) {
            if (componentType.isPrimitive()) {
                addInstruction(0, type, Opcode.NEWARRAY, (byte)componentType.getTypeCode());
                return;
            }
            addInstruction(0, type, Opcode.ANEWARRAY, mCp.addConstantClass(componentType));
            return;
        }

        int stackAdjust = -(dimensions - 1);
        ConstantInfo info = mCp.addConstantClass(type);
        byte[] bytes = new byte[4];

        bytes[0] = Opcode.MULTIANEWARRAY;
        //bytes[1] = (byte)0;
        //bytes[2] = (byte)0;
        bytes[3] = (byte)dimensions;
        
        mInstructions.new ConstantOperandInstruction(stackAdjust, type, bytes, info);
    }

    // stack operation style instructions

    public void dup() {
        mInstructions.new StackOperationInstruction(Opcode.DUP);
    }

    public void dupX1() {
        mInstructions.new StackOperationInstruction(Opcode.DUP_X1);
    }

    public void dupX2() {
        mInstructions.new StackOperationInstruction(Opcode.DUP_X2);
    }

    public void dup2() {
        mInstructions.new StackOperationInstruction(Opcode.DUP2);
    }

    public void dup2X1() {
        mInstructions.new StackOperationInstruction(Opcode.DUP2_X1);
    }

    public void dup2X2() {
        mInstructions.new StackOperationInstruction(Opcode.DUP2_X2);
    }

    public void pop() {
        mInstructions.new StackOperationInstruction(Opcode.POP);
    }

    public void pop2() {
        mInstructions.new StackOperationInstruction(Opcode.POP2);
    }

    public void swap() {
        mInstructions.new StackOperationInstruction(Opcode.SWAP);
    }

    public void swap2() {
        dup2X2();
        pop2();
    }

    // flow control instructions

    private void branch(int stackAdjust, Location location, byte opcode) {
        if (!(location instanceof InstructionList.LabelInstruction)) {
            throw new IllegalArgumentException("Branch location is not a label instruction");
        }
        mInstructions.new BranchInstruction(stackAdjust, opcode,
                                            (InstructionList.LabelInstruction)location);
    }

    public void branch(Location location) {
        branch(0, location, Opcode.GOTO);
    }

    public void ifNullBranch(Location location, boolean choice) {
        branch(-1, location, choice ? Opcode.IFNULL : Opcode.IFNONNULL);
    }

    public void ifEqualBranch(Location location, boolean choice) {
        branch(-2, location, choice ? Opcode.IF_ACMPEQ : Opcode.IF_ACMPNE);
    }

    public void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IFEQ;
        } else if (choice == "!=") {
            opcode = Opcode.IFNE;
        } else if (choice == "<") {
            opcode = Opcode.IFLT;
        } else if (choice == ">=") {
            opcode = Opcode.IFGE;
        } else if (choice == ">") {
            opcode = Opcode.IFGT;
        } else if (choice == "<=") {
            opcode = Opcode.IFLE;
        } else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }
        
        branch(-1, location, opcode);
    }

    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IF_ICMPEQ;
        } else if (choice == "!=") {
            opcode = Opcode.IF_ICMPNE;
        } else if (choice == "<") {
            opcode = Opcode.IF_ICMPLT;
        } else if (choice == ">=") {
            opcode = Opcode.IF_ICMPGE;
        } else if (choice == ">") {
            opcode = Opcode.IF_ICMPGT;
        } else if (choice == "<=") {
            opcode = Opcode.IF_ICMPLE;
        } else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }

        branch(-2, location, opcode);
    }

    public void switchBranch(int[] cases, Location[] locations, Location defaultLocation) {
        mInstructions.new SwitchInstruction(cases, locations, defaultLocation);
    }

    public void jsr(Location location) {
        // Adjust the stack by one to make room for the return address.
        branch(1, location, Opcode.JSR);
    }

    public void ret(LocalVariable local) {
        if (local == null) {
            throw new IllegalArgumentException("No local variable specified");
        }

        mInstructions.new RetInstruction(local);
    }

    // math instructions

    public void math(byte opcode) {
        int stackAdjust;
        
        switch(opcode) {
        case Opcode.INEG:
        case Opcode.LNEG:
        case Opcode.FNEG:
        case Opcode.DNEG:
            stackAdjust = 0;
            break;
        case Opcode.IADD:
        case Opcode.ISUB:
        case Opcode.IMUL:
        case Opcode.IDIV:
        case Opcode.IREM:
        case Opcode.IAND:
        case Opcode.IOR:
        case Opcode.IXOR:
        case Opcode.ISHL:
        case Opcode.ISHR:
        case Opcode.IUSHR:
        case Opcode.FADD:
        case Opcode.FSUB:
        case Opcode.FMUL:
        case Opcode.FDIV:
        case Opcode.FREM:
        case Opcode.FCMPG:
        case Opcode.FCMPL:
        case Opcode.LSHL:
        case Opcode.LSHR:
        case Opcode.LUSHR:
            stackAdjust = -1;
            break;
        case Opcode.LADD:
        case Opcode.LSUB:
        case Opcode.LMUL:
        case Opcode.LDIV:
        case Opcode.LREM:
        case Opcode.LAND:
        case Opcode.LOR:
        case Opcode.LXOR:
        case Opcode.DADD:
        case Opcode.DSUB:
        case Opcode.DMUL:
        case Opcode.DDIV:
        case Opcode.DREM:
            stackAdjust = -2;
            break;
        case Opcode.LCMP:
        case Opcode.DCMPG:
        case Opcode.DCMPL:
            stackAdjust = -3;
            break;
        default:
            throw new IllegalArgumentException
                ("Not a math opcode: " + Opcode.getMnemonic(opcode));
        }

        TypeDesc pushed;

        switch(opcode) {
        case Opcode.INEG:
        case Opcode.IADD:
        case Opcode.ISUB:
        case Opcode.IMUL:
        case Opcode.IDIV:
        case Opcode.IREM:
        case Opcode.IAND:
        case Opcode.IOR:
        case Opcode.IXOR:
        case Opcode.ISHL:
        case Opcode.ISHR:
        case Opcode.IUSHR:
        case Opcode.LCMP:
        case Opcode.FCMPG:
        case Opcode.FCMPL:
        case Opcode.DCMPG:
        case Opcode.DCMPL:
            pushed = TypeDesc.INT;
            break;
        case Opcode.LNEG:
        case Opcode.LADD:
        case Opcode.LSUB:
        case Opcode.LMUL:
        case Opcode.LDIV:
        case Opcode.LREM:
        case Opcode.LAND:
        case Opcode.LOR:
        case Opcode.LXOR:
        case Opcode.LSHL:
        case Opcode.LSHR:
        case Opcode.LUSHR:
            pushed = TypeDesc.LONG;
            break;
        case Opcode.FNEG:
        case Opcode.FADD:
        case Opcode.FSUB:
        case Opcode.FMUL:
        case Opcode.FDIV:
        case Opcode.FREM:
            pushed = TypeDesc.FLOAT;
            break;
        case Opcode.DNEG:
        case Opcode.DADD:
        case Opcode.DSUB:
        case Opcode.DMUL:
        case Opcode.DDIV:
        case Opcode.DREM:
            pushed = TypeDesc.DOUBLE;
            break;
        default:
            throw new IllegalArgumentException
                ("Not a math opcode: " + Opcode.getMnemonic(opcode));
        }

        addInstruction(stackAdjust, pushed, opcode);
    }

    // miscellaneous instructions

    public void arrayLength() {
        addInstruction(0, TypeDesc.INT, Opcode.ARRAYLENGTH);
    }

    public void throwObject() {
        addInstruction(-1, TypeDesc.VOID, Opcode.ATHROW);
    }

    public void checkCast(TypeDesc type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addInstruction(0, TypeDesc.VOID, Opcode.CHECKCAST, info);
    }

    public void instanceOf(TypeDesc type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addInstruction(0, TypeDesc.INT, Opcode.INSTANCEOF, info);
    }

    public void integerIncrement(LocalVariable local, int amount) {
        if (local == null) {
            throw new IllegalArgumentException("No local variable specified");
        }

        if (-32768 <= amount && amount <= 32767) {
            mInstructions.new ShortIncrementInstruction(local, (short)amount);
        } else {
            // Amount can't possibly fit in a 16-bit value, so use regular
            // instructions instead.

            loadLocal(local);
            loadConstant(amount);
            math(Opcode.IADD);
            storeLocal(local);
        }
    }

    public void monitorEnter() {
        addInstruction(-1, TypeDesc.VOID, Opcode.MONITORENTER);
    }

    public void monitorExit() {
        addInstruction(-1, TypeDesc.VOID, Opcode.MONITOREXIT);
    }

    public void nop() {
        addInstruction(0, TypeDesc.VOID, Opcode.NOP);
    }

    public void breakpoint() {
        addInstruction(0, TypeDesc.VOID, Opcode.BREAKPOINT);
    }
}
