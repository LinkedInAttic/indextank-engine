/*
 *  Copyright 2009-2010 Brian S O'Neill
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.ProtectionDomain;

import java.security.cert.Certificate;

import org.cojen.util.KeyFactory;
import org.cojen.util.WeakValuedHashMap;

/**
 * Allows classes to be defined and loaded at runtime. A random number is
 * appended to class names to prevent name collisions and to discourage
 * referencing them persistently outside the runtime environment. This behavior
 * can be disabled by constructing with {@code explicit} set to true.
 *
 * <p>Debugging can be enabled via the java command-line option
 * "-Dorg.cojen.classfile.RuntimeClassFile.DEBUG=true". This causes all
 * generated classes to be written to the temp directory, and a message is
 * written to System.out indicating exactly where.
 *
 * @author Brian S O'Neill
 */
public class RuntimeClassFile extends ClassFile {
    private static final boolean DEBUG;

    static {
        DEBUG =
            Boolean.getBoolean("org.cojen.classfile.RuntimeClassFile.DEBUG") ||
            Boolean.getBoolean("org.cojen.util.ClassInjector.DEBUG") ||
            Boolean.getBoolean("cojen.util.ClassInjector.DEBUG");
    }

    private static final Random cRandom = new Random();

    private static Map<Object, Loader> cLoaders = new WeakValuedHashMap<Object, Loader>();

    private final Loader mLoader;

    public RuntimeClassFile() {
        this(null, null, null, null, false, null);
    }

    /**
     * @param className fully qualified class name; pass null to use default
     */
    public RuntimeClassFile(String className) {
        this(className, null, null, null, false, null);
    }

    /**
     * @param className fully qualified class name; pass null to use default
     * @param superClassName fully qualified super class name; pass null to use Object.
     */
    public RuntimeClassFile(String className, String superClassName) {
        this(className, superClassName, null, null, false, null);
    }

   /**
     * @param className fully qualified class name; pass null to use default
     * @param superClassName fully qualified super class name; pass null to use Object.
     * @param parentLoader parent class loader; pass null to use default
     */
    public RuntimeClassFile(String className, String superClassName, ClassLoader parentLoader) {
        this(className, superClassName, parentLoader, null, false, null);
    }

    /**
     * @param className fully qualified class name; pass null to use default
     * @param superClassName fully qualified super class name; pass null to use Object.
     * @param parentLoader parent class loader; pass null to use default
     * @param domain to define class in; pass null to use default
     */
    public RuntimeClassFile(String className, String superClassName,
                            ClassLoader parentLoader, ProtectionDomain domain)
    {
        this(className, superClassName, parentLoader, domain, false, null);
    }

    /**
     * @param className fully qualified class name; pass null to use default
     * @param superClassName fully qualified super class name; pass null to use Object.
     * @param parentLoader parent class loader; pass null to use default
     * @param domain to define class in; pass null to use default
     * @param explicit pass true to prevent name mangling
     */
    public RuntimeClassFile(String className, String superClassName,
                            ClassLoader parentLoader, ProtectionDomain domain,
                            boolean explicit)
    {
        this(className, superClassName, parentLoader, domain, explicit, null);
    }

    // Magic constructor to select name and loader before calling super class constructor.
    private RuntimeClassFile(String className, String superClassName,
                             ClassLoader parentLoader, ProtectionDomain domain,
                             boolean explicit, LoaderAndName loaderAndName)
    {
        super((loaderAndName = loaderAndName
               (className, parentLoader, domain, explicit)).mClassName, superClassName);
        mLoader = loaderAndName.mLoader;
    }

    /**
     * Finishes the class definition.
     */
    public Class defineClass() {
        byte[] bytes;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writeTo(bout);
            bytes = bout.toByteArray();
        } catch (IOException e) {
            InternalError ie = new InternalError(e.toString());
            ie.initCause(e);
            throw ie;
        }

        if (DEBUG) {
            File file = new File(getClassName().replace('.', '/') + ".class");
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                file = new File(tempDir, file.getPath());
            } catch (SecurityException e) {
            }
            try {
                file.getParentFile().mkdirs();
                System.out.println("RuntimeClassFile writing to " + file);
                OutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return mLoader.define(getClassName(), bytes);
    }

    private static LoaderAndName loaderAndName(String className,
                                               ClassLoader parentLoader,
                                               ProtectionDomain domain,
                                               boolean explicit)
    {
        if (className == null) {
            if (explicit) {
                throw new IllegalArgumentException("Explicit class name not provided");
            }
            className = RuntimeClassFile.class.getName();
        }

        if (parentLoader == null) {
            parentLoader = RuntimeClassFile.class.getClassLoader();
            if (parentLoader == null) {
                parentLoader = ClassLoader.getSystemClassLoader();
            }
        }

        final Object loaderKey = createLoaderKey(className, parentLoader, domain);

        Loader loader = cLoaders.get(loaderKey);
        if (loader == null) {
            loader = parentLoader == null ? new Loader(domain) : new Loader(parentLoader, domain);
            cLoaders.put(loaderKey, loader);
        }

        if (explicit) {
            if (!loader.reserveName(className, true)) {
                throw new IllegalArgumentException("Class already defined: " + className);
            }
            return new LoaderAndName(loader, className);
        }

        for (int tryCount = 0; tryCount < 1000; tryCount++) {
            long id = cRandom.nextInt();

            // Use a small identifier if possible, making it easier to read
            // stack traces and decompiled classes.
            switch (tryCount) {
            case 0:
                id &= 0xffL;
                break;
            case 1: case 2: case 3: case 4:
                id &= 0xffffL;
                break;
            default:
                id &= 0xffffffffL;
                break;
            }

            String mangled = className + '$' + id;

            if (loader.reserveName(mangled, false)) {
                return new LoaderAndName(loader, mangled);
            }
        }

        throw new InternalError("Unable to create unique class name");
    }

    private static Object createLoaderKey(String className, ClassLoader parentLoader,
                                          ProtectionDomain domain)
    {
        String packageName;
        {
            int index = className.lastIndexOf('.');
            if (index < 0) {
                packageName = "";
            } else {
                packageName = className.substring(0, index);
            }
        }

        // ProtectionDomain doesn't have an equals method, so break it apart
        // and add the elements to the composite key.

        Object domainKey = null;
        Object csKey = null;
        Object permsKey = null;
        Object principalsKey = null;

        if (domain != null) {
            domainKey = "";
            csKey = domain.getCodeSource();

            PermissionCollection pc = domain.getPermissions();
            if (pc != null) {
                List<Permission> permList = Collections.list(pc.elements());
                if (permList.size() == 1) {
                    permsKey = permList.get(0);
                } else if (permList.size() > 1) {
                    permsKey = new HashSet<Permission>(permList);
                }
            }

            Principal[] principals = domain.getPrincipals();
            if (principals != null && principals.length > 0) {
                if (principals.length == 1) {
                    principalsKey = principals[0];
                } else {
                    Set<Principal> principalSet = new HashSet<Principal>(principals.length);
                    for (Principal principal : principals) {
                        principalSet.add(principal);
                    }
                    principalsKey = principalSet;
                }
            }
        }

        return KeyFactory.createKey(new Object[] {
            parentLoader, packageName, domainKey, csKey, permsKey, principalsKey
        });
    }

    private static final class Loader extends ClassLoader {
        private final Map<String, Boolean> mReservedNames = new WeakHashMap<String, Boolean>();
        private final ProtectionDomain mDomain;

        Loader(ClassLoader parent, ProtectionDomain domain) {
            super(parent);
            mDomain = prepareDomain(domain, this);
        }

        Loader(ProtectionDomain domain) {
            super();
            mDomain = prepareDomain(domain, this);
        }

        private static ProtectionDomain prepareDomain(ProtectionDomain domain,
                                                      ClassLoader loader)
        {
            if (domain == null) {
                return null;
            }

            return new ProtectionDomain(domain.getCodeSource(),
                                        domain.getPermissions(),
                                        loader,
                                        domain.getPrincipals());
        }

        // Prevent name collisions while multiple threads are defining classes
        // by reserving the name.
        boolean reserveName(String name, boolean explicit) {
            synchronized (mReservedNames) {
                if (mReservedNames.put(name, Boolean.TRUE) != null && !explicit) {
                    return false;
                }
            }

            // If explicit and name has already been reserved, don't
            // immediately return false. This allows the class to be defined if
            // an earlier RuntimeClassFile instance was abandoned. A duplicate
            // class definition can still be attempted later, which is
            // converted to an IllegalStateException by the define method.

            try {
                loadClass(name);
            } catch (ClassNotFoundException e) {
                return true;
            } catch (LinkageError e) {
                // Class by same name exists, but it is broken.
            }

            return false;
        }

        Class define(String name, byte[] b) {
            try {
                Class clazz;
                if (mDomain == null) {
                    clazz = defineClass(name, b, 0, b.length);
                } else {
                    clazz = defineClass(name, b, 0, b.length, mDomain);
                }
                resolveClass(clazz);
                return clazz;
            } catch (LinkageError e) {
                // Replace duplicate name definition with a better exception.
                try {
                    loadClass(name);
                    throw new IllegalStateException("Class already defined: " + name);
                } catch (ClassNotFoundException e2) {
                }
                throw e;
            } finally {
                mReservedNames.remove(name);
            }
        }
    }

    private static final class LoaderAndName {
        final Loader mLoader;
        final String mClassName;

        LoaderAndName(Loader loader, String className) {
            mLoader = loader;
            mClassName = className;
        }
    }
}
