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

package org.cojen.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.cojen.classfile.ClassFile;

/**
 * ClassInjector allows transient classes to be loaded, where a transient class
 * is defined as being dynamically created at runtime. Unless explicit, the
 * name given to transient classes is randomly assigned to prevent name
 * collisions and to discourage referencing the classname persistently outside
 * the runtime environment.
 * <p>
 * Classes defined by ClassInjector may be unloaded, if no references to it
 * exist. Once unloaded, they cannot be loaded again by name since the
 * original bytecode was never preserved.
 * <p>
 * Debugging can be enabled via the java command-line option
 * "-Dorg.cojen.util.ClassInjector.DEBUG=true". This causes all generated classes
 * to be written to the temp directory, and a message is written to System.out
 * indicating exactly where.
 *
 * @author Brian S O'Neill
 * @deprecated use {@link org.cojen.classfile.RuntimeClassFile}
 */
@Deprecated
public class ClassInjector {
    private static final boolean DEBUG;

    static {
        DEBUG =
            Boolean.getBoolean("org.cojen.classfile.RuntimeClassFile.DEBUG") ||
            Boolean.getBoolean("org.cojen.util.ClassInjector.DEBUG") ||
            Boolean.getBoolean("cojen.util.ClassInjector.DEBUG");
    }

    private static final Random cRandom = new Random();

    // Weakly maps ClassLoaders to softly referenced internal ClassLoaders.
    private static Map cLoaders = new WeakIdentityMap();

    /**
     * Create a ClassInjector for defining one class. The parent ClassLoader
     * used is the one which loaded the ClassInjector class.
     */
    public static ClassInjector create() {
        return create(null, null);
    }

    /**
     * Create a ClassInjector for defining one class. The prefix is optional,
     * which is used as the start of the auto-generated class name. If the
     * parent ClassLoader is not specified, it will default to the ClassLoader of
     * the ClassInjector class.
     * <p>
     * If the parent loader was used for loading injected classes, the new
     * class will be loaded by it. This allows auto-generated classes access to
     * package accessible members, as long as they are defined in the same
     * package.
     *
     * @param prefix optional class name prefix
     * @param parent optional parent ClassLoader
     */
    public static ClassInjector create(String prefix, ClassLoader parent) {
        return create(prefix, parent, false);
    }

    /**
     * Create a ClassInjector for defining one class with an explicit name. If
     * the parent ClassLoader is not specified, it will default to the
     * ClassLoader of the ClassInjector class.
     * <p>
     * If the parent loader was used for loading injected classes, the new
     * class will be loaded by it. This allows auto-generated classes access to
     * package accessible members, as long as they are defined in the same
     * package.
     *
     * @param name required class name
     * @param parent optional parent ClassLoader
     * @throws IllegalArgumentException if name is null
     */
    public static ClassInjector createExplicit(String name, ClassLoader parent) {
        if (name == null) {
            throw new IllegalArgumentException("Explicit class name not provided");
        }
        return create(name, parent, true);
    }

    private static ClassInjector create(String prefix, ClassLoader parent, boolean explicit) {
        if (prefix == null) {
            prefix = ClassInjector.class.getName();
        }
        if (parent == null) {
            parent = ClassInjector.class.getClassLoader();
            if (parent == null) {
                parent = ClassLoader.getSystemClassLoader();
            }
        }

        String name = explicit ? prefix : null;
        Loader loader;

        synchronized (cRandom) {
            getLoader: {
                if (parent instanceof Loader) {
                    // Use the same loader, allowing the new class access to
                    // same package protected members.
                    loader = (Loader) parent;
                    break getLoader;
                }
                SoftReference ref = (SoftReference) cLoaders.get(parent);
                if (ref != null) {
                    loader = (Loader) ref.get();
                    if (loader != null && loader.isValid()) {
                        break getLoader;
                    }
                    ref.clear();
                }
                loader = parent == null ? new Loader() : new Loader(parent);
                cLoaders.put(parent, new SoftReference(loader));
            }

            if (explicit) {
                reserveCheck: {
                    for (int i=0; i<2; i++) {
                        if (loader.reserveName(name)) {
                            try {
                                loader.loadClass(name);
                            } catch (ClassNotFoundException e) {
                                break reserveCheck;
                            }
                        }
                        if (i > 0) {
                            throw new IllegalStateException
                                ("Class name already reserved: " + name);
                        }
                        // Make a new loader and try again.
                        loader = parent == null ? new Loader() : new Loader(parent);
                    }

                    // Save new loader.
                    cLoaders.put(parent, new SoftReference(loader));
                }
            } else {
                for (int tryCount = 0; tryCount < 1000; tryCount++) {
                    name = null;
                    
                    long ID = cRandom.nextInt();
                    
                    // Use a small identifier if possible, making it easier to read
                    // stack traces and decompiled classes.
                    switch (tryCount) {
                    case 0:
                        ID &= 0xffL;
                        break;
                    case 1:
                        ID &= 0xffffL;
                        break;
                    default:
                        ID &= 0xffffffffL;
                        break;
                    }
                    
                    name = prefix + '$' + ID;
                    
                    if (!loader.reserveName(name)) {
                        continue;
                    }
                    
                    try {
                        loader.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        break;
                    } catch (LinkageError e) {
                    }
                }
            }
        }

        if (name == null) {
            throw new InternalError("Unable to create unique class name");
        }

        return new ClassInjector(name, loader);
    }

    private final String mName;
    private final Loader mLoader;

    private ByteArrayOutputStream mData;
    private Class mClass;

    private ClassInjector(String name, Loader loader) {
        mName = name;
        mLoader = loader;
    }

    /**
     * Returns the name that must be given to the new class.
     */
    public String getClassName() {
        return mName;
    }

    /**
     * Open a stream to define the new class into.
     *
     * @throws IllegalStateException if new class has already been defined
     * or if a stream has already been opened
     */
    public OutputStream openStream() throws IllegalStateException {
        if (mClass != null) {
            throw new IllegalStateException("New class has already been defined");
        }
        ByteArrayOutputStream data = mData;
        if (data != null) {
            throw new IllegalStateException("Stream already opened");
        }
        mData = data = new ByteArrayOutputStream();
        return data;
    }

    /**
     * Define the new class from a ClassFile object.
     *
     * @return the newly created class
     * @throws IllegalStateException if new class has already been defined
     * or if a stream has already been opened
     */
    public Class defineClass(ClassFile cf) {
        try {
            cf.writeTo(openStream());
        } catch (IOException e) {
            throw new InternalError(e.toString());
        }
        return getNewClass();
    }

    /**
     * Returns the newly defined class.
     *
     * @throws IllegalStateException if class was never defined
     */
    public Class getNewClass() throws IllegalStateException, ClassFormatError {
        if (mClass != null) {
            return mClass;
        }
        ByteArrayOutputStream data = mData;
        if (data == null) {
            throw new IllegalStateException("Class not defined yet");
        }

        byte[] bytes = data.toByteArray();

        if (DEBUG) {
            File file = new File(mName.replace('.', '/') + ".class");
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                file = new File(tempDir, file.getPath());
            } catch (SecurityException e) {
            }
            try {
                file.getParentFile().mkdirs();
                System.out.println("ClassInjector writing to " + file);
                OutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mClass = mLoader.define(mName, bytes);
        mData = null;
        return mClass;
    }

    private static final class Loader extends ClassLoader {
        private Set mReservedNames = new HashSet();

        Loader(ClassLoader parent) {
            super(parent);
        }

        Loader() {
            super();
        }

        // Prevent name collisions while multiple threads are injecting classes
        // by reserving the name.
        synchronized boolean reserveName(String name) {
            return mReservedNames.add(name);
        }

        synchronized boolean isValid() {
            // Only use loader for 100 injections, to facilitate class
            // unloading.
            return mReservedNames.size() < 100;
        }

        Class define(String name, byte[] b) {
            Class clazz = defineClass(name, b, 0, b.length);
            resolveClass(clazz);
            return clazz;
        }
    }
}
