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
import java.util.MissingResourceException;

/**
 * CodeAssembler is a high-level interface for assembling Java Virtual Machine
 * byte code. It can also be used as a visitor to a disassembler.
 *
 * @author Brian S O'Neill
 */
public interface CodeAssembler {
    /** Convert floating point values as normal */
    public final static int CONVERT_FP_NORMAL = 0;
    /** Convert floating point values as bits (NaN is canonicalized) */
    public final static int CONVERT_FP_BITS = 1;
    /** Convert floating point values as raw bits */
    public final static int CONVERT_FP_RAW_BITS = 2;

    /**
     * Returns the amount of parameters that are accepted by the method being
     * built, not including any "this" reference.
     */
    int getParameterCount();

    /**
     * Returns a specific parameter, whose index lies within 0 to
     * getParameterCount() - 1. The names of the LocalVariables returned by
     * this method are initially set to null. It is encouraged that a name be
     * provided.
     */
    LocalVariable getParameter(int index) throws IndexOutOfBoundsException;

    /**
     * Creates a LocalVariable reference from a name and type. Although name
     * is optional, it is encouraged that a name be provided. Names do not 
     * need to be unique.
     *
     * @param name Optional name for the LocalVariable.
     * @param type The type of data that the requested LocalVariable can 
     * store. 
     */
    LocalVariable createLocalVariable(String name, TypeDesc type);

    /**
     * Creates a label, whose location must be set. To create a label and
     * locate it here, the following example demonstrates how the call to
     * setLocation can be chained:
     *
     * <pre>
     * CodeBuilder builder;
     * ...
     * Label label = builder.createLabel().setLocation();
     * </pre>
     *
     * @see Label#setLocation
     */ 
    Label createLabel();

    /**
     * Sets up an exception handler located here, the location of the next
     * code to be generated.
     *
     * @param startLocation Location at the start of the section of
     * code to be wrapped by an exception handler.
     * @param endLocation Location directly after the end of the
     * section of code.
     * @param catchClassName The class name of exception to be caught; 
     * if null, then catch every object.
     */
    void exceptionHandler(Location startLocation,
                          Location endLocation,
                          String catchClassName);
    
    /**
     * Map the location of the next code to be generated to a line number 
     * in source code. This enables line numbers in a stack trace from the 
     * generated code.
     */
    void mapLineNumber(int lineNumber);

    /**
     * Allows code to disassembled and copied straight in. The code object
     * passed in must have a single method named "define" whose arguments match
     * the type and order of values expected on the operand stack. If a return
     * value is provided, it will pushed onto the stack. The define method can
     * have any access modifier.
     *
     * @throws IllegalArgumentException if define method not found, or if
     * multiple are found
     * @throws MissingResourceException if define code not found
     */
    void inline(Object code) throws IllegalArgumentException, MissingResourceException;

    // load-constant-to-stack style instructions

    /**
     * Generates code that loads a null reference onto the stack.
     */
    void loadNull();

    /**
     * Generates code that loads a constant string value onto the stack. If
     * value is null, the generated code loads a null onto the stack. Strings
     * that exceed 65535 UTF encoded bytes in length are loaded by creating a
     * StringBuffer (or a StringBuilder), appending substrings, and then
     * converting it to a String.
     */
    void loadConstant(String value);

    /**
     * Generates code that loads a constant class value onto the stack.
     * If value is null, the generated code loads a null onto the stack.
     *
     * @param type any object or primitive type
     * @throws IllegalStateException if class file target version does not
     * support this feature
     */
    void loadConstant(TypeDesc type) throws IllegalStateException;

    /**
     * Generates code that loads a constant boolean value onto the stack.
     */
    void loadConstant(boolean value);

    /**
     * Generates code that loads a constant int, char, short or byte value 
     * onto the stack.
     */
    void loadConstant(int value);

    /**
     * Generates code that loads a constant long value onto the stack.
     */
    void loadConstant(long value);

    /**
     * Generates code that loads a constant float value onto the stack.
     */
    void loadConstant(float value);

    /**
     * Generates code that loads a constant double value onto the stack.
     */
    void loadConstant(double value);

    // load-local-to-stack style instructions

    /**
     * Generates code that loads a local variable onto the stack. Parameters
     * passed to a method and the "this" reference are all considered local
     * variables, as well as any that were created.
     *
     * @param local The local variable reference
     */
    void loadLocal(LocalVariable local);

    /**
     * Loads a reference to "this" onto the stack. Static methods have no 
     * "this" reference, and an exception is thrown when attempting to
     * generate "this" in a static method.
     */
    void loadThis();

    // store-from-stack-to-local style instructions

    /**
     * Generates code that pops a value off of the stack into a local variable.
     * Parameters passed to a method and the "this" reference are all 
     * considered local variables, as well as any that were created.
     *
     * @param local The local variable reference
     * @see #getParameter
     * @see #createLocalVariable
     */
    void storeLocal(LocalVariable local);

    // load-to-stack-from-array style instructions

    /**
     * Generates code that loads a value from an array. An array
     * reference followed by an index must be on the stack. The array 
     * reference and index are replaced by the value retrieved from the array 
     * after the generated instruction has executed.
     * <p>
     * The type doesn't need to be an exact match for objects.
     * TypeDesc.OBJECT works fine for all objects. For primitive types, use
     * the appropriate TypeDesc. For an int, the type is TypeDesc.INT.
     *
     * @param type The type of data stored in the array.
     */
    void loadFromArray(TypeDesc type);

    // store-to-array-from-stack style instructions

    /**
     * Generates code that stores a value to an array. An array
     * reference followed by an index, followed by a value (or two if a long
     * or double) must be on the stack. All items on the stack are gone
     * after the generated instruction has executed.
     * <p>
     * The type doesn't need to be an exact match for objects.
     * TypeDesc.OBJECT works fine for all objects. For primitive types, use
     * the appropriate TypeDesc. For an int, the type is TypeDesc.INT.
     *
     * @param type The type of data stored in the array.
     */
    void storeToArray(TypeDesc type);

    // load-field-to-stack style instructions

    /**
     * Generates code that loads a value from a field from this class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    void loadField(String fieldName,
                   TypeDesc type);

    /**
     * Generates code that loads a value from a field from any class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    void loadField(String className,
                   String fieldName,
                   TypeDesc type);

    /**
     * Generates code that loads a value from a field from any class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     *
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void loadField(TypeDesc classDesc,
                   String fieldName,
                   TypeDesc type);

    /**
     * Generates code that loads a value from a static field from this class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    void loadStaticField(String fieldName,
                         TypeDesc type);

    /**
     * Generates code that loads a value from a static field from any class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    void loadStaticField(String className,
                         String fieldName,
                         TypeDesc type);

    /**
     * Generates code that loads a value from a static field from any class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     *
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void loadStaticField(TypeDesc classDesc,
                         String fieldName,
                         TypeDesc type);

    // store-to-field-from-stack style instructions

    /**
     * Generates code that stores a value into a field from this class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    void storeField(String fieldName,
                    TypeDesc type);

    /**
     * Generates code that stores a value into a field from any class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    void storeField(String className,
                    String fieldName,
                    TypeDesc type);

    /**
     * Generates code that stores a value into a field from any class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     *
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void storeField(TypeDesc classDesc,
                    String fieldName,
                    TypeDesc type);

    /**
     * Generates code that stores a value into a field from this class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    void storeStaticField(String fieldName,
                          TypeDesc type);

    /**
     * Generates code that stores a value into a field from any class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    void storeStaticField(String className,
                          String fieldName,
                          TypeDesc type);

    /**
     * Generates code that stores a value into a field from any class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     *
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void storeStaticField(TypeDesc classDesc,
                          String fieldName,
                          TypeDesc type);

    // return style instructions

    /**
     * Generates code that returns void.
     */
    void returnVoid();

    /**
     * Generates code that returns an object or primitive type. The value to
     * return must be on the stack.
     * <p>
     * The type doesn't need to be an exact match for objects.
     * TypeDesc.OBJECT works fine for all objects. For primitive types, use
     * the appropriate TypeDesc. For an int, the type is TypeDesc.INT.
     */
    void returnValue(TypeDesc type);

    // numerical conversion style instructions

    /**
     * Generates code that converts the value of a primitive type already
     * on the stack. Conversions between all primitive types are supported as
     * well as boxing and unboxing conversions. Some example conversions:
     *
     * <pre>
     * int to char
     * byte to double
     * Double to double
     * Float to boolean
     * long to Long
     * Double to Short
     * </pre>
     * 
     * In all, 240 conversions are supported.
     *
     * @throws IllegalArgumentException if conversion not supported
     */
    void convert(TypeDesc fromType, TypeDesc toType);

    /**
     * Generates code that converts the value of a primitive type already
     * on the stack. Conversions between all primitive types are supported as
     * well as boxing and unboxing conversions. Some example conversions:
     *
     * <pre>
     * int to char
     * byte to double
     * Double to double
     * Float to boolean
     * long to Long
     * Double to Short
     * </pre>
     * 
     * In all, 240 conversions are supported.
     *
     * @param fpConvertMode controls floating point conversion if converting
     * float &lt;--&gt; int or double &lt;--&gt; long
     * @throws IllegalArgumentException if conversion not supported
     */
    void convert(TypeDesc fromType, TypeDesc toType, int fpConvertMode);

    // invocation style instructions

    /**
     * Generates code to invoke a method. If the method is static, the method's
     * argument(s) must be on the stack. If the method is non-static, then the
     * object reference must also be on the stack, prior to the arguments.
     */
    void invoke(Method method);

    /**
     * Generates code to invoke a virtual method in this class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeVirtual(String methodName,
                       TypeDesc ret,
                       TypeDesc[] params);

    /**
     * Generates code to invoke a virtual method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeVirtual(String className,
                       String methodName,
                       TypeDesc ret,
                       TypeDesc[] params);

    /**
     * Generates code to invoke a virtual method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void invokeVirtual(TypeDesc classDesc,
                       String methodName,
                       TypeDesc ret,
                       TypeDesc[] params);

    /**
     * Generates code to invoke a static method in this class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeStatic(String methodName,
                      TypeDesc ret,
                      TypeDesc[] params);

    /**
     * Generates code to invoke a static method in any class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeStatic(String className,
                      String methodName,
                      TypeDesc ret,
                      TypeDesc[] params);

    /**
     * Generates code to invoke a static method in any class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void invokeStatic(TypeDesc classDesc,
                      String methodName,
                      TypeDesc ret,
                      TypeDesc[] params);

    /**
     * Generates code to invoke an interface method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeInterface(String className,
                         String methodName,
                         TypeDesc ret,
                         TypeDesc[] params);

    /**
     * Generates code to invoke an interface method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void invokeInterface(TypeDesc classDesc,
                         String methodName,
                         TypeDesc ret,
                         TypeDesc[] params);

    /**
     * Generates code to invoke a private method in this class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokePrivate(String methodName,
                       TypeDesc ret,
                       TypeDesc[] params);
    
    /**
     * Generates code to invoke a method in the super class.
     * The object reference and the method's argument(s) must be on the stack.
     */
    void invokeSuper(Method method);

    /**
     * Generates code to invoke a method in the super class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    void invokeSuper(String superClassName,
                     String methodName,
                     TypeDesc ret,
                     TypeDesc[] params);

    /**
     * Generates code to invoke a method in the super class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     * @throws IllegalArgumentException if superClassDesc refers to an array or
     * primitive type
     */
    void invokeSuper(TypeDesc superClassDesc,
                     String methodName,
                     TypeDesc ret,
                     TypeDesc[] params);

    /**
     * Generates code to invoke a class constructor in any class. The object
     * reference and the constructor's argument(s) must be on the stack.
     */
    void invoke(Constructor constructor);

    /**
     * Generates code to invoke a class constructor in this class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    void invokeConstructor(TypeDesc[] params);

    /**
     * Generates code to invoke a class constructor in any class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    void invokeConstructor(String className, TypeDesc[] params);

    /**
     * Generates code to invoke a class constructor in any class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     * @throws IllegalArgumentException if classDesc refers to an array or
     * primitive type
     */
    void invokeConstructor(TypeDesc classDesc, TypeDesc[] params);

    /**
     * Generates code to invoke a super class constructor. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    void invokeSuperConstructor(TypeDesc[] params);

    // creation style instructions

    /**
     * Generates code to create a new object. Unless the new object is an 
     * array, it is invalid until a constructor method is invoked on it.
     * <p>
     * If the specified type is an array, this call is equivalent to
     * newObject(type, 1). The size of the dimension must be on the operand
     * stack. To create multi-dimensional arrays, call
     * newObject(type, dimensions).
     *
     * @see #invokeConstructor
     */
    void newObject(TypeDesc type);

    /**
     * Generates code to create a new array. The type descriptor specifies
     * the type of array to create. The dimensions parameter specifies the
     * amount of dimensions that will initialized, which may not be larger than
     * the amount of dimensions specified in the type.
     * <p>
     * For each dimension, its size must be on the operand stack. If the
     * specified dimensions is 0 and the type is not an array, then this call
     * is equivalent to newObject(type).
     */
    void newObject(TypeDesc type, int dimensions);

    // stack operation style instructions

    /**
     * Generates code for the dup instruction.
     */
    void dup();

    /**
     * Generates code for the dup_x1 instruction.
     */
    void dupX1();

    /**
     * Generates code for the dup_x2 instruction.
     */
    void dupX2();

    /**
     * Generates code for the dup2 instruction.
     */
    void dup2();

    /**
     * Generates code for the dup2_x1 instruction.
     */
    void dup2X1();

    /**
     * Generates code for the dup2_x2 instruction.
     */
    void dup2X2();

    /**
     * Generates code for the pop instruction.
     */
    void pop();

    /**
     * Generates code for the pop2 instruction.
     */
    void pop2();

    /**
     * Generates code for the swap instruction.
     */
    void swap();

    /**
     * Generates code for a swap2 instruction.
     */
    void swap2();

    // flow control instructions

    /**
     * Generates code that performs an unconditional branch to the specified
     * location.
     *
     * @param location The location to branch to
     */
    void branch(Location location);

    /**
     * Generates code that performs a conditional branch based on the
     * value of an object on the stack. A branch is performed based on whether
     * the object reference on the stack is null or not.
     *
     * <p>The generated instruction consumes the value on the stack.
     *
     * @param location The location to branch to
     * @param choice If true, do branch when null, else branch when not null
     */
    void ifNullBranch(Location location, boolean choice);

    /**
     * Generates code that performs a conditional branch based on the value of
     * two object references on the stack. A branch is performed based on
     * whether the two objects are exactly the same.
     *
     * <p>The generated instruction consumes the two values on the stack.
     *
     * @param location The location to branch to
     * @param choice If true, branch when equal, else branch when not equal
     */
    void ifEqualBranch(Location location, boolean choice);

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between an int value on the stack and zero. The int value on the
     * stack is on the left side of the comparison expression.
     *
     * <p>The generated instruction consumes the value on the stack.
     *
     * @param location The location to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @throws IllegalArgumentException When the choice is not valid
     */
    void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException;

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between two int values on the stack. The first int value on the stack
     * is on the left side of the comparison expression.
     *
     * <p>The generated instruction consumes the two values on the stack.
     *
     * @param location The location to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @throws IllegalArgumentException When the choice is not valid
     */
    void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException;

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between two values of the given type on the stack. The first int value
     * on the stack is on the left side of the comparison expression. When
     * comparing objects, only an identity comparison is performed.
     *
     * <p>When comparing floating point values, treatment of NaN requires
     * special attention. Ordinarily, it is assumed that the branch location
     * represents the target of a comparison failure, and that the code to
     * handle the "true" condition immediately follows the comparison. If this
     * is not the case, append a 't' suffix to the choice to indicate that the
     * target location is reached for a "true" condition. This suffix is
     * ignored if the type is not a float or double.
     *
     * <p>The generated instruction(s) consumes the two values on the stack.
     *
     * @param location The location to branch to

     * @param choice One of "==", "!=", "<", ">=", ">", "<=", "==t", "!=t",
     * "<t", ">=t", ">t", or "<=t". Object types can only be compared for
     * equality.
     * @param type Type to expect on the stack
     * @throws IllegalArgumentException When the choice is not valid
     */
    void ifComparisonBranch(Location location, String choice, TypeDesc type)
        throws IllegalArgumentException;

    /**
     * Generates code for a switch statement. The generated code is either a
     * lookupswitch or tableswitch. The choice of which switch type to generate
     * is made based on the amount of bytes to be generated. A tableswitch
     * is usually smaller, unless the cases are sparse.
     *
     * <p>The key value to switch on must already be on the stack when this
     * instruction executes. It is consumed by the instruction.
     *
     * @param cases The values to match on. The array length must be the same
     * as for locations.
     * @param locations The locations to branch to for each case.
     * The array length must be the same as for cases.
     * @param defaultLocation The location to branch to if the key on
     * the stack was not matched.
     */
    void switchBranch(int[] cases, 
                      Location[] locations, Location defaultLocation);

    /**
     * Generates code that performs a subroutine branch to the specified 
     * location. The instruction generated is either jsr or jsr_w. It is most
     * often used for implementing a finally block.
     *
     * @param location The location to branch to
     */
    void jsr(Location location);

    /**
     * Generates code that returns from a subroutine invoked by jsr.
     *
     * @param local The local variable reference that contains the return
     * address. The local variable must be of an object type.
     */
    void ret(LocalVariable local);

    // math instructions

    /**
     * Generates code for either a unary or binary math operation on one
     * or two values pushed on the stack.
     * <p>
     * Pass in an opcode from the the Opcode class. The only valid math
     * opcodes are:
     *
     * <pre>
     * IADD, ISUB, IMUL, IDIV, IREM, INEG, IAND, IOR, IXOR, ISHL, ISHR, IUSHR
     * LADD, LSUB, LMUL, LDIV, LREM, LNEG, LAND, LOR, LXOR, LSHL, LSHR, LUSHR
     * FADD, FSUB, FMUL, FDIV, FREM, FNEG
     * DADD, DSUB, DMUL, DDIV, DREM, DNEG
     *
     * LCMP
     * FCMPG, FCMPL
     * DCMPG, DCMPL
     * </pre>
     *
     * A not operation (~) is performed by doing a loadConstant with either
     * -1 or -1L followed by math(Opcode.IXOR) or math(Opcode.LXOR).
     *
     * @param opcode An opcode from the Opcode class.
     * @throws IllegalArgumentException When the opcode selected is not
     * a math operation.
     * @see Opcode
     */
    void math(byte opcode);

    // miscellaneous instructions

    /**
     * Generates code for an arraylength instruction. The object to get the
     * length from must already be on the stack.
     */
    void arrayLength();

    /**
     * Generates code that throws an exception. The object to throw must
     * already be on the stack.
     */
    void throwObject();

    /**
     * Generates code that performs an object cast operation. The object
     * to check must already be on the stack.
     */
    void checkCast(TypeDesc type);

    /**
     * Generates code that performs an instanceof operation. The object to
     * check must already be on the stack.
     */
    void instanceOf(TypeDesc type);

    /**
     * Generates code that increments a local integer variable by a signed 
     * constant amount.
     */
    void integerIncrement(LocalVariable local, int amount);

    /**
     * Generates code to enter the monitor on an object loaded on the stack.
     */
    void monitorEnter();

    /**
     * Generates code to exit the monitor on an object loaded on the stack.
     */
    void monitorExit();

    /**
     * Generates an instruction that does nothing. (No-OPeration)
     */
    void nop();

    /**
     * Generates a breakpoint instruction for use in a debugging environment.
     */
    void breakpoint();
}
