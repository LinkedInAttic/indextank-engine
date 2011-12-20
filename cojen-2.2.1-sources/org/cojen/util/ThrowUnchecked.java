/*
 *  Copyright 2007-2010 Brian S O'Neill
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

/*
 * Copyright 2006 Amazon Technologies, Inc. or its affiliates.
 * Amazon, Amazon.com and Carbonado are trademarks or registered trademarks
 * of Amazon Technologies, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cojen.util;

import java.lang.reflect.UndeclaredThrowableException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;

/**
 * Allows exceptions to be thrown which aren't declared to be thrown. Use of
 * this technique can cause confusion since it violates the Java language rules
 * for undeclared checked exceptions. For this reason, this class should not be
 * used except under special circumstances such as to work around compiler
 * bugs. An exception can be made, if calling any of the fireDeclared methods
 * and the set of declared types matches what the caller is allowed to throw.
 *
 * <p>Example:
 *
 * <pre>
 * public &lt;E extends Throwable&gt; void someMethod(E exception) throws E {
 *     ...
 *
 *     // Apparent compiler bug sometimes disallows this. Doesn't appear to
 *     // show up when compiling source files individually.
 *
 *     //throw exception;
 *
 *     // Throw it this way instead, and compiler doesn't know.
 *     ThrowUnchecked.fire(exception);
 * }
 * </pre>
 *
 * @author Brian S O'Neill
 * @since 2.1
 */
public abstract class ThrowUnchecked {
    private static volatile ThrowUnchecked cImpl;

    /**
     * Throws the given exception, even though it may be checked. This method
     * only returns normally if the exception is null.
     *
     * @param t exception to throw
     */
    public static void fire(Throwable t) {
        if (t != null) {
            // Don't need to do anything special for unchecked exceptions.
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }

            ThrowUnchecked impl = cImpl;
            if (impl == null) {
                synchronized (ThrowUnchecked.class) {
                    impl = cImpl;
                    if (impl == null) {
                        cImpl = impl =
                            AccessController.doPrivileged(new PrivilegedAction<ThrowUnchecked>() {
                                public ThrowUnchecked run() {
                                    return generateImpl();
                                }
                            });
                    }
                }
            }

            impl.doFire(t);
        }
    }

    /**
     * Throws the given exception if it is unchecked or an instance of any of
     * the given declared types. Otherwise, it is thrown as an
     * UndeclaredThrowableException. This method only returns normally if the
     * exception is null.
     *
     * @param t exception to throw
     * @param declaredTypes if exception is checked and is not an instance of
     * any of these types, then it is thrown as an
     * UndeclaredThrowableException.
     */
    public static void fireDeclared(Throwable t, Class... declaredTypes) {
        if (t != null) {
            if (declaredTypes != null) {
                for (Class declaredType : declaredTypes) {
                    if (declaredType.isInstance(t)) {
                        fire(t);
                    }
                }
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new UndeclaredThrowableException(t);
        }
    }

    /**
     * Throws the either the original exception or the first found cause if it
     * matches one of the given declared types or is unchecked. Otherwise, the
     * original exception is thrown as an UndeclaredThrowableException. This
     * method only returns normally if the exception is null.
     *
     * @param t exception whose cause is to be thrown
     * @param declaredTypes if exception is checked and is not an instance of
     * any of these types, then it is thrown as an
     * UndeclaredThrowableException.
     */
    public static void fireFirstDeclared(Throwable t, Class... declaredTypes) {
        Throwable cause = t;
        while (cause != null) {
            cause = cause.getCause();
            if (cause == null) {
                break;
            }
            if (declaredTypes != null) {
                for (Class declaredType : declaredTypes) {
                    if (declaredType.isInstance(cause)) {
                        fire(cause);
                    }
                }
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
        }
        throw new UndeclaredThrowableException(t);
    }

    /**
     * Throws the cause of the given exception, even though it may be
     * checked. If the cause is null, then the original exception is
     * thrown. This method only returns normally if the exception is null.
     *
     * @param t exception whose cause is to be thrown
     */
    public static void fireCause(Throwable t) {
        if (t != null) {
            Throwable cause = t.getCause();
            if (cause == null) {
                cause = t;
            }
            fire(cause);
        }
    }

    /**
     * Throws the cause of the given exception if it is unchecked or an
     * instance of any of the given declared types. Otherwise, it is thrown as
     * an UndeclaredThrowableException. If the cause is null, then the original
     * exception is thrown. This method only returns normally if the exception
     * is null.
     *
     * @param t exception whose cause is to be thrown
     * @param declaredTypes if exception is checked and is not an instance of
     * any of these types, then it is thrown as an
     * UndeclaredThrowableException.
     */
    public static void fireDeclaredCause(Throwable t, Class... declaredTypes) {
        if (t != null) {
            Throwable cause = t.getCause();
            if (cause == null) {
                cause = t;
            }
            fireDeclared(cause, declaredTypes);
        }
    }

    /**
     * Throws the first found cause that matches one of the given declared
     * types or is unchecked. Otherwise, the immediate cause is thrown as an
     * UndeclaredThrowableException. If the immediate cause is null, then the
     * original exception is thrown. This method only returns normally if the
     * exception is null.
     *
     * @param t exception whose cause is to be thrown
     * @param declaredTypes if exception is checked and is not an instance of
     * any of these types, then it is thrown as an
     * UndeclaredThrowableException.
     */
    public static void fireFirstDeclaredCause(Throwable t, Class... declaredTypes) {
        Throwable cause = t;
        while (cause != null) {
            cause = cause.getCause();
            if (cause == null) {
                break;
            }
            if (declaredTypes != null) {
                for (Class declaredType : declaredTypes) {
                    if (declaredType.isInstance(cause)) {
                        fire(cause);
                    }
                }
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
        }
        fireDeclaredCause(t, declaredTypes);
    }

    /**
     * Throws the root cause of the given exception, even though it may be
     * checked. If the root cause is null, then the original exception is
     * thrown. This method only returns normally if the exception is null.
     *
     * @param t exception whose root cause is to be thrown
     */
    public static void fireRootCause(Throwable t) {
        Throwable root = t;
        while (root != null) {
            Throwable cause = root.getCause();
            if (cause == null) {
                break;
            }
            root = cause;
        }
        fire(root);
    }

    /**
     * Throws the root cause of the given exception if it is unchecked or an
     * instance of any of the given declared types. Otherwise, it is thrown as
     * an UndeclaredThrowableException. If the root cause is null, then the
     * original exception is thrown. This method only returns normally if the
     * exception is null.
     *
     * @param t exception whose root cause is to be thrown
     * @param declaredTypes if exception is checked and is not an instance of
     * any of these types, then it is thrown as an
     * UndeclaredThrowableException.
     */
    public static void fireDeclaredRootCause(Throwable t, Class... declaredTypes) {
        Throwable root = t;
        while (root != null) {
            Throwable cause = root.getCause();
            if (cause == null) {
                break;
            }
            root = cause;
        }
        fireDeclared(root, declaredTypes);
    }

    private static ThrowUnchecked generateImpl() {
        RuntimeClassFile cf = new RuntimeClassFile(null, ThrowUnchecked.class.getName());
        cf.addDefaultConstructor();
        CodeBuilder b = new CodeBuilder
            (cf.addMethod(Modifiers.PROTECTED, "doFire",
                          null, new TypeDesc[] {TypeDesc.forClass(Throwable.class)}));
        b.loadLocal(b.getParameter(0));
        b.throwObject();
        try {
            return (ThrowUnchecked) cf.defineClass().newInstance();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected ThrowUnchecked() {
    }

    protected abstract void doFire(Throwable t);
}
