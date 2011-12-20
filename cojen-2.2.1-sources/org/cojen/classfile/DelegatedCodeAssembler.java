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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Delegates all method calls to another CodeAssembler. Override any method to
 * track activity or change the way code is generated.
 *
 * @author Brian S O'Neill
 */
public class DelegatedCodeAssembler implements CodeAssembler {
    protected final CodeAssembler mAssembler;

    public DelegatedCodeAssembler(CodeAssembler assembler) {
        if (assembler == null) {
            throw new IllegalArgumentException();
        }
        mAssembler = assembler;
    }

    public int getParameterCount() {
        return mAssembler.getParameterCount();
    }

    public LocalVariable getParameter(int index) {
        return mAssembler.getParameter(index);
    }

    public LocalVariable createLocalVariable(String name, TypeDesc type) {
        return mAssembler.createLocalVariable(name, type);
    }

    public Label createLabel() {
        return mAssembler.createLabel();
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        mAssembler.exceptionHandler
            (startLocation, endLocation, catchClassName);
    }
    
    public void mapLineNumber(int lineNumber) {
        mAssembler.mapLineNumber(lineNumber);
    }

    public void inline(Object code) {
        mAssembler.inline(code);
    }

    public void loadNull() {
        mAssembler.loadNull();
    }

    public void loadConstant(String value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(TypeDesc type) {
        mAssembler.loadConstant(type);
    }

    public void loadConstant(boolean value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(int value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(long value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(float value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(double value) {
        mAssembler.loadConstant(value);
    }

    public void loadLocal(LocalVariable local) {
        mAssembler.loadLocal(local);
    }

    public void loadThis() {
        mAssembler.loadThis();
    }

    public void storeLocal(LocalVariable local) {
        mAssembler.storeLocal(local);
    }

    public void loadFromArray(TypeDesc type) {
        mAssembler.loadFromArray(type);
    }

    public void storeToArray(TypeDesc type) {
        mAssembler.storeToArray(type);
    }

    public void loadField(String fieldName,
                          TypeDesc type) {
        mAssembler.loadField(fieldName, type);
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDesc type) {
        mAssembler.loadField(className, fieldName, type);
    }

    public void loadField(TypeDesc classDesc,
                          String fieldName,
                          TypeDesc type) {
        mAssembler.loadField(classDesc, fieldName, type);
    }

    public void loadStaticField(String fieldName,
                                TypeDesc type) {
        mAssembler.loadStaticField(fieldName, type);
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDesc type) {
        mAssembler.loadStaticField(className, fieldName, type);
    }

    public void loadStaticField(TypeDesc classDesc,
                                String fieldName,
                                TypeDesc type) {
        mAssembler.loadStaticField(classDesc, fieldName, type);
    }

    public void storeField(String fieldName,
                           TypeDesc type) {
        mAssembler.storeField(fieldName, type);
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDesc type) {
        mAssembler.storeField(className, fieldName, type);
    }

    public void storeField(TypeDesc classDesc,
                           String fieldName,
                           TypeDesc type) {
        mAssembler.storeField(classDesc, fieldName, type);
    }

    public void storeStaticField(String fieldName,
                                 TypeDesc type) {
        mAssembler.storeStaticField(fieldName, type);
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDesc type) {
        mAssembler.storeStaticField(className, fieldName, type);
    }

    public void storeStaticField(TypeDesc classDesc,
                                 String fieldName,
                                 TypeDesc type) {
        mAssembler.storeStaticField(classDesc, fieldName, type);
    }

    public void returnVoid() {
        mAssembler.returnVoid();
    }

    public void returnValue(TypeDesc type) {
        mAssembler.returnValue(type);
    }

    public void convert(TypeDesc fromType, TypeDesc toType) {
        mAssembler.convert(fromType, toType);
    }

    public void convert(TypeDesc fromType, TypeDesc toType, int fpConvertMode) {
        mAssembler.convert(fromType, toType, fpConvertMode);
    }

    public void invoke(Method method) {
        mAssembler.invoke(method);
    }

    public void invokeVirtual(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mAssembler.invokeVirtual(methodName, ret, params);
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mAssembler.invokeVirtual(className, methodName, ret, params);
    }

    public void invokeVirtual(TypeDesc classDesc,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mAssembler.invokeVirtual(classDesc, methodName, ret, params);
    }

    public void invokeStatic(String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mAssembler.invokeStatic(methodName, ret, params);
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mAssembler.invokeStatic(className, methodName, ret, params);
    }

    public void invokeStatic(TypeDesc classDesc,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc[] params) {
        mAssembler.invokeStatic(classDesc, methodName, ret, params);
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        mAssembler.invokeInterface(className, methodName, ret, params);
    }

    public void invokeInterface(TypeDesc classDesc,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        mAssembler.invokeInterface(classDesc, methodName, ret, params);
    }

    public void invokePrivate(String methodName,
                              TypeDesc ret,
                              TypeDesc[] params) {
        mAssembler.invokePrivate(methodName, ret, params);
    }

    public void invokeSuper(Method method) {
        mAssembler.invokeSuper(method);
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        mAssembler.invokeSuper(superClassName, methodName, ret, params);
    }

    public void invokeSuper(TypeDesc superClassDesc,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc[] params) {
        mAssembler.invokeSuper(superClassDesc, methodName, ret, params);
    }

    public void invoke(Constructor constructor) {
        mAssembler.invoke(constructor);
    }

    public void invokeConstructor(TypeDesc[] params) {
        mAssembler.invokeConstructor(params);
    }

    public void invokeConstructor(String className, TypeDesc[] params) {
        mAssembler.invokeConstructor(className, params);
    }

    public void invokeConstructor(TypeDesc classDesc, TypeDesc[] params) {
        mAssembler.invokeConstructor(classDesc, params);
    }

    public void invokeSuperConstructor(TypeDesc[] params) {
        mAssembler.invokeSuperConstructor(params);
    }

    public void newObject(TypeDesc type) {
        mAssembler.newObject(type);
    }

    public void newObject(TypeDesc type, int dimensions) {
        mAssembler.newObject(type, dimensions);
    }

    public void dup() {
        mAssembler.dup();
    }

    public void dupX1() {
        mAssembler.dupX1();
    }

    public void dupX2() {
        mAssembler.dupX2();
    }

    public void dup2() {
        mAssembler.dup2();
    }

    public void dup2X1() {
        mAssembler.dup2X1();
    }

    public void dup2X2() {
        mAssembler.dup2X2();
    }

    public void pop() {
        mAssembler.pop();
    }

    public void pop2() {
        mAssembler.pop2();
    }

    public void swap() {
        mAssembler.swap();
    }

    public void swap2() {
        mAssembler.swap2();
    }

    public void branch(Location location) {
        mAssembler.branch(location);
    }

    public void ifNullBranch(Location location, boolean choice) {
        mAssembler.ifNullBranch(location, choice);
    }

    public void ifEqualBranch(Location location, boolean choice) {
        mAssembler.ifEqualBranch(location, choice);
    }

    public void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException {
        mAssembler.ifZeroComparisonBranch(location, choice);
    }

    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {
        mAssembler.ifComparisonBranch(location, choice);
    }

    public void ifComparisonBranch(Location location, String choice, TypeDesc type)
        throws IllegalArgumentException {
        mAssembler.ifComparisonBranch(location, choice, type);
    }

    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {
        mAssembler.switchBranch(cases, locations, defaultLocation);
    }

    public void jsr(Location location) {
        mAssembler.jsr(location);
    }

    public void ret(LocalVariable local) {
        mAssembler.ret(local);
    }

    public void math(byte opcode) {
        mAssembler.math(opcode);
    }

    public void arrayLength() {
        mAssembler.arrayLength();
    }

    public void throwObject() {
        mAssembler.throwObject();
    }

    public void checkCast(TypeDesc type) {
        mAssembler.checkCast(type);
    }

    public void instanceOf(TypeDesc type) {
        mAssembler.instanceOf(type);
    }

    public void integerIncrement(LocalVariable local, int amount) {
        mAssembler.integerIncrement(local, amount);
    }

    public void monitorEnter() {
        mAssembler.monitorEnter();
    }

    public void monitorExit() {
        mAssembler.monitorExit();
    }

    public void nop() {
        mAssembler.nop();
    }

    public void breakpoint() {
        mAssembler.breakpoint();
    }
}
