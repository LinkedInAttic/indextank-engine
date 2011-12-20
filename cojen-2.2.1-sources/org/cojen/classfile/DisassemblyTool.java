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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cojen.classfile.attribute.CodeAttr;
import org.cojen.classfile.attribute.SignatureAttr;

/**
 * Disassembles a class file, sending the results to standard out. The class
 * can be specified by name or by a file name. If the class is specified by
 * name, it must be available in the classpath.
 * <p>
 * Two output formats are supported: assembly and builder. The assembly format
 * is the default, and it produces a pseudo Java source file, where the method
 * bodies contain JVM assembly code.
 * <p>
 * The builder format produces a valid Java file, which uses the Cojen
 * classfile API. When compiled and run, it rebuilds the original class and
 * inner classes. This format makes it easier to understand how to use the
 * classfile API to generate new classes.
 *
 * @author Brian S O'Neill
 */
public class DisassemblyTool {
    /**
     * Disassembles a class file, sending the results to standard out.
     *
     * <pre>
     * DisassemblyTool [-f &lt;format style&gt;] &lt;file or class name&gt;
     * </pre>
     * 
     * The format style may be "assembly" (the default) or "builder".
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("DisassemblyTool [-f <format style>] <file or class name>");
            System.out.println();
            System.out.println("The format style may be \"assembly\" (the default) or \"builder\"");
            return;
        }

        String style;
        String name;

        if ("-f".equals(args[0])) {
            style = args[1];
            name = args[2];
        } else {
            style = "assembly";
            name = args[0];
        }

        ClassFileDataLoader loader;
        InputStream in;

        try {
            final File file = new File(name);
            in = new FileInputStream(file);
            loader = new ClassFileDataLoader() {
                public InputStream getClassData(String name)
                    throws IOException
                {
                    name = name.substring(name.lastIndexOf('.') + 1);
                    File f = new File(file.getParentFile(), name + ".class");

                    if (f.exists()) {
                        return new FileInputStream(f);
                    }

                    return null;
                }
            };
        } catch (FileNotFoundException e) {
            if (name.endsWith(".class")) {
                System.err.println(e);
                return;
            }

            loader = new ResourceClassFileDataLoader();
            in = loader.getClassData(name);

            if (in == null) {
                System.err.println(e);
                return;
            }
        }

        in = new BufferedInputStream(in);
        ClassFile cf = ClassFile.readFrom(in, loader, null);

        PrintWriter out = new PrintWriter(System.out);

        Printer p;
        if (style == null || style.equals("assembly")) {
            p = new AssemblyStylePrinter();
        } else if (style.equals("builder")) {
            p = new BuilderStylePrinter();
        } else {
            System.err.println("Unknown format style: " + style);
            return;
        }

        p.disassemble(cf, out);

        out.flush();
    }

    public static interface Printer {
        void disassemble(ClassFile cf, PrintWriter out);
    }
}
