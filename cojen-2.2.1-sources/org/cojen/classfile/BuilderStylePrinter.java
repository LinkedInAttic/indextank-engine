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

import java.io.PrintWriter;

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

/**
 * Disassembles a ClassFile into a Java source file, which when run, produces
 * the original class.
 *
 * @author Brian S O'Neill
 */
class BuilderStylePrinter implements DisassemblyTool.Printer {
    private PrintWriter mOut;

    private int mIndent = 0;
    private boolean mNeedIndent = true;

    public BuilderStylePrinter() {
    }

    public void disassemble(ClassFile cf, PrintWriter out) {
        mOut = out;

        println("import java.io.BufferedOutputStream;");
        println("import java.io.File;");
        println("import java.io.FileOutputStream;");
        println("import java.io.OutputStream;");
        println();
        println("import org.cojen.classfile.ClassFile;");
        println("import org.cojen.classfile.CodeBuilder;");
        println("import org.cojen.classfile.FieldInfo;");
        println("import org.cojen.classfile.Label;");
        println("import org.cojen.classfile.LocalVariable;");
        println("import org.cojen.classfile.Location;");
        println("import org.cojen.classfile.MethodInfo;");
        println("import org.cojen.classfile.Modifiers;");
        println("import org.cojen.classfile.Opcode;");
        println("import org.cojen.classfile.TypeDesc;");

        disassemble(cf, (String)null);
    }
    
    private void disassemble(ClassFile cf, String innerClassSuffix) {
        println();
        if (innerClassSuffix == null) {
            println("/**");
            println(" * Builds ClassFile for " + cf.getClassName());
            println(" *");
            println(" * @author auto-generated");
            println(" */");
            println("public class ClassFileBuilder {");
        } else {
            println("/**");
            println(" * Builds ClassFile for " + cf.getClassName());
            println(" */");
            println("private static class InnerBuilder" + innerClassSuffix + " {");
        }
        mIndent += 4;

        if (innerClassSuffix == null) {
            println("public static void main(String[] args) throws Exception {");
            mIndent += 4;
            println("// " + cf);
            println("ClassFile cf = createClassFile();");

            println();
            println("if (args.length > 0) {");
            mIndent += 4;
            println("File file = new File(args[0]);");
            println("if (file.isDirectory()) {");
            mIndent += 4;
            println("writeClassFiles(cf, file);");
            mIndent -= 4;
            println("} else {");
            mIndent += 4;
            println("OutputStream out = new BufferedOutputStream(new FileOutputStream(file));");
            println("cf.writeTo(out);");
            println("out.close();");
            mIndent -= 4;
            println("}");
            mIndent -= 4;
            println("}");
            mIndent -= 4;
            println("}");

            println();
            println("private static void writeClassFiles(ClassFile cf, File dir) throws Exception {");
            mIndent += 4;
            println("File file = new File(dir, cf.getClassName().replace('.', '/') + \".class\");");
            println("file.getParentFile().mkdirs();");
            println("OutputStream out = new BufferedOutputStream(new FileOutputStream(file));");
            println("cf.writeTo(out);");
            println("out.close();");

            println();
            println("ClassFile[] innerClasses = cf.getInnerClasses();");
            println("for (int i=0; i<innerClasses.length; i++) {");
            mIndent += 4;
            println("writeClassFiles(innerClasses[i], dir);");
            mIndent -= 4;
            println("}");
            mIndent -= 4;
            println("}");

            println();
        }

        if (innerClassSuffix == null) {
            println("public static ClassFile createClassFile() {");
            mIndent += 4;
            println("ClassFile cf = new ClassFile(\"" + escape(cf.getClassName())
                    + "\", \"" + escape(cf.getSuperClassName()) + "\");");
        } else {
            println("static ClassFile createClassFile(ClassFile cf) {");
            mIndent += 4;
        }

        if (cf.getTarget() != null) {
            println("cf.setTarget(\"" + escape(cf.getTarget()) + "\");");
        }
        println("cf.setSourceFile(\"" + escape(cf.getSourceFile()) + "\");");

        if (cf.isSynthetic()) {
            println("cf.markSynthetic();");
        }
        if (cf.isDeprecated()) {
            println("cf.markDeprecated();");
        }

        if (!cf.getModifiers().equals(Modifiers.PUBLIC)) {
            print("cf.setModifiers(");
            printModifiers(cf);
            println(");");
        }

        String[] interfaces = cf.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            println("cf.addInterface(\"" + escape(interfaces[i]) + "\");");
        }

        if (cf.getInitializer() != null) {
            println();
            println("createStaticInitializer(cf);");
        }

        FieldInfo[] fields = cf.getFields();
        boolean createdFieldVariable = false;

        for (int i=0; i<fields.length; i++) {
            if (i == 0) {
                println();
                println("//");
                println("// Create fields");
                println("//");
            }

            println();

            FieldInfo fi = fields[i];
            if (fi.isSynthetic() || fi.isDeprecated() || fi.getConstantValue() != null) {
                if (!createdFieldVariable) {
                    print("FieldInfo ");
                    createdFieldVariable = true;
                }
                print("fi = ");
            }

            print("cf.addField(");
            printModifiers(fi);
            print(", ");
            print('\"' + escape(fi.getName()) + "\", ");
            print(fi.getType());
            println(");");

            if (fi.getConstantValue() != null) {
                ConstantInfo constant = fi.getConstantValue();
                print("fi.setConstantValue(");
                if (constant instanceof ConstantStringInfo) {
                    print("\"");
                    String value = ((ConstantStringInfo)constant).getValue();
                    print(escape(value));
                    print("\"");
                } else if (constant instanceof ConstantIntegerInfo) {
                    print(String.valueOf(((ConstantIntegerInfo)constant).getValue()));
                } else if (constant instanceof ConstantLongInfo) {
                    print(String.valueOf(((ConstantLongInfo)constant).getValue()));
                    print("L");
                } else if (constant instanceof ConstantFloatInfo) {
                    float value = ((ConstantFloatInfo)constant).getValue();
                    if (value != value) {
                        print("0.0f/0.0f");
                    } else if (value == Float.NEGATIVE_INFINITY) {
                        print("-1.0f/0.0f");
                    } else if (value == Float.POSITIVE_INFINITY) {
                        print("1.0f/0.0f");
                    } else {
                        print(String.valueOf(value));
                        print("f");
                    }
                } else if (constant instanceof ConstantDoubleInfo) {
                    double value = ((ConstantDoubleInfo)constant).getValue();
                    if (value != value) {
                        print("0.0d/0.0d");
                    } else if (value == Float.NEGATIVE_INFINITY) {
                        print("-1.0d/0.0d");
                    } else if (value == Float.POSITIVE_INFINITY) {
                        print("1.0d/0.0d");
                    } else {
                        print(String.valueOf(value));
                        print("d");
                    }
                }
                println(");");
            }
            if (fi.isSynthetic()) {
                println("fi.markSynthetic();");
            }
            if (fi.isDeprecated()) {
                println("fi.markDeprecated();");
            }
        }

        MethodInfo[] methods = cf.getConstructors();
        for (int i=0; i<methods.length; i++) {
            if (i == 0) {
                println();
                println("//");
                println("// Create constructors");
                println("//");
            }
            println();
            println("// " + methods[i]);
            println("createConstructor_" + (i + 1) + "(cf);");
        }

        methods = cf.getMethods();
        for (int i=0; i<methods.length; i++) {
            if (i == 0) {
                println();
                println("//");
                println("// Create methods");
                println("//");
            }

            println();
            println("// " + methods[i]);
            println("createMethod_" + (i + 1) + "(cf);");
        }

        final ClassFile[] innerClasses = cf.getInnerClasses();

        for (int i=0; i<innerClasses.length; i++) {
            if (i == 0) {
                println();
                println("//");
                println("// Create inner classes");
                println("//");
            }

            println();
            println("// " + innerClasses[i]);
            
            if (i == 0) {
                print("ClassFile ");
            }
            print("innerClass = ");
            String name = innerClasses[i].getClassName();
            String innerName = innerClasses[i].getInnerClassName();
            if (innerName != null) {
                if ((cf.getClassName() + '$' + innerName).equals(name)) {
                    name = null;
                }
                innerName = '"' + escape(innerName) + '"';
            }
            if (name != null) {
                name = '"' + escape(name) + '"';
            }
            println("cf.addInnerClass(" + name + ", " + innerName + ", \"" +
                    escape(innerClasses[i].getSuperClassName()) + "\");");
            String suffix = "_" + (i + 1);
            if (innerClassSuffix != null) {
                suffix = innerClassSuffix + suffix;
            }
            println("InnerBuilder" + suffix + ".createClassFile(innerClass);");
        }

        println();
        println("return cf;");

        mIndent -= 4;
        println("}");

        if (cf.getInitializer() != null) {
            println();
            println("private static void createStaticInitializer(ClassFile cf) {");
            mIndent += 4;
            disassemble(cf.getInitializer());
            mIndent -= 4;
            println("}");
        }

        methods = cf.getConstructors();
        for (int i=0; i<methods.length; i++) {
            println();
            println("// " + methods[i]);
            println("private static void createConstructor_" + (i + 1) + "(ClassFile cf) {");
            mIndent += 4;
            disassemble(methods[i]);
            mIndent -= 4;
            println("}");
        }

        methods = cf.getMethods();
        for (int i=0; i<methods.length; i++) {
            println();
            println("// " + methods[i]);
            println("private static void createMethod_" + (i + 1) + "(ClassFile cf) {");
            mIndent += 4;
            disassemble(methods[i]);
            mIndent -= 4;
            println("}");
        }

        for (int i=0; i<innerClasses.length; i++) {
            String suffix = "_" + (i + 1);
            if (innerClassSuffix != null) {
                suffix = innerClassSuffix + suffix;
            }
            disassemble(innerClasses[i], suffix);
        }

        mIndent -= 4;
        println("}");
    }

    private void disassemble(MethodInfo mi) {
        print("MethodInfo mi = cf.add");

        if (mi.getName().equals("<clinit>")) {
            println("Initializer();");
        } else if (mi.getName().equals("<init>")) {
            print("Constructor(");
            printModifiers(mi);
            print(", ");
            print(mi.getMethodDescriptor().getParameterTypes());
            println(");");
        } else {
            print("Method(");
            printModifiers(mi);
            print(", ");
            print("\"" + escape(mi.getName()) + "\", ");
            print(mi.getMethodDescriptor().getReturnType());
            print(", ");
            print(mi.getMethodDescriptor().getParameterTypes());
            println(");");
        }

        if (mi.isSynthetic()) {
            println("mi.markSynthetic();");
        }
        if (mi.isDeprecated()) {
            println("mi.markDeprecated();");
        }
        TypeDesc[] exceptions = mi.getExceptions();
        for (int j=0; j<exceptions.length; j++) {
            print("mi.addException(");
            print(exceptions[j]);
            println(");");
        }

        if (mi.getCodeAttr() != null) {
            println("CodeBuilder b = new CodeBuilder(mi);");
            println();

            TypeDesc[] paramTypes = mi.getMethodDescriptor().getParameterTypes();
            boolean isStatic = mi.getModifiers().isStatic();
            String indentStr = generateIndent(mIndent);
            
            new CodeDisassembler(mi).disassemble
                (new CodeAssemblerPrinter(paramTypes, isStatic,
                                          mOut, indentStr, ";", "b."));
        }
    }

    private String escape(String str) {
        return CodeAssemblerPrinter.escape(str);
    }

    private void printModifiers(ClassFile cf) {
        printModifiers(cf.getModifiers());
    }

    private void printModifiers(FieldInfo fi) {
        printModifiers(fi.getModifiers());
    }

    private void printModifiers(MethodInfo mi) {
        printModifiers(mi.getModifiers());
    }

    private void printModifiers(Modifiers modifiers) {
        print("Modifiers.");

        if (modifiers.isPublic()) {
            if (modifiers.isAbstract()) {
                print("PUBLIC_ABSTRACT");
                modifiers = modifiers.toAbstract(false);
            } else if (modifiers.isStatic()) {
                print("PUBLIC_STATIC");
                modifiers = modifiers.toStatic(false);
            } else {
                print("PUBLIC");
            }
            modifiers = modifiers.toPublic(false);
        } else if (modifiers.isProtected()) {
            print("PROTECTED");
            modifiers = modifiers.toProtected(false);
        } else if (modifiers.isPrivate()) {
            print("PRIVATE");
            modifiers = modifiers.toPrivate(false);
        } else {
            print("NONE");
        }

        if (modifiers.isStatic()) {
            print(".toStatic(true)");
        }
        if (modifiers.isFinal()) {
            print(".toFinal(true)");
        }
        if (modifiers.isSynchronized()) {
            print(".toSynchronized(true)");
        }
        if (modifiers.isVolatile()) {
            print(".toVolatile(true)");
        }
        if (modifiers.isTransient()) {
            print(".toTransient(true)");
        }
        if (modifiers.isNative()) {
            print(".toNative(true)");
        }
        if (modifiers.isInterface()) {
            print(".toInterface(true)");
        }
        if (modifiers.isAbstract() && !modifiers.isInterface()) {
            print(".toAbstract(true)");
        }
        if (modifiers.isStrict()) {
            print(".toStrict(true)");
        }
    }

    private void print(TypeDesc type) {
        if (type == null || type == TypeDesc.VOID) {
            print("null");
            return;
        }

        if (type.isPrimitive()) {
            print("TypeDesc.".concat(type.getFullName().toUpperCase()));
            return;
        } else if (type == TypeDesc.OBJECT) {
            print("TypeDesc.OBJECT");
            return;
        } else if (type == TypeDesc.STRING) {
            print("TypeDesc.STRING");
            return;
        }

        TypeDesc componentType = type.getComponentType();
        if (componentType != null) {
            print(componentType);
            print(".toArrayType()");
        } else {
            print("TypeDesc.forClass(\"");
            print(escape(type.getRootName()));
            print("\")");
        }
    }

    private void print(TypeDesc[] params) {
        if (params == null || params.length == 0) {
            print("null");
            return;
        }

        print("new TypeDesc[] {");

        for (int i=0; i<params.length; i++) {
            if (i > 0) {
                print(", ");
            }
            print(params[i]);
        }

        print("}");
    }

    private void print(String text) {
        indent();
        mOut.print(text);
    }

    private void println(String text) {
        print(text);
        println();
    }

    private void println() {
        mOut.println();
        mNeedIndent = true;
    }

    private void indent() {
        if (mNeedIndent) {
            for (int i=mIndent; --i>= 0; ) {
                mOut.print(' ');
            }
            mNeedIndent = false;
        }
    }

    private String generateIndent(int amount) {
        StringBuffer buf = new StringBuffer(amount);
        for (int i=0; i<amount; i++) {
            buf.append(' ');
        }
        return buf.toString();
    }
}
