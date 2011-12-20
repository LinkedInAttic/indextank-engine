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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.PrintWriter;

/**
 * CodeAssembler implementation that prints out instructions using a Java-like
 * syntax that matches the methods of CodeAssembler. When used in conjunction
 * with a {@link CodeDisassembler}, this class makes it easier to understand
 * how to use a CodeAssembler.
 *
 * @author Brian S O'Neill
 */
public class CodeAssemblerPrinter extends AbstractCodeAssembler implements CodeAssembler {
    private final LocalVariable[] mParams;
    private final boolean mIsStatic;
    private final PrintWriter mWriter;
    private final String mLinePrefix;
    private final String mLineSuffix;
    private final String mBulder;

    private boolean mNeedSeparatorLine;

    private int mLocalCounter;
    private int mLabelCounter;

    private int mTypeDescCounter;
    // Maps TypeDesc objects to String variable names.
    private Map<TypeDesc, String> mTypeDescNames;

    private int mTypeDescArrayCounter;
    // Maps TypeDesc arrays to String variable names.
    private Map<List<TypeDesc>, String> mTypeDescArrayNames;

    public CodeAssemblerPrinter(TypeDesc[] paramTypes, boolean isStatic,
                                PrintWriter writer)
    {
        this(paramTypes, isStatic, writer, null, null, null);
    }

    /**
     * @param linePrefix optional prefix for each line
     * @param lineSuffix optional suffix for each line
     * @param builder when specified, generated method calls are invoked on
     * local variable by this name
     */
    public CodeAssemblerPrinter(TypeDesc[] paramTypes, boolean isStatic,
                                PrintWriter writer,
                                String linePrefix, String lineSuffix,
                                String builder)
    {
        mIsStatic = isStatic;
        mWriter = writer;
        mLinePrefix = linePrefix;
        mLineSuffix = lineSuffix;
        if (builder == null || builder.length() == 0) {
            builder = "";
        } else if (!builder.endsWith(".")) {
            builder += '.';
        }
        mBulder = builder;
        mTypeDescNames = new HashMap<TypeDesc, String>();
        mTypeDescArrayNames = new HashMap<List<TypeDesc>, String>();

        mParams = new LocalVariable[paramTypes.length];

        int varNum = (isStatic) ? 0 : 1;
        for (int i = 0; i<paramTypes.length; i++) {
            String varName = "var_" + (++mLocalCounter);
            println("LocalVariable " + varName + " = " +
                    mBulder + "getParameter(" + i + ')');
            LocalVariable localVar = new NamedLocal(varName, paramTypes[i], varNum);
            varNum += (localVar.isDoubleWord() ? 2 : 1);
            mParams[i] = localVar;
        }
    }

    public int getParameterCount() {
        return mParams.length;
    }

    public LocalVariable getParameter(int index) {
        return mParams[index];
    }

    public LocalVariable createLocalVariable(String name, 
                                             TypeDesc type) {
        String varName = "var_" + (++mLocalCounter);
        if (name != null) {
            name = '"' + name + '"';
        }
        println("LocalVariable " + varName + " = " +
                mBulder + "createLocalVariable(" + name +
                ", " + getTypeDescName(type) + ')');
        return new NamedLocal(varName, type, -1);
    }

    public Label createLabel() {
        String name = "label_" + (++mLabelCounter);
        println("Label " + name + " = " + mBulder + "createLabel()");
        return new NamedLabel(name);
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        println(mBulder + "exceptionHandler(" +
                getLabelName(startLocation) + ", " +
                getLabelName(endLocation) + ", " +
                (catchClassName == null ? "null" : ('"' + catchClassName + '"')) +
                ')');
    }
    
    public void mapLineNumber(int lineNumber) {
        separatorLine();
        println(mBulder + "mapLineNumber(" + lineNumber + ')');
    }

    public void loadNull() {
        println(mBulder + "loadNull()");
    }

    public void loadConstant(String value) {
        if (value == null) {
            loadNull();
        } else {
            println(mBulder + "loadConstant(\"" + escape(value) + "\")");
        }
    }

    public void loadConstant(TypeDesc type) {
        if (type == null) {
            loadNull();
        } else {
            println(mBulder + "loadConstant(" + getTypeDescName(type) + ')');
        }
        
    }

    public void loadConstant(boolean value) {
        println(mBulder + "loadConstant(" + value + ')');
    }

    public void loadConstant(int value) {
        println(mBulder + "loadConstant(" + value + ')');
    }

    public void loadConstant(long value) {
        println(mBulder + "loadConstant(" + value + "L)");
    }

    public void loadConstant(float value) {
        String str;
        if (value != value) {
            str = "0.0f/0.0f";
        } else if (value == Float.NEGATIVE_INFINITY) {
            str = "-1.0f/0.0f";
        } else if (value == Float.POSITIVE_INFINITY) {
            str = "1.0f/0.0f";
        } else {
            str = String.valueOf(value) + 'f';
        }
        println(mBulder + "loadConstant(" + str + ")");
    }

    public void loadConstant(double value) {
        String str;
        if (value != value) {
            str = "0.0d/0.0d";
        } else if (value == Double.NEGATIVE_INFINITY) {
            str = "-1.0d/0.0d";
        } else if (value == Double.POSITIVE_INFINITY) {
            str = "1.0d/0.0d";
        } else {
            str = String.valueOf(value) + 'd';
        }
        println(mBulder + "loadConstant(" + str + ")");
    }

    public void loadLocal(LocalVariable local) {
        println(mBulder + "loadLocal(" + local.getName() + ')');
    }

    public void loadThis() {
        println(mBulder + "loadThis()");
    }

    public void storeLocal(LocalVariable local) {
        println(mBulder + "storeLocal(" + local.getName() + ')');
    }

    public void loadFromArray(TypeDesc type) {
        println(mBulder + "loadFromArray(" + getTypeDescName(type) + ')');
    }

    public void storeToArray(TypeDesc type) {
        println(mBulder + "storeToArray(" + getTypeDescName(type) + ')');
    }

    public void loadField(String fieldName,
                          TypeDesc type) {
        println(mBulder + "loadField(\"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDesc type) {
        println(mBulder + "loadField(\"" + className + "\", \"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void loadField(TypeDesc classDesc,
                          String fieldName,
                          TypeDesc type) {
        println(mBulder + "loadField(\"" + getTypeDescName(classDesc) + "\", \""
                + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void loadStaticField(String fieldName,
                                TypeDesc type) {
        println(mBulder + "loadStaticField(\"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDesc type) {
        println(mBulder + "loadStaticField(\"" + className + "\", \"" +
                fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void loadStaticField(TypeDesc classDesc,
                                String fieldName,
                                TypeDesc type) {
        println(mBulder + "loadStaticField(\"" + getTypeDescName(classDesc) + "\", \"" +
                fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeField(String fieldName,
                           TypeDesc type) {
        println(mBulder + "storeField(\"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDesc type) {
        println(mBulder + "storeField(\"" + className + "\", \"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeField(TypeDesc classDesc,
                           String fieldName,
                           TypeDesc type) {
        println(mBulder + "storeField(\"" + getTypeDescName(classDesc) + "\", \"" +
                fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeStaticField(String fieldName,
                                 TypeDesc type) {
        println(mBulder + "storeStaticField(\"" + fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDesc type) {
        println(mBulder + "storeStaticField(\"" + className + "\", \"" +
                fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void storeStaticField(TypeDesc classDesc,
                                 String fieldName,
                                 TypeDesc type) {
        println(mBulder + "storeStaticField(\"" + getTypeDescName(classDesc) + "\", \"" +
                fieldName + "\", " +
                getTypeDescName(type) + ')');
    }

    public void returnVoid() {
        println(mBulder + "returnVoid()");
    }

    public void returnValue(TypeDesc type) {
        println(mBulder + "returnValue(" + getTypeDescName(type) + ')');
    }

    public void convert(TypeDesc fromType, TypeDesc toType) {
        println(mBulder + "convert(" +
                getTypeDescName(fromType) + ", " +
                getTypeDescName(toType) + ')');
    }

    public void convert(TypeDesc fromType, TypeDesc toType, int fpConvertMode) {
        switch (fpConvertMode) {
        default:
            convert(fromType, toType);
            break;
        case CONVERT_FP_BITS:
            println(mBulder + "convert(" +
                    getTypeDescName(fromType) + ", " +
                    getTypeDescName(toType) + ", " +
                    mBulder + ".CONVERT_FP_BITS" + ')');
            break;
        case CONVERT_FP_RAW_BITS:
            println(mBulder + "convert(" +
                    getTypeDescName(fromType) + ", " +
                    getTypeDescName(toType) + ", " +
                    mBulder + ".CONVERT_FP_RAW_BITS" + ')');
            break;
        }
    }

    public void invokeVirtual(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        println(mBulder + "invokeVirtual(\"" + methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        println(mBulder + "invokeVirtual(\"" + className + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeVirtual(TypeDesc classDesc,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        println(mBulder + "invokeVirtual(\"" + getTypeDescName(classDesc) + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeStatic(String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        println(mBulder + "invokeStatic(\"" + methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        println(mBulder + "invokeStatic(\"" + className + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeStatic(TypeDesc classDesc,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        println(mBulder + "invokeStatic(\"" + getTypeDescName(classDesc) + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        println(mBulder + "invokeInterface(\"" + className + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeInterface(TypeDesc classDesc,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        println(mBulder + "invokeInterface(\"" + getTypeDescName(classDesc) + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokePrivate(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        println(mBulder + "invokePrivate(\"" + methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        println(mBulder + "invokeSuper(\"" + superClassName + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeSuper(TypeDesc superClassDesc,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        println(mBulder + "invokeSuper(\"" + getTypeDescName(superClassDesc) + "\", \"" +
                methodName + "\", " +
                getTypeDescName(ret) + ", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeConstructor(TypeDesc[] params) {
        println(mBulder + "invokeConstructor(" +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeConstructor(String className, TypeDesc[] params) {
        println(mBulder + "invokeConstructor(\"" + className + "\", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeConstructor(TypeDesc classDesc, TypeDesc[] params) {
        println(mBulder + "invokeConstructor(\"" + getTypeDescName(classDesc) + "\", " +
                getTypeDescArrayName(params) + ')');
    }

    public void invokeSuperConstructor(TypeDesc[] params) {
        println(mBulder + "invokeSuperConstructor(" +
                getTypeDescArrayName(params) + ')');
    }

    public void newObject(TypeDesc type) {
        println(mBulder + "newObject(" + getTypeDescName(type) + ')');
    }

    public void newObject(TypeDesc type, int dimensions) {
        if (dimensions == 0 && !type.isArray()) {
            newObject(type);
        } else {
            println(mBulder + "newObject(" + getTypeDescName(type) + ", " +
                    dimensions + ')');
        }
    }

    public void dup() {
        println(mBulder + "dup()");
    }

    public void dupX1() {
        println(mBulder + "dupX1()");
    }

    public void dupX2() {
        println(mBulder + "dupX2()");
    }

    public void dup2() {
        println(mBulder + "dup2()");
    }

    public void dup2X1() {
        println(mBulder + "dup2X1()");
    }

    public void dup2X2() {
        println(mBulder + "dup2X2()");
    }

    public void pop() {
        println(mBulder + "pop()");
    }

    public void pop2() {
        println(mBulder + "pop2()");
    }

    public void swap() {
        println(mBulder + "swap()");
    }

    public void swap2() {
        println(mBulder + "swap2()");
    }

    public void branch(Location location) {
        println(mBulder + "branch(" + getLabelName(location) + ')');
    }

    public void ifNullBranch(Location location, boolean choice) {
        println(mBulder + "ifNullBranch(" +
                getLabelName(location) + ", " + choice + ')');
    }

    public void ifEqualBranch(Location location, boolean choice) {
        println(mBulder + "ifEqualBranch(" +
                getLabelName(location) + ", " + choice + ')');
    }

    public void ifZeroComparisonBranch(Location location, String choice) {
        println(mBulder + "ifZeroComparisonBranch(" +
                getLabelName(location) + ", \"" + choice + "\")");
    }

    public void ifComparisonBranch(Location location, String choice) {
        println(mBulder + "ifComparisonBranch(" +
                getLabelName(location) + ", \"" + choice + "\")");
    }

    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {

        StringBuffer buf = new StringBuffer(cases.length * 15);

        buf.append(mBulder + "switchBranch(");

        buf.append("new int[] {");
        for (int i=0; i<cases.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(cases[i]);
        }
        buf.append("}");

        buf.append(", ");

        buf.append("new Location[] {");
        for (int i=0; i<locations.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(getLabelName(locations[i]));
        }
        buf.append("}");

        buf.append(", ");
        buf.append(getLabelName(defaultLocation));
        buf.append(')');

        println(buf.toString());
    }

    public void jsr(Location location) {
        println(mBulder + "jsr(" + getLabelName(location) + ')');
    }

    public void ret(LocalVariable local) {
        println(mBulder + "ret(" + local.getName() + ')');
    }

    public void math(byte opcode) {
        println
            (mBulder + "math(Opcode." + Opcode.getMnemonic(opcode).toUpperCase() + ')');
    }

    public void arrayLength() {
        println(mBulder + "arrayLength()");
    }

    public void throwObject() {
        println(mBulder + "throwObject()");
    }

    public void checkCast(TypeDesc type) {
        println(mBulder + "checkCast(" + getTypeDescName(type) + ')');
    }

    public void instanceOf(TypeDesc type) {
        println(mBulder + "instanceOf(" + getTypeDescName(type) + ')');
    }

    public void integerIncrement(LocalVariable local, int amount) {
        println(mBulder + "integerIncrement(" + local.getName() + ", " + amount + ')');
    }

    public void monitorEnter() {
        println(mBulder + "monitorEnter()");
    }

    public void monitorExit() {
        println(mBulder + "monitorExit()");
    }

    public void nop() {
        println(mBulder + "nop()");
    }

    public void breakpoint() {
        println(mBulder + "breakpoint()");
    }

    private void separatorLine() {
        if (mNeedSeparatorLine) {
            mWriter.println();
            mNeedSeparatorLine = false;
        }
    }

    private void println(String str) {
        mNeedSeparatorLine = true;
        if (mLinePrefix != null) {
            mWriter.print(mLinePrefix);
        }
        if (mLineSuffix == null) {
            mWriter.println(str);
        } else {
            mWriter.print(str);
            mWriter.println(mLineSuffix);
        }
    }

    private String getLabelName(Location location) {
        if (location instanceof NamedLabel) {
            return ((NamedLabel)location).mName;
        } else {
            return ((NamedLabel)createLabel()).mName;
        }
    }

    private String getTypeDescName(TypeDesc type) {
        if (type == null) {
            return "null";
        }

        String name = mTypeDescNames.get(type);

        if (name == null) {
            if (type.isPrimitive()) {
                name = "TypeDesc.".concat(type.getRootName().toUpperCase());
                mTypeDescNames.put(type, name);
                return name;
            } else if (type == TypeDesc.OBJECT) {
                mTypeDescNames.put(type, name = "TypeDesc.OBJECT");
                return name;
            } else if (type == TypeDesc.STRING) {
                mTypeDescNames.put(type, name = "TypeDesc.STRING");
                return name;
            }

            name = "type_" + (++mTypeDescCounter);
            mTypeDescNames.put(type, name);

            StringBuffer buf = new StringBuffer("TypeDesc ");
            buf.append(name);
            buf.append(" = ");

            TypeDesc componentType = type.getComponentType();
            if (componentType != null) {
                buf.append(getTypeDescName(componentType));
                buf.append(".toArrayType(");
            } else {
                buf.append("TypeDesc.forClass(");
                buf.append('"');
                buf.append(type.getRootName());
                buf.append('"');
            }

            buf.append(')');
            println(buf.toString());
        }

        return name;
    }

    private String getTypeDescArrayName(TypeDesc[] types) {
        if (types == null) {
            return "null";
        }

        List<TypeDesc> key = Arrays.asList(types);
        String name = mTypeDescArrayNames.get(key);

        if (name == null) {
            name = "params_" + (++mTypeDescArrayCounter);
            mTypeDescArrayNames.put(key, name);

            StringBuffer buf = new StringBuffer("TypeDesc[] ");
            buf.append(name);
            buf.append(" = new TypeDesc[] {");

            for (int i=0; i<types.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(getTypeDescName(types[i]));
            }

            buf.append('}');
            println(buf.toString());
        }

        return name;
    }

    static String escape(String value) {
        return escape(value, false);
    }

    static String escape(String value, boolean forChar) {
        int length = value.length();
        int i = 0;
        for (; i < length; i++) {
            char c = value.charAt(i);
            if (c < 32 || c > 126 || c == '"' || c == '\\' || (forChar && c == '\'')) {
                break;
            }
        }

        if (i >= length) {
            return value;
        }

        StringBuffer buf = new StringBuffer(length + 16);
        for (i=0; i<length; i++) {
            char c = value.charAt(i);
            if (c >= 32 && c <= 126 && c != '"' && c != '\\' && (!forChar || c != '\'')) {
                buf.append(c);
                continue;
            }

            switch (c) {
            case '\0':
                buf.append("\\0");
                break;
            case '"':
                buf.append("\\\"");
                break;
            case '\'':
                buf.append("\\'");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            case '\b':
                buf.append("\\b");
                break;
            case '\f':
                buf.append("\\f");
                break;
            case '\n':
                buf.append("\\n");
                break;
            case '\r':
                buf.append("\\r");
                break;
            case '\t':
                buf.append("\\t");
                break;
            default:
                String u = Integer.toHexString(c).toLowerCase();
                buf.append("\\u");
                for (int len = u.length(); len < 4; len++) {
                    buf.append('0');
                }
                buf.append(u);
                break;
            }
        }

        return buf.toString();
    }

    private class NamedLocal implements LocalVariable {
        private final String mName;
        private final TypeDesc mType;
        private final int mNumber;

        public NamedLocal(String name, TypeDesc type, int number) {
            mName = name;
            mType = type;
            mNumber = number;
        }

        public String getName() {
            return mName;
        }
        
        public void setName(String name) {
            println(mName + ".setName(" + name + ')');
        }
        
        public TypeDesc getType() {
            return mType;
        }
        
        public boolean isDoubleWord() {
            return mType.isDoubleWord();
        }
        
        public int getNumber() {
            return mNumber;
        }
        
        public Location getStartLocation() {
            return null;
        }
        
        public Location getEndLocation() {
            return null;
        }

        public Set<LocationRange> getLocationRangeSet() {
            return null;
        }
    }

    private class NamedLabel implements Label {
        public final String mName;

        public NamedLabel(String name) {
            mName = name;
        }

        public Label setLocation() {
            println(mName + ".setLocation()");
            return this;
        }
        
        public int getLocation() {
            return -1;
        }

        public int compareTo(Location obj) {
            return 0;
        }
    }
}
