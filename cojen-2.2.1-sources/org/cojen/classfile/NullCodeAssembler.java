/*
 *  Copyright 2008-2010 Brian S O'Neill
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

/**
 * CodeAssembler implementation which discards everything.
 *
 * @author Brian S O'Neill
 */
public class NullCodeAssembler extends AbstractCodeAssembler {
    private final MethodInfo mMethod;
    private final LocalVariable[] mParams;
    private int mInstrCount;

    public NullCodeAssembler(MethodInfo mi) {
        mMethod = mi;
        TypeDesc[] paramTypes = mMethod.getMethodDescriptor().getParameterTypes();
        mParams = new LocalVariable[paramTypes.length];
        for (int i=0; i<paramTypes.length; i++) {
            mParams[i] = new Variable(null, paramTypes[i]);
        }
    }

    protected int getInstructionsSeen() {
        return mInstrCount;
    }

    public int getParameterCount() {
        return mMethod.getMethodDescriptor().getParameterCount();
    }

    public LocalVariable getParameter(int index) {
        return mParams[index];
    }

    public LocalVariable createLocalVariable(String name, TypeDesc type) {
        return new Variable(name, type);
    }

    public Label createLabel() {
        return new DummyLabel();
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        // Handler is not an instruction.
    }
    
    public void mapLineNumber(int lineNumber) {
        // Line number is not an instruction.
    }

    public void loadNull() {
        mInstrCount++;
    }

    public void loadConstant(String value) {
        mInstrCount++;
    }

    public void loadConstant(TypeDesc type) {
        mInstrCount++;
    }

    public void loadConstant(boolean value) {
        mInstrCount++;
    }

    public void loadConstant(int value) {
        mInstrCount++;
    }

    public void loadConstant(long value) {
        mInstrCount++;
    }

    public void loadConstant(float value) {
        mInstrCount++;
    }

    public void loadConstant(double value) {
        mInstrCount++;
    }

    public void loadLocal(LocalVariable local) {
        mInstrCount++;
    }

    public void loadThis() {
        mInstrCount++;
    }

    public void storeLocal(LocalVariable local) {
        mInstrCount++;
    }

    public void loadFromArray(TypeDesc type) {
        mInstrCount++;
    }

    public void storeToArray(TypeDesc type) {
        mInstrCount++;
    }

    public void loadField(String fieldName, TypeDesc type) {
        mInstrCount++;
    }

    public void loadField(String className, String fieldName, TypeDesc type) {
        mInstrCount++;
    }

    public void loadField(TypeDesc classDesc,
                          String fieldName,
                          TypeDesc type) {
        mInstrCount++;
    }

    public void loadStaticField(String fieldName,
                                TypeDesc type) {
        mInstrCount++;
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDesc type) {
        mInstrCount++;
    }

    public void loadStaticField(TypeDesc classDesc,
                                String fieldName,
                                TypeDesc type) {
        mInstrCount++;
    }

    public void storeField(String fieldName,
                           TypeDesc type) {
        mInstrCount++;
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDesc type) {
        mInstrCount++;
    }

    public void storeField(TypeDesc classDesc,
                           String fieldName,
                           TypeDesc type) {
        mInstrCount++;
    }

    public void storeStaticField(String fieldName,
                                 TypeDesc type) {
        mInstrCount++;
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDesc type) {
        mInstrCount++;
    }

    public void storeStaticField(TypeDesc classDesc,
                                 String fieldName,
                                 TypeDesc type) {
        mInstrCount++;
    }

    public void returnVoid() {
        mInstrCount++;
    }

    public void returnValue(TypeDesc type) {
        mInstrCount++;
    }

    public void convert(TypeDesc fromType, TypeDesc toType) {
        mInstrCount++;
    }

    public void convert(TypeDesc fromType, TypeDesc toType, int fpConvertMode) {
        mInstrCount++;
    }

    public void invokeVirtual(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeVirtual(TypeDesc classDesc,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeStatic(String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeStatic(TypeDesc classDesc,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeInterface(TypeDesc classDesc,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokePrivate(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mInstrCount++;
    }
    
    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeSuper(TypeDesc superClassDesc,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeConstructor(TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeConstructor(String className, TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeConstructor(TypeDesc classDesc, TypeDesc[] params) {
        mInstrCount++;
    }

    public void invokeSuperConstructor(TypeDesc[] params) {
        mInstrCount++;
    }

    public void newObject(TypeDesc type) {
        mInstrCount++;
    }

    public void newObject(TypeDesc type, int dimensions) {
        mInstrCount++;
    }

    public void dup() {
        mInstrCount++;
    }

    public void dupX1() {
        mInstrCount++;
    }

    public void dupX2() {
        mInstrCount++;
    }

    public void dup2() {
        mInstrCount++;
    }

    public void dup2X1() {
        mInstrCount++;
    }

    public void dup2X2() {
        mInstrCount++;
    }

    public void pop() {
        mInstrCount++;
    }

    public void pop2() {
        mInstrCount++;
    }

    public void swap() {
        mInstrCount++;
    }

    public void swap2() {
        mInstrCount++;
    }

    public void branch(Location location) {
        mInstrCount++;
    }

    public void ifNullBranch(Location location, boolean choice) {
        mInstrCount++;
    }

    public void ifEqualBranch(Location location, boolean choice) {
        mInstrCount++;
    }

    public void ifZeroComparisonBranch(Location location, String choice) {
        mInstrCount++;
    }

    public void ifComparisonBranch(Location location, String choice) {
        mInstrCount++;
    }

    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {
        mInstrCount++;
    }

    public void jsr(Location location) {
        mInstrCount++;
    }

    public void ret(LocalVariable local) {
        mInstrCount++;
    }

    public void math(byte opcode) {
        mInstrCount++;
    }

    public void arrayLength() {
        mInstrCount++;
    }

    public void throwObject() {
        mInstrCount++;
    }

    public void checkCast(TypeDesc type) {
        mInstrCount++;
    }

    public void instanceOf(TypeDesc type) {
        mInstrCount++;
    }

    public void integerIncrement(LocalVariable local, int amount) {
        mInstrCount++;
    }

    public void monitorEnter() {
        mInstrCount++;
    }

    public void monitorExit() {
        mInstrCount++;
    }

    public void nop() {
        mInstrCount++;
    }

    public void breakpoint() {
        mInstrCount++;
    }

    private static class DummyLabel implements Label {
        DummyLabel() {
        }

        public Label setLocation() {
            return this;
        }

        public int getLocation() {
            return -1;
        }

        public int compareTo(Location loc) {
            return 0;
        }
    }

    private static class Variable implements LocalVariable {
        private String mName;
        private final TypeDesc mType;

        Variable(String name, TypeDesc type) {
            mName = name;
            mType = type;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public TypeDesc getType() {
            return mType;
        }

        public boolean isDoubleWord() {
            return getType().isDoubleWord();
        }

        public int getNumber() {
            return -1;
        }

        public java.util.Set<LocationRange> getLocationRangeSet() {
            return null;
        }
    }
}
