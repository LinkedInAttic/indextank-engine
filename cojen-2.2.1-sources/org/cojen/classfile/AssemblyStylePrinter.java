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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cojen.util.IntHashMap;
import org.cojen.classfile.attribute.Annotation;
import org.cojen.classfile.attribute.CodeAttr;
import org.cojen.classfile.attribute.LocalVariableTableAttr;
import org.cojen.classfile.attribute.SignatureAttr;
import org.cojen.classfile.attribute.StackMapTableAttr;
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
 * Disassembles a ClassFile into a pseudo Java assembly language format.
 *
 * @author Brian S O'Neill
 */
class AssemblyStylePrinter implements DisassemblyTool.Printer {
    private ClassFile mClassFile;
    private ConstantPool mCp;
    private PrintWriter mOut;

    private byte[] mByteCodes;
    // Current address being decompiled.
    private int mAddress;

    // Maps int address keys to String labels.
    private IntHashMap<Object> mLabels;

    private ExceptionHandler[] mExceptionHandlers;

    // Maps int catch locations to Lists of ExceptionHandler objects.
    private IntHashMap<List<ExceptionHandler>> mCatchLocations;

    public AssemblyStylePrinter() {
    }

    public void disassemble(ClassFile cf, PrintWriter out) {
        disassemble(cf, out, "");
    }

    private void disassemble(ClassFile cf, PrintWriter out, String indent) {
        mClassFile = cf;
        mCp = cf.getConstantPool();
        mOut = out;

        if (indent.length() == 0 || mClassFile.getSourceFile() != null ||
            mClassFile.isDeprecated() || mClassFile.isSynthetic()) {

            println(indent, "/**");

            boolean addBreak = false;

            if (indent.length() == 0) {
                print(indent, " * Disassembled on ");
                print(new Date());
                println(".");
                addBreak = true;
            }

            if (indent.length() == 0 && mClassFile.getTarget() != null) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                print(indent, " * @target ");
                println(CodeAssemblerPrinter.escape(mClassFile.getTarget()));
            }

            if (mClassFile.getSourceFile() != null) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                print(indent, " * @source ");
                println(CodeAssemblerPrinter.escape(mClassFile.getSourceFile()));
            }

            if (mClassFile.isInnerClass()) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                if (mClassFile.getInnerClassName() == null) {
                    println(indent, " * @anonymous");
                } else {
                    print(indent, " * @name ");
                    println(CodeAssemblerPrinter.escape(mClassFile.getInnerClassName()));
                }
            }

            if (mClassFile.isDeprecated()) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                println(indent, " * @deprecated");
            }

            if (mClassFile.isSynthetic()) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                println(indent, " * @synthetic");
            }

            // TODO: Just testing
            SignatureAttr sig = mClassFile.getSignatureAttr();
            if (sig != null) {
                if (addBreak) {
                    println(indent, " * ");
                    addBreak = false;
                }
                println(indent, " * @signature " + sig.getSignature().getValue());
            }

            println(indent, " */");
        }

        disassemble(indent, mClassFile.getRuntimeVisibleAnnotations());
        disassemble(indent, mClassFile.getRuntimeInvisibleAnnotations());

        print(indent);

        disassemble(mClassFile.getModifiers());
        boolean isInterface = mClassFile.getModifiers().isInterface();
        if (!isInterface) {
            print("class ");
        }
        print(mClassFile.getClassName());

        if (mClassFile.getSuperClassName() != null) {
            print(" extends ");
            print(mClassFile.getSuperClassName());
        }

        String innerIndent = indent + "    ";

        String[] interfaces = mClassFile.getInterfaces();
        if (interfaces.length == 0) {
            println(" {");
        } else {
            println();
            for (int i=0; i<interfaces.length; i++) {
                if (i == 0) {
                    print(innerIndent, "implements ");
                } else {
                    println(",");
                    print(innerIndent, "           ");
                }
                print(interfaces[i]);
            }
            println();
            println(indent, "{");
        }

        FieldInfo[] fields = mClassFile.getFields();
        MethodInfo[] methods = mClassFile.getMethods();
        MethodInfo[] ctors = mClassFile.getConstructors();
        MethodInfo init = mClassFile.getInitializer();

        Object[] members = new Object[fields.length + methods.length +
                                     ctors.length + ((init == null) ? 0 : 1)];

        int m = 0;

        for (int i=0; i<fields.length; i++) {
            members[m++] = fields[i];
        }

        for (int i=0; i<methods.length; i++) {
            members[m++] = methods[i];
        }

        for (int i=0; i<ctors.length; i++) {
            members[m++] = ctors[i];
        }

        if (init != null) {
            members[m++] = init;
        }

        sortMembers(members);

        for (int i=0; i<members.length; i++) {
            if (i > 0) {
                println();
            }
            if (members[i] instanceof FieldInfo) {
                disassemble(innerIndent, (FieldInfo)members[i]);
            } else {
                disassemble(innerIndent, (MethodInfo)members[i]);
            }
        }

        mByteCodes = null;
        mLabels = null;
        mExceptionHandlers = null;
        mCatchLocations = null;

        ClassFile[] innerClasses = mClassFile.getInnerClasses();

        for (int i=0; i<innerClasses.length; i++) {
            if (i > 0 || members.length > 0) {
                println();
            }
            AssemblyStylePrinter printer = new AssemblyStylePrinter();
            printer.disassemble(innerClasses[i], mOut, innerIndent);
        }

        println(indent, "}");

        mOut.flush();
        mOut = null;
    }

    private void disassemble(String indent, FieldInfo field) {
        SignatureAttr sig = field.getSignatureAttr();
        if (field.isDeprecated() || field.isSynthetic() || sig != null) {
            println(indent, "/**");
            if (field.isDeprecated()) {
                println(indent, " * @deprecated");
            }
            if (field.isSynthetic()) {
                println(indent, " * @synthetic");
            }
            if (sig != null) {
                println(indent, " * @signature " + sig.getSignature().getValue());
            }
            println(indent, " */");
        }

        disassemble(indent, field.getRuntimeVisibleAnnotations());
        disassemble(indent, field.getRuntimeInvisibleAnnotations());

        print(indent);
        disassemble(field.getModifiers());
        disassemble(field.getType());
        print(" ");
        print(field.getName());
        ConstantInfo constant = field.getConstantValue();
        if (constant != null) {
            print(" = ");
            disassemble(constant);
        }
        println(";");
    }

    private void disassemble(String indent, MethodInfo method) {
        SignatureAttr sig = method.getSignatureAttr();
        if (method.isDeprecated() || method.isSynthetic() || sig != null) {
            println(indent, "/**");
            if (method.isDeprecated()) {
                println(indent, " * @deprecated");
            }
            if (method.isSynthetic()) {
                println(indent, " * @synthetic");
            }
            if (sig != null) {
                println(indent, " * @signature " + sig.getSignature().getValue());
            }
            println(indent, " */");
        }

        disassemble(indent, method.getRuntimeVisibleAnnotations());
        disassemble(indent, method.getRuntimeInvisibleAnnotations());

        print(indent);

        MethodDesc md = method.getMethodDescriptor();

        if ("<clinit>".equals(method.getName()) &&
            md.getReturnType() == TypeDesc.VOID &&
            md.getParameterCount() == 0 &&
            (method.getModifiers().isStatic()) &&
            (!method.getModifiers().isAbstract()) &&
            method.getExceptions().length == 0) {

            // Static initializer.
            print("static");
        } else {
            Modifiers modifiers = method.getModifiers();
            boolean varargs = modifiers.isVarArgs();
            if (varargs) {
                // Don't display the modifier.
                modifiers = modifiers.toVarArgs(false);
            }
            disassemble(modifiers);
            print(md.toMethodSignature(method.getName(), varargs));
        }

        CodeAttr code = method.getCodeAttr();

        TypeDesc[] exceptions = method.getExceptions();
        if (exceptions.length == 0) {
            if (code == null) {
                println(";");
            } else {
                println(" {");
            }
        } else {
            println();
            for (int i=0; i<exceptions.length; i++) {
                if (i == 0) {
                    print(indent + "    ", "throws ");
                } else {
                    println(",");
                    print(indent + "    ", "       ");
                }
                print(exceptions[i].getFullName());
            }
            if (code == null) {
                println(";");
            } else {
                println();
                println(indent, "{");
            }
        }

        if (code != null) {
            disassemble(indent + "    ", code);
            println(indent, "}");
        }
    }

    private void disassemble(Modifiers modifiers) {
        print(modifiers);
        if (modifiers.getBitmask() != 0) {
            print(" ");
        }
    }

    private void disassemble(ConstantInfo constant) {
        disassemble(constant, false);
    }

    private void disassemble(ConstantInfo constant, boolean showClassLiteral) {
        if (constant instanceof ConstantStringInfo) {
            print("\"");
            String value = ((ConstantStringInfo)constant).getValue();
            print(CodeAssemblerPrinter.escape(value));
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
        } else if (constant instanceof ConstantClassInfo) {
            ConstantClassInfo cci = (ConstantClassInfo)constant;
            disassemble(cci.getType());
            if (showClassLiteral) {
                print(".class");
            }
        } else if (constant instanceof ConstantUTFInfo) {
            print("\"");
            String value = ((ConstantUTFInfo)constant).getValue();
            print(CodeAssemblerPrinter.escape(value));
            print("\"");
        } else {
            print(constant);
        }
    }

    private void disassemble(TypeDesc type) {
        print(type.getFullName());
    }

    private void disassemble(LocalVariable var) {
        if (var != null) {
            print(" // ");
            print(var.getName());
            print(": ");
            disassemble(var.getType());
        }
    }

    private void disassemble(String indent, Annotation[] annotations) {
        for (int i=0; i<annotations.length; i++) {
            print(indent);
            disassemble(indent, annotations[i]);
            println();
        }
    }

    private void disassemble(String indent, Annotation ann) {
        print("@");
        print(ann.getType().getFullName());
        Map mvMap = ann.getMemberValues();
        if (mvMap.size() == 0) {
            return;
        }
        print("(");
        Iterator it = mvMap.entrySet().iterator();
        int ordinal = 0;
        while (it.hasNext()) {
            if (ordinal++ > 0) {
                print(", ");
            }
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            if (!"value".equals(name)) {
                print(name);
                print("=");
            }
            disassemble(indent, (Annotation.MemberValue)entry.getValue());
        }
        print(")");
    }

    private void disassemble(String indent, Annotation.MemberValue mv) {
        Object value = mv.getValue();
        switch (mv.getTag()) {
        default:
            print("?");
            break;
        case Annotation.MEMBER_TAG_BOOLEAN:
            ConstantIntegerInfo ci = (ConstantIntegerInfo)value;
            print(ci.getValue() == 0 ? "false" : "true");
            break;
        case Annotation.MEMBER_TAG_BYTE:
        case Annotation.MEMBER_TAG_SHORT:
        case Annotation.MEMBER_TAG_INT:
        case Annotation.MEMBER_TAG_LONG:
        case Annotation.MEMBER_TAG_FLOAT:
        case Annotation.MEMBER_TAG_DOUBLE:
        case Annotation.MEMBER_TAG_STRING:
        case Annotation.MEMBER_TAG_CLASS:
            disassemble((ConstantInfo)value, true);
            break;
        case Annotation.MEMBER_TAG_CHAR: {
            print("'");
            char c = (char) ((ConstantIntegerInfo)value).getValue();
            print(CodeAssemblerPrinter.escape(String.valueOf(c), true));
            print("'");
            break;
        }
        case Annotation.MEMBER_TAG_ENUM:
            Annotation.EnumConstValue ecv = (Annotation.EnumConstValue)value;
            print(TypeDesc.forDescriptor(ecv.getTypeName().getValue()).getFullName());
            print(".");
            print(ecv.getConstName().getValue());
            break;
        case Annotation.MEMBER_TAG_ANNOTATION:
            disassemble(indent, (Annotation)value);
            break;
        case Annotation.MEMBER_TAG_ARRAY:
            Annotation.MemberValue[] mvs = (Annotation.MemberValue[])value;

            String originalIndent = indent;
            boolean multiLine = false;
            if (mvs.length > 0) {
                if (mvs.length > 4 ||
                    (mvs.length > 1 && mvs[0].getTag() == Annotation.MEMBER_TAG_ENUM) ||
                    mvs[0].getTag() == Annotation.MEMBER_TAG_ARRAY ||
                    mvs[0].getTag() == Annotation.MEMBER_TAG_ANNOTATION) {

                    multiLine = true;
                    indent = indent + "    ";
                }
            }

            if (multiLine || mvs.length != 1) {
                print("{");
                if (multiLine) {
                    println();
                }
            }
            
            for (int j=0; j<mvs.length; j++) {
                if (multiLine) {
                    print(indent);
                }
                disassemble(indent, mvs[j]);
                if (j + 1 < mvs.length) {
                    print(",");
                    if (!multiLine) {
                        print(" ");
                    }
                }
                if (multiLine) {
                    println();
                }
            }
            
            indent = originalIndent;

            if (multiLine || mvs.length != 1) {
                if (multiLine) {
                    print(indent);
                }
                print("}");
            }
            
            break;
        }
    }

    private void disassemble(String indent, CodeAttr code) {
        CodeBuffer buffer = code.getCodeBuffer();
        mExceptionHandlers = buffer.getExceptionHandlers();
        print(indent);
        print("// max stack:  ");
        println(String.valueOf(buffer.getMaxStackDepth()));
        print(indent);
        print("// max locals: ");
        println(String.valueOf(buffer.getMaxLocals()));

        mByteCodes = buffer.getByteCodes();

        gatherLabels();

        Location currentLoc = new Location() {
            public int getLocation() {
                return mAddress;
            }

            public int compareTo(Location other) {
                if (this == other) {
                    return 0;
                }
                
                int loca = getLocation();
                int locb = other.getLocation();
                
                if (loca < locb) {
                    return -1;
                } else if (loca > locb) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

        StackMapTableAttr.StackMapFrame frame;
        if (code.getStackMapTable() == null) {
            frame = null;
        } else {
            frame = code.getStackMapTable().getInitialFrame();
        }

        int currentLine = -1;

        for (mAddress = 0; mAddress < mByteCodes.length; mAddress++) {
            int nextLine = code.getLineNumber(currentLoc);
            if (nextLine != currentLine) {
                if ((currentLine = nextLine) >= 0) {
                    println(indent, "// line " + currentLine);
                }
            }

            // Check if a label needs to be created and/or located.
            locateLabel(indent);

            frame = stackMap(indent, frame);

            byte opcode = mByteCodes[mAddress];

            String mnemonic;
            try {
                mnemonic = Opcode.getMnemonic(opcode);
            } catch (IllegalArgumentException e) {
                mnemonic = String.valueOf(opcode & 0xff);
            }

            print(indent, mnemonic);
            
            switch (opcode) {
                
            default:
                break;

                // Opcodes with no operands...

            case Opcode.NOP:
            case Opcode.BREAKPOINT:
            case Opcode.ACONST_NULL:
            case Opcode.ICONST_M1:
            case Opcode.ICONST_0:
            case Opcode.ICONST_1:
            case Opcode.ICONST_2:
            case Opcode.ICONST_3:
            case Opcode.ICONST_4:
            case Opcode.ICONST_5:
            case Opcode.LCONST_0:
            case Opcode.LCONST_1:
            case Opcode.FCONST_0:
            case Opcode.FCONST_1:
            case Opcode.FCONST_2:
            case Opcode.DCONST_0:
            case Opcode.DCONST_1:
            case Opcode.POP:
            case Opcode.POP2:
            case Opcode.DUP:
            case Opcode.DUP_X1:
            case Opcode.DUP_X2:
            case Opcode.DUP2:
            case Opcode.DUP2_X1:
            case Opcode.DUP2_X2:
            case Opcode.SWAP:
            case Opcode.IADD:  case Opcode.LADD: 
            case Opcode.FADD:  case Opcode.DADD:
            case Opcode.ISUB:  case Opcode.LSUB:
            case Opcode.FSUB:  case Opcode.DSUB:
            case Opcode.IMUL:  case Opcode.LMUL:
            case Opcode.FMUL:  case Opcode.DMUL:
            case Opcode.IDIV:  case Opcode.LDIV:
            case Opcode.FDIV:  case Opcode.DDIV:
            case Opcode.IREM:  case Opcode.LREM:
            case Opcode.FREM:  case Opcode.DREM:
            case Opcode.INEG:  case Opcode.LNEG:
            case Opcode.FNEG:  case Opcode.DNEG:
            case Opcode.ISHL:  case Opcode.LSHL:
            case Opcode.ISHR:  case Opcode.LSHR:
            case Opcode.IUSHR: case Opcode.LUSHR:
            case Opcode.IAND:  case Opcode.LAND:
            case Opcode.IOR:   case Opcode.LOR:
            case Opcode.IXOR:  case Opcode.LXOR:
            case Opcode.FCMPL: case Opcode.DCMPL:
            case Opcode.FCMPG: case Opcode.DCMPG:
            case Opcode.LCMP: 
            case Opcode.I2L:
            case Opcode.I2F:
            case Opcode.I2D:
            case Opcode.L2I:
            case Opcode.L2F:
            case Opcode.L2D:
            case Opcode.F2I:
            case Opcode.F2L:
            case Opcode.F2D:
            case Opcode.D2I:
            case Opcode.D2L:
            case Opcode.D2F:
            case Opcode.I2B:
            case Opcode.I2C:
            case Opcode.I2S:
            case Opcode.IRETURN:
            case Opcode.LRETURN:
            case Opcode.FRETURN:
            case Opcode.DRETURN:
            case Opcode.ARETURN:
            case Opcode.RETURN:
            case Opcode.IALOAD:
            case Opcode.LALOAD:
            case Opcode.FALOAD:
            case Opcode.DALOAD:
            case Opcode.AALOAD:
            case Opcode.BALOAD:
            case Opcode.CALOAD:
            case Opcode.SALOAD:
            case Opcode.IASTORE:
            case Opcode.LASTORE:
            case Opcode.FASTORE:
            case Opcode.DASTORE:
            case Opcode.AASTORE:
            case Opcode.BASTORE:
            case Opcode.CASTORE:
            case Opcode.SASTORE:
            case Opcode.ARRAYLENGTH:
            case Opcode.ATHROW:
            case Opcode.MONITORENTER:
            case Opcode.MONITOREXIT:
                println();
                continue;

            case Opcode.ILOAD_0:
            case Opcode.LLOAD_0:
            case Opcode.FLOAD_0:
            case Opcode.DLOAD_0:
            case Opcode.ALOAD_0:
                disassemble(code.getLocalVariable(mAddress, 0));
                println();
                continue;

            case Opcode.ISTORE_0:
            case Opcode.LSTORE_0:
            case Opcode.FSTORE_0:
            case Opcode.DSTORE_0:
            case Opcode.ASTORE_0:
                disassemble(code.getLocalVariable(mAddress + 1, 0));
                println();
                continue;

            case Opcode.ILOAD_1:
            case Opcode.LLOAD_1:
            case Opcode.FLOAD_1:
            case Opcode.DLOAD_1:
            case Opcode.ALOAD_1:
                disassemble(code.getLocalVariable(mAddress, 1));
                println();
                continue;

            case Opcode.ISTORE_1:
            case Opcode.LSTORE_1:
            case Opcode.FSTORE_1:
            case Opcode.DSTORE_1:
            case Opcode.ASTORE_1:
                disassemble(code.getLocalVariable(mAddress + 1, 1));
                println();
                continue;

            case Opcode.ILOAD_2:
            case Opcode.LLOAD_2:
            case Opcode.FLOAD_2:
            case Opcode.DLOAD_2:
            case Opcode.ALOAD_2:
                disassemble(code.getLocalVariable(mAddress, 2));
                println();
                continue;

            case Opcode.ISTORE_2:
            case Opcode.LSTORE_2:
            case Opcode.FSTORE_2:
            case Opcode.DSTORE_2:
            case Opcode.ASTORE_2:
                disassemble(code.getLocalVariable(mAddress + 1, 2));
                println();
                continue;

            case Opcode.ILOAD_3:
            case Opcode.LLOAD_3:
            case Opcode.FLOAD_3:
            case Opcode.DLOAD_3:
            case Opcode.ALOAD_3:
                disassemble(code.getLocalVariable(mAddress, 3));
                println();
                continue;

            case Opcode.ISTORE_3:
            case Opcode.LSTORE_3:
            case Opcode.FSTORE_3:
            case Opcode.DSTORE_3:
            case Opcode.ASTORE_3:
                disassemble(code.getLocalVariable(mAddress + 1, 3));
                println();
                continue;

                // End opcodes with no operands.
            }

            // Space to separate operands.
            print(" ");

            int index;
            ConstantInfo constant;

            switch (opcode) {
            default:
                break;

                // Opcodes that load a constant from the constant pool...
                
            case Opcode.LDC:
            case Opcode.LDC_W:
            case Opcode.LDC2_W:
                switch (opcode) {
                case Opcode.LDC:
                    index = readUnsignedByte();
                    break;
                case Opcode.LDC_W:
                case Opcode.LDC2_W:
                    index = readUnsignedShort();
                    break;
                default:
                    index = 0;
                    break;
                }

                disassemble(getConstant(index), true);
                break;

            case Opcode.NEW:
            case Opcode.ANEWARRAY:
            case Opcode.CHECKCAST:
            case Opcode.INSTANCEOF:
                constant = getConstant(readUnsignedShort());
                if (constant instanceof ConstantClassInfo) {
                    disassemble(constant);
                } else {
                    print(constant);
                }
                break;
            case Opcode.MULTIANEWARRAY:
                constant = getConstant(readUnsignedShort());
                int dims = readUnsignedByte();
                if (constant instanceof ConstantClassInfo) {
                    disassemble(constant);
                } else {
                    print(constant);
                }
                print(" ");
                print(String.valueOf(dims));
                break;

            case Opcode.GETSTATIC:
            case Opcode.PUTSTATIC:
            case Opcode.GETFIELD:
            case Opcode.PUTFIELD:
                constant = getConstant(readUnsignedShort());
                if (constant instanceof ConstantFieldInfo) {
                    ConstantFieldInfo field = (ConstantFieldInfo)constant;
                    Descriptor type = field.getNameAndType().getType();
                    if (type instanceof TypeDesc) {
                        disassemble((TypeDesc)type);
                    } else {
                        print(type);
                    }
                    print(" ");
                    print(field.getParentClass().getType().getFullName());
                    print(".");
                    print(field.getNameAndType().getName());
                } else {
                    print(constant);
                }
                break;

            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKESTATIC:
            case Opcode.INVOKEINTERFACE:
            case Opcode.INVOKEDYNAMIC:
                constant = getConstant(readUnsignedShort());

                String className;
                ConstantNameAndTypeInfo nameAndType;

                if (opcode == Opcode.INVOKEINTERFACE) {
                    // Read and ignore nargs and padding byte.
                    readShort();
                    if (!(constant instanceof ConstantInterfaceMethodInfo)) {
                        print(constant);
                        break;
                    }
                    ConstantInterfaceMethodInfo method = 
                        (ConstantInterfaceMethodInfo)constant;
                    className =
                        method.getParentClass().getType().getFullName();
                    nameAndType = method.getNameAndType();
                } else if (opcode == Opcode.INVOKEDYNAMIC) {
                    // Read and ignore extra bytes.
                    readShort();
                    className = null;
                    nameAndType = (ConstantNameAndTypeInfo)constant;
                } else {
                    if (!(constant instanceof ConstantMethodInfo)) {
                        print(constant);
                        break;
                    }
                    ConstantMethodInfo method = (ConstantMethodInfo)constant;
                    className =
                        method.getParentClass().getType().getFullName();
                    nameAndType = method.getNameAndType();
                }

                Descriptor type = nameAndType.getType();
                if (!(type instanceof MethodDesc)) {
                    print(type);
                    break;
                }
                disassemble(((MethodDesc)type).getReturnType());
                print(" ");
                if (className != null) {
                    print(className);
                    print(".");
                }
                print(nameAndType.getName());

                print("(");
                TypeDesc[] params = ((MethodDesc)type).getParameterTypes();
                for (int i=0; i<params.length; i++) {
                    if (i > 0) {
                        print(", ");
                    }
                    disassemble(params[i]);
                }
                print(")");
                break;

                // End opcodes that load a constant from the constant pool.

                // Opcodes that load or store local variables...

            case Opcode.ILOAD:
            case Opcode.LLOAD:
            case Opcode.FLOAD:
            case Opcode.DLOAD:
            case Opcode.ALOAD:
            case Opcode.RET:
                int varNum = readUnsignedByte();
                print(String.valueOf(varNum));
                disassemble(code.getLocalVariable(mAddress, varNum));
                break;
            case Opcode.ISTORE:
            case Opcode.LSTORE:
            case Opcode.FSTORE:
            case Opcode.DSTORE:
            case Opcode.ASTORE:
                varNum = readUnsignedByte();
                print(String.valueOf(varNum));
                disassemble(code.getLocalVariable(mAddress + 1, varNum));
                break;
            case Opcode.IINC:
                print(String.valueOf(varNum = readUnsignedByte()));
                print(" ");
                int incValue = readByte();
                if (incValue >= 0) {
                    print("+");
                }
                print(String.valueOf(incValue));
                disassemble(code.getLocalVariable(mAddress, varNum));
                break;

                // End opcodes that load or store local variables.

                // Opcodes that branch to another address.
            case Opcode.GOTO:
            case Opcode.JSR:
            case Opcode.IFNULL:
            case Opcode.IFNONNULL:
            case Opcode.IF_ACMPEQ:
            case Opcode.IF_ACMPNE:
            case Opcode.IFEQ:
            case Opcode.IFNE:
            case Opcode.IFLT:
            case Opcode.IFGE:
            case Opcode.IFGT:
            case Opcode.IFLE:
            case Opcode.IF_ICMPEQ:
            case Opcode.IF_ICMPNE:
            case Opcode.IF_ICMPLT:
            case Opcode.IF_ICMPGE:
            case Opcode.IF_ICMPGT:
            case Opcode.IF_ICMPLE:
                print(getLabel(mAddress + readShort()));
                break;
            case Opcode.GOTO_W:
            case Opcode.JSR_W:
                print(getLabel(mAddress + readInt()));
                break;

                // End opcodes that branch to another address.

                // Miscellaneous opcodes...
            case Opcode.BIPUSH:
                int value = readByte();
                print(String.valueOf(value));
                printCharLiteral(value);
                break;
            case Opcode.SIPUSH:
                value = readShort();
                print(String.valueOf(value));
                printCharLiteral(value);
                break;

            case Opcode.NEWARRAY:
                int atype = readByte();
                switch (atype) {
                case 4: // T_BOOLEAN
                    print("boolean");
                    break;
                case 5: // T_CHAR
                    print("char");
                    break;
                case 6: // T_FLOAT
                    print("float");
                    break;
                case 7: // T_DOUBLE
                    print("double");
                    break;
                case 8: // T_BYTE
                    print("byte");
                    break;
                case 9: // T_SHORT
                    print("short");
                    break;
                case 10: // T_INT
                    print("int");
                    break;
                case 11: // T_LONG
                    print("long");
                    break;
                default:
                    print("T_" + atype);
                    break;
                }
                break;

            case Opcode.TABLESWITCH:
            case Opcode.LOOKUPSWITCH:
                int opcodeAddress = mAddress;
                // Read padding until address is 32 bit word aligned.
                while (((mAddress + 1) & 3) != 0) {
                    ++mAddress;
                }
                String defaultLocation = getLabel(opcodeAddress + readInt());
                int[] cases;
                String[] locations;
                
                if (opcode == Opcode.TABLESWITCH) {
                    int lowValue = readInt();
                    int highValue = readInt();
                    int caseCount = highValue - lowValue + 1;
                    print("// " + caseCount + " cases");
                    try {
                        cases = new int[caseCount];
                    } catch (NegativeArraySizeException e) {
                        break;
                    }
                    locations = new String[caseCount];
                    for (int i=0; i<caseCount; i++) {
                        cases[i] = lowValue + i;
                        locations[i] = getLabel(opcodeAddress + readInt());
                    }
                } else {
                    int caseCount = readInt();
                    print("// " + caseCount + " cases");
                    try {
                        cases = new int[caseCount];
                    } catch (NegativeArraySizeException e) {
                        break;
                    }
                    locations = new String[caseCount];
                    for (int i=0; i<caseCount; i++) {
                        cases[i] = readInt();
                        locations[i] = getLabel(opcodeAddress + readInt());
                    }
                }

                println();

                print(indent, "    default: goto ");
                println(defaultLocation);

                String prefix = indent + "    " + "case ";
                for (int i=0; i<cases.length; i++) {
                    print(prefix + cases[i]);
                    print(": goto ");
                    print(locations[i]);
                    printCharLiteral(cases[i]);
                    println();
                }

                break;

            case Opcode.WIDE:
                opcode = mByteCodes[++mAddress];
                print(Opcode.getMnemonic(opcode));
                print(" ");

                switch (opcode) {

                default:
                    break;

                case Opcode.ILOAD: case Opcode.ISTORE:
                case Opcode.LLOAD: case Opcode.LSTORE:
                case Opcode.FLOAD: case Opcode.FSTORE:
                case Opcode.DLOAD: case Opcode.DSTORE:
                case Opcode.ALOAD: case Opcode.ASTORE:
                case Opcode.RET:
                    print(String.valueOf(readUnsignedShort()));
                    break;
                case Opcode.IINC:
                    print(String.valueOf(readUnsignedShort()));
                    print(" ");
                    incValue = readShort();
                    if (incValue >= 0) {
                        print("+");
                    }
                    print(String.valueOf(incValue));
                    break;
                }

                break;
            } // end huge switch

            println();
        } // end for loop
    }

    private void gatherLabels() {
        mLabels = new IntHashMap<Object>();
        mCatchLocations = new IntHashMap<List<ExceptionHandler>>
            (mExceptionHandlers.length * 2 + 1);

        // Gather labels for any exception handlers.
        for (int i = mExceptionHandlers.length - 1; i >= 0; i--) {
            ExceptionHandler handler = mExceptionHandlers[i];
            createLabel(new Integer(handler.getStartLocation().getLocation()));
            createLabel(new Integer(handler.getEndLocation().getLocation()));
            int labelKey = handler.getCatchLocation().getLocation();
            createLabel(labelKey);
            List<ExceptionHandler> list = mCatchLocations.get(labelKey);
            if (list == null) {
                list = new ArrayList<ExceptionHandler>(2);
                mCatchLocations.put(labelKey, list);
            }
            list.add(handler);
        }

        // Now gather labels that occur within byte code.
        for (mAddress = 0; mAddress < mByteCodes.length; mAddress++) {
            byte opcode = mByteCodes[mAddress];

            switch (opcode) {

            default:
                break;

                // Opcodes that use labels.

            case Opcode.GOTO:
            case Opcode.JSR:
            case Opcode.IFNULL:
            case Opcode.IFNONNULL:
            case Opcode.IF_ACMPEQ:
            case Opcode.IF_ACMPNE:
            case Opcode.IFEQ:
            case Opcode.IFNE:
            case Opcode.IFLT:
            case Opcode.IFGE:
            case Opcode.IFGT:
            case Opcode.IFLE:
            case Opcode.IF_ICMPEQ:
            case Opcode.IF_ICMPNE:
            case Opcode.IF_ICMPLT:
            case Opcode.IF_ICMPGE:
            case Opcode.IF_ICMPGT:
            case Opcode.IF_ICMPLE:
                createLabel(new Integer(mAddress + readShort()));
                break;

            case Opcode.GOTO_W:
            case Opcode.JSR_W:
                createLabel(new Integer(mAddress + readInt()));
                break;

            case Opcode.TABLESWITCH:
            case Opcode.LOOKUPSWITCH:
                int opcodeAddress = mAddress;
                // Read padding until address is 32 bit word aligned.
                while (((mAddress + 1) & 3) != 0) {
                    ++mAddress;
                }
                
                // Read the default location.
                

                createLabel(new Integer(opcodeAddress + readInt()));
                
                if (opcode == Opcode.TABLESWITCH) {
                    int lowValue = readInt();
                    int highValue = readInt();
                    int caseCount = highValue - lowValue + 1;

                    for (int i=0; i<caseCount; i++) {
                        // Read the branch location.
                        createLabel(new Integer(opcodeAddress + readInt()));
                    }
                } else {
                    int caseCount = readInt();

                    for (int i=0; i<caseCount; i++) {
                        // Skip the case value.
                        mAddress += 4;
                        // Read the branch location.
                        createLabel(new Integer(opcodeAddress + readInt()));
                    }
                }
                break;

                // All other operations are skipped. The amount to skip
                // depends on the operand size.

                // Opcodes with no operands...

            case Opcode.NOP:
            case Opcode.BREAKPOINT:
            case Opcode.ACONST_NULL:
            case Opcode.ICONST_M1:
            case Opcode.ICONST_0:
            case Opcode.ICONST_1:
            case Opcode.ICONST_2:
            case Opcode.ICONST_3:
            case Opcode.ICONST_4:
            case Opcode.ICONST_5:
            case Opcode.LCONST_0:
            case Opcode.LCONST_1:
            case Opcode.FCONST_0:
            case Opcode.FCONST_1:
            case Opcode.FCONST_2:
            case Opcode.DCONST_0:
            case Opcode.DCONST_1:
            case Opcode.POP:
            case Opcode.POP2:
            case Opcode.DUP:
            case Opcode.DUP_X1:
            case Opcode.DUP_X2:
            case Opcode.DUP2:
            case Opcode.DUP2_X1:
            case Opcode.DUP2_X2:
            case Opcode.SWAP:
            case Opcode.IADD:  case Opcode.LADD: 
            case Opcode.FADD:  case Opcode.DADD:
            case Opcode.ISUB:  case Opcode.LSUB:
            case Opcode.FSUB:  case Opcode.DSUB:
            case Opcode.IMUL:  case Opcode.LMUL:
            case Opcode.FMUL:  case Opcode.DMUL:
            case Opcode.IDIV:  case Opcode.LDIV:
            case Opcode.FDIV:  case Opcode.DDIV:
            case Opcode.IREM:  case Opcode.LREM:
            case Opcode.FREM:  case Opcode.DREM:
            case Opcode.INEG:  case Opcode.LNEG:
            case Opcode.FNEG:  case Opcode.DNEG:
            case Opcode.ISHL:  case Opcode.LSHL:
            case Opcode.ISHR:  case Opcode.LSHR:
            case Opcode.IUSHR: case Opcode.LUSHR:
            case Opcode.IAND:  case Opcode.LAND:
            case Opcode.IOR:   case Opcode.LOR:
            case Opcode.IXOR:  case Opcode.LXOR:
            case Opcode.FCMPL: case Opcode.DCMPL:
            case Opcode.FCMPG: case Opcode.DCMPG:
            case Opcode.LCMP: 
            case Opcode.I2L:
            case Opcode.I2F:
            case Opcode.I2D:
            case Opcode.L2I:
            case Opcode.L2F:
            case Opcode.L2D:
            case Opcode.F2I:
            case Opcode.F2L:
            case Opcode.F2D:
            case Opcode.D2I:
            case Opcode.D2L:
            case Opcode.D2F:
            case Opcode.I2B:
            case Opcode.I2C:
            case Opcode.I2S:
            case Opcode.IRETURN:
            case Opcode.LRETURN:
            case Opcode.FRETURN:
            case Opcode.DRETURN:
            case Opcode.ARETURN:
            case Opcode.RETURN:
            case Opcode.IALOAD:
            case Opcode.LALOAD:
            case Opcode.FALOAD:
            case Opcode.DALOAD:
            case Opcode.AALOAD:
            case Opcode.BALOAD:
            case Opcode.CALOAD:
            case Opcode.SALOAD:
            case Opcode.IASTORE:
            case Opcode.LASTORE:
            case Opcode.FASTORE:
            case Opcode.DASTORE:
            case Opcode.AASTORE:
            case Opcode.BASTORE:
            case Opcode.CASTORE:
            case Opcode.SASTORE:
            case Opcode.ARRAYLENGTH:
            case Opcode.ATHROW:
            case Opcode.MONITORENTER:
            case Opcode.MONITOREXIT:
            case Opcode.ILOAD_0: case Opcode.ISTORE_0:
            case Opcode.ILOAD_1: case Opcode.ISTORE_1:
            case Opcode.ILOAD_2: case Opcode.ISTORE_2:
            case Opcode.ILOAD_3: case Opcode.ISTORE_3:
            case Opcode.LLOAD_0: case Opcode.LSTORE_0:
            case Opcode.LLOAD_1: case Opcode.LSTORE_1:
            case Opcode.LLOAD_2: case Opcode.LSTORE_2:
            case Opcode.LLOAD_3: case Opcode.LSTORE_3:
            case Opcode.FLOAD_0: case Opcode.FSTORE_0:
            case Opcode.FLOAD_1: case Opcode.FSTORE_1:
            case Opcode.FLOAD_2: case Opcode.FSTORE_2:
            case Opcode.FLOAD_3: case Opcode.FSTORE_3:
            case Opcode.DLOAD_0: case Opcode.DSTORE_0:
            case Opcode.DLOAD_1: case Opcode.DSTORE_1:
            case Opcode.DLOAD_2: case Opcode.DSTORE_2:
            case Opcode.DLOAD_3: case Opcode.DSTORE_3:
            case Opcode.ALOAD_0: case Opcode.ASTORE_0:
            case Opcode.ALOAD_1: case Opcode.ASTORE_1:
            case Opcode.ALOAD_2: case Opcode.ASTORE_2:
            case Opcode.ALOAD_3: case Opcode.ASTORE_3:
                break;

                // Opcodes with one operand byte...

            case Opcode.LDC:
            case Opcode.ILOAD: case Opcode.ISTORE:
            case Opcode.LLOAD: case Opcode.LSTORE:
            case Opcode.FLOAD: case Opcode.FSTORE:
            case Opcode.DLOAD: case Opcode.DSTORE:
            case Opcode.ALOAD: case Opcode.ASTORE:
            case Opcode.RET:
            case Opcode.IINC:
            case Opcode.BIPUSH:
            case Opcode.NEWARRAY:
                mAddress += 1;
                break;

                // Opcodes with two operand bytes...

            case Opcode.LDC_W:
            case Opcode.LDC2_W:
            case Opcode.NEW:
            case Opcode.ANEWARRAY:
            case Opcode.CHECKCAST:
            case Opcode.INSTANCEOF:
            case Opcode.GETSTATIC:
            case Opcode.PUTSTATIC:
            case Opcode.GETFIELD:
            case Opcode.PUTFIELD:
            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKESTATIC:
            case Opcode.SIPUSH:
                mAddress += 2;
                break;

                // Opcodes with three operand bytes...

            case Opcode.MULTIANEWARRAY:
                mAddress += 3;
                break;

                // Opcodes with four operand bytes...

            case Opcode.INVOKEINTERFACE:
            case Opcode.INVOKEDYNAMIC:
                mAddress += 4;
                break;

                // Wide opcode has a variable sized operand.

            case Opcode.WIDE:
                opcode = mByteCodes[++mAddress];
                mAddress += 2;
                if (opcode == Opcode.IINC) {
                    mAddress += 2;
                }
                break;
            } // end huge switch
        } // end for loop

        Integer[] keys = new Integer[mLabels.size()];
        mLabels.keySet().toArray(keys);
        Arrays.sort(keys);
        for (int i=0; i<keys.length; i++) {
            mLabels.put(keys[i], "L" + (i + 1) + '_' + keys[i]);
        }
    }

    private void createLabel(int labelKey) {
        mLabels.put(labelKey, (Object) labelKey);
    }

    private ConstantInfo getConstant(int index) {
        try {
            return mCp.getConstant(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private String getLabel(int address) {
        Object label = mLabels.get(new Integer(address));
        if (label == null || (!(label instanceof String))) {
            return "L?_" + address;
        } else {
            return (String)label;
        }
    }

    private void locateLabel(String indent) {
        int labelKey = mAddress;
        Object labelValue = mLabels.get(labelKey);

        if (labelValue == null) {
            return;
        }

        int len = indent.length() - 4;
        if (len > 0) {
            print(indent.substring(0, len));
        }

        print(labelValue);
        println(":");

        List<ExceptionHandler> handlers = mCatchLocations.get(labelKey);

        if (handlers != null) {
            for (int i=0; i<handlers.size(); i++) {
                ExceptionHandler handler = handlers.get(i);
                print(indent, "try (");
                print(getLabel(handler.getStartLocation().getLocation()));
                print("..");
                print(getLabel(handler.getEndLocation().getLocation()));
                print(") catch (");
                if (handler.getCatchType() == null) {
                    print("...");
                } else {
                    disassemble(handler.getCatchType());
                }
                println(")");
            }
        }
    }

    private StackMapTableAttr.StackMapFrame stackMap(String indent,
                                                     StackMapTableAttr.StackMapFrame frame)
    {
        if (frame == null) {
            return null;
        }

        if (mAddress < frame.getOffset()) {
            return frame;
        }

        print(indent);
        print("// stack:  ");
        print(frame.getStackItemInfos());
        println();
        print(indent);
        print("// locals: ");
        print(frame.getLocalInfos());
        println();

        return frame.getNext();
    }

    private void print(StackMapTableAttr.VerificationTypeInfo[] infos) {
        print('{');
        int num = 0;
        for (int i=0; i<infos.length; i++) {
            if (i > 0) {
                print(", ");
            }
            print(num);
            print('=');
            StackMapTableAttr.VerificationTypeInfo info = infos[i];
            print(info.toString());
            if (info.getType() == null || !info.getType().isDoubleWord()) {
                num += 1;
            } else {
                num += 2;
            }
        }
        print('}');
    }

    private int readByte() {
        return mByteCodes[++mAddress];
    }

    private int readUnsignedByte() {
        return mByteCodes[++mAddress] & 0xff;
    }

    private int readShort() {
        return (mByteCodes[++mAddress] << 8) | (mByteCodes[++mAddress] & 0xff);
    }

    private int readUnsignedShort() {
        return 
            ((mByteCodes[++mAddress] & 0xff) << 8) | 
            ((mByteCodes[++mAddress] & 0xff) << 0);
    }

    private int readInt() {
        return
            (mByteCodes[++mAddress] << 24) | 
            ((mByteCodes[++mAddress] & 0xff) << 16) |
            ((mByteCodes[++mAddress] & 0xff) << 8) |
            ((mByteCodes[++mAddress] & 0xff) << 0);
    }

    private void print(Object text) {
        mOut.print(text);
    }

    private void println(Object text) {
        mOut.println(text);
    }

    private void print(String indent, Object text) {
        mOut.print(indent);
        mOut.print(text);
    }

    private void println(String indent, Object text) {
        mOut.print(indent);
        mOut.println(text);
    }

    private void println() {
        mOut.println();
    }

    private void printCharLiteral(int value) {
        if (value >= 0 && value <= 65535) {
            int type = Character.getType((char)value);
            switch (type) {
            case Character.UPPERCASE_LETTER:
            case Character.LOWERCASE_LETTER:
            case Character.TITLECASE_LETTER:
            case Character.MODIFIER_LETTER:
            case Character.OTHER_LETTER:
            case Character.NON_SPACING_MARK:
            case Character.ENCLOSING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.DECIMAL_DIGIT_NUMBER:
            case Character.LETTER_NUMBER:
            case Character.OTHER_NUMBER:
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.CONNECTOR_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.MATH_SYMBOL:
            case Character.CURRENCY_SYMBOL:
            case Character.MODIFIER_SYMBOL:
            case Character.OTHER_SYMBOL:
                print(" // '");
                print(String.valueOf((char)value));
                print("'");
            }
        }
    }

    private void sortMembers(Object[] members) {
        Arrays.sort(members, new MemberComparator());
    }

    /**
     * Orders members in this canonical sequence:
     *
     * - statics
     *   - fields
     *   - initializer
     *   - methods
     * - non-statics
     *   - fields
     *   - constructors
     *   - methods
     *
     * Fields, constructors, and methods are sorted:
     * - public
     *   - final
     *   - non-final
     *   - transient
     * - protected
     *   - final
     *   - non-final
     *   - transient
     * - package
     *   - final
     *   - non-final
     *   - transient
     * - private
     *   - final
     *   - non-final
     *   - transient
     */
    private class MemberComparator implements Comparator<Object> {
        public int compare(Object a, Object b) {
            Modifiers aFlags, bFlags;

            if (a instanceof FieldInfo) {
                aFlags = ((FieldInfo)a).getModifiers();
            } else {
                aFlags = ((MethodInfo)a).getModifiers();
            }

            if (b instanceof FieldInfo) {
                bFlags = ((FieldInfo)b).getModifiers();
            } else {
                bFlags = ((MethodInfo)b).getModifiers();
            }

            // static before non-static
            if (aFlags.isStatic()) {
                if (!bFlags.isStatic()) {
                    return -1;
                }
            } else {
                if (bFlags.isStatic()) {
                    return 1;
                }
            }

            // fields before methods
            if (a instanceof FieldInfo) {
                if (b instanceof MethodInfo) {
                    return -1;
                }
            } else {
                if (!(b instanceof MethodInfo)) {
                    return 1;
                }

                // initializers and constructors before regular methods
                String aName = ((MethodInfo)a).getName();
                String bName = ((MethodInfo)b).getName();
                
                if ("<init>".equals(aName) || "<clinit>".equals(aName)) {
                    if ("<init>".equals(bName) || "<clinit>".equals(bName)) {
                    } else {
                        return -1;
                    }
                } else {
                    if ("<init>".equals(bName) || "<clinit>".equals(bName)) {
                        return 1;
                    }
                }
            }

            // public, protected, package, private order
            int aValue, bValue;
            if (aFlags.isPublic()) {
                aValue = 0;
            } else if (aFlags.isProtected()) {
                aValue = 4;
            } else if (!aFlags.isPrivate()) {
                aValue = 8;
            } else {
                aValue = 12;
            }

            if (bFlags.isPublic()) {
                bValue = 0;
            } else if (bFlags.isProtected()) {
                bValue = 4;
            } else if (!bFlags.isPrivate()) {
                bValue = 8;
            } else {
                bValue = 12;
            }

            // final before non-final
            aValue += (aFlags.isFinal()) ? 0 : 2;
            bValue += (bFlags.isFinal()) ? 0 : 2;

            // transient after non-transient
            aValue += (aFlags.isTransient()) ? 1 : 0;
            bValue += (bFlags.isTransient()) ? 1 : 0;

            if (aValue < bValue) {
                return -1;
            } else if (aValue > bValue) {
                return 1;
            }

            return 0;
        }
    }
}
