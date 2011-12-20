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

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.ClassFile;
import org.cojen.classfile.Label;
import org.cojen.classfile.MethodDesc;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;

/**
 * Generic one-shot factory which supports late object creation. If the object
 * creation results in an exception or is taking too long, the object produced
 * instead is a bogus one. After retrying, if the real object is created, then
 * the bogus object turns into a wrapper to the real object.
 *
 * <p>Note: If a bogus object is created, the wrapper cannot always be a drop-in
 * replacement for the real object. If the wrapper is cloned, it won't have the
 * same behavior as cloning the real object. Also, synchronizing on the wrapper
 * will not synchronize the real object.
 *
 * @author Brian S O'Neill
 * @since 2.1
 */
public abstract class BelatedCreator<T, E extends Exception> {
    private static final String REF_FIELD_NAME = "ref";

    private static final Map<Class<?>, Class<?>> cWrapperCache;

    private static final ExecutorService cThreadPool;

    static {
        cWrapperCache = new SoftValuedHashMap();
        cThreadPool = Executors.newCachedThreadPool(new TFactory());
    }

    private final Class<T> mType;
    final int mMinRetryDelayMillis;

    private T mReal;
    private boolean mFailed;
    private Throwable mFailedError;
    private T mBogus;
    private AtomicReference<T> mRef;

    private CreateThread mCreateThread;

    /**
     * @param type type of object created
     * @param minRetryDelayMillis minimum milliseconds to wait before retrying
     * to create object after failure; if negative, never retry
     * @throws IllegalArgumentException if type is null or is not an interface
     */
    protected BelatedCreator(Class<T> type, int minRetryDelayMillis) {
        if (type == null) {
            throw new IllegalArgumentException("Type is null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Type must be an interface: " + type);
        }
        mType = type;
        mMinRetryDelayMillis = minRetryDelayMillis;
    }

    /**
     * Returns real or bogus object. If real object is returned, then future
     * invocations of this method return the same real object instance. This
     * method waits for the real object to be created, if it is blocked. If
     * real object creation fails immediately, then this method will not wait,
     * returning a bogus object immediately instead.
     *
     * @param timeoutMillis maximum time to wait for real object before
     * returning bogus one; if negative, potentially wait forever
     * @throws E exception thrown from createReal
     */
    public synchronized T get(final int timeoutMillis) throws E {
        if (mReal != null) {
            return mReal;
        }

        if (mBogus != null && mMinRetryDelayMillis < 0) {
            return mBogus;
        }

        if (mCreateThread == null) {
            mCreateThread = new CreateThread();
            cThreadPool.submit(mCreateThread);
        }

        if (timeoutMillis != 0) {
            final long start = System.nanoTime();

            try {
                if (timeoutMillis < 0) {
                    while (mReal == null && mCreateThread != null) {
                        if (timeoutMillis < 0) {
                            wait();
                        }
                    }
                } else {
                    long remaining = timeoutMillis;
                    while (mReal == null && mCreateThread != null && !mFailed) {
                        wait(remaining);
                        long elapsed = (System.nanoTime() - start) / 1000000L;
                        if ((remaining -= elapsed) <= 0) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
            }

            if (mReal != null) {
                return mReal;
            }

            long elapsed = (System.nanoTime() - start) / 1000000L;

            if (elapsed >= timeoutMillis) {
                timedOutNotification(elapsed);
            }
        }

        if (mFailedError != null) {
            // Take ownership of error. Also stitch traces together for
            // context. This gives the illusion that the creation attempt
            // occurred in this thread.

            Throwable error = mFailedError;
            mFailedError = null;

            StackTraceElement[] trace = error.getStackTrace();
            error.fillInStackTrace();
            StackTraceElement[] localTrace = error.getStackTrace();
            StackTraceElement[] completeTrace =
                new StackTraceElement[trace.length + localTrace.length];
            System.arraycopy(trace, 0, completeTrace, 0, trace.length);
            System.arraycopy(localTrace, 0, completeTrace, trace.length, localTrace.length);
            error.setStackTrace(completeTrace);

            ThrowUnchecked.fire(error);
        }

        if (mBogus == null) {
            mRef = new AtomicReference<T>(createBogus());

            mBogus = AccessController.doPrivileged(new PrivilegedAction<T>() {
                public T run() {
                    try {
                        return getWrapper().newInstance(mRef);
                    } catch (Exception e) {
                        ThrowUnchecked.fire(e);
                        return null;
                    }
                }
            });
        }

        return mBogus;
    }

    /**
     * Create instance of real object. If there is a recoverable error creating
     * the object, return null. Any error logging must be performed by the
     * implementation of this method. If null is returned, expect this method
     * to be called again in the future.
     *
     * @return real object, or null if there was a recoverable error
     * @throws E unrecoverable error
     */
    protected abstract T createReal() throws E;

    /**
     * Create instance of bogus object.
     */
    protected abstract T createBogus();

    /**
     * Notification that createReal is taking too long. This can be used to log
     * a message.
     *
     * @param timedOutMillis milliseconds waited before giving up
     */
    protected abstract void timedOutNotification(long timedOutMillis);

    /**
     * Notification that createReal has produced the real object. The default
     * implementation does nothing.
     */
    protected void createdNotification(T object) {
    }

    synchronized void created(T object) {
        mReal = object;
        if (mBogus != null) {
            mBogus = null;
            if (mRef != null) {
                // Changing reference to real object. The ref object is also
                // held by the auto-generated wrapper, so changing here changes
                // the wrapper's behavior.
                mRef.set(object);
            }
        }
        mFailed = false;
        notifyAll();
        createdNotification(object);
    }

    synchronized void failed() {
        if (mReal == null) {
            mFailed = true;
        }
        notifyAll();
    }

    /**
     * @param error optional error to indicate thread is exiting because of this
     */
    synchronized void handleThreadExit(Throwable error) {
        if (mReal == null) {
            mFailed = true;
            if (error != null) {
                mFailedError = error;
            }
        }
        mCreateThread = null;
        notifyAll();
    }

    /**
     * Returns a Constructor that accepts an AtomicReference to the wrapped
     * object.
     */
    private Constructor<T> getWrapper() {
        Class<T> clazz;
        synchronized (cWrapperCache) {
            clazz = (Class<T>) cWrapperCache.get(mType);
            if (clazz == null) {
                clazz = createWrapper();
                cWrapperCache.put(mType, clazz);
            }
        }

        try {
            return clazz.getConstructor(AtomicReference.class);
        } catch (NoSuchMethodException e) {
            ThrowUnchecked.fire(e);
            return null;
        }
    }

    private Class<T> createWrapper() {
        RuntimeClassFile cf = new RuntimeClassFile(mType.getName());
        cf.addInterface(mType);
        cf.markSynthetic();
        cf.setSourceFile(BelatedCreator.class.getName());
        cf.setTarget("1.5");

        final TypeDesc atomicRefType = TypeDesc.forClass(AtomicReference.class);

        cf.addField(Modifiers.PRIVATE.toFinal(true), REF_FIELD_NAME, atomicRefType);

        CodeBuilder b = new CodeBuilder(cf.addConstructor(Modifiers.PUBLIC,
                                                          new TypeDesc[] {atomicRefType}));
        b.loadThis();
        b.invokeSuperConstructor(null);
        b.loadThis();
        b.loadLocal(b.getParameter(0));
        b.storeField(REF_FIELD_NAME, atomicRefType);
        b.returnVoid();

        // Now define all interface methods to call wrapped object.

        for (Method m : mType.getMethods()) {
            try {
                Object.class.getMethod(m.getName(), m.getParameterTypes());
                // Defined in object too, so skip it for now
                continue;
            } catch (NoSuchMethodException e) {
            }

            addWrappedCall(cf, new CodeBuilder(cf.addMethod(m)), m);
        }

        // Also wrap non-final public methods from Object. mType is an
        // interface, so we don't have to worry about any superclasses --
        // except Object.  We want to make sure that all (non-final) Object
        // methods delegate to the generated proxy.  For example, one would
        // expect toString to call the wrapped object, not the wrapper itself.

        for (Method m : Object.class.getMethods()) {
            int modifiers = m.getModifiers();
            if (!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)) {
                b = new CodeBuilder
                    (cf.addMethod(Modifiers.PUBLIC, m.getName(), MethodDesc.forMethod(m)));
                addWrappedCall(cf, b, m);
            }
        }

        Class<T> clazz = cf.defineClass();
        return clazz;
    }

    private void addWrappedCall(ClassFile cf, CodeBuilder b, Method m) {
        // Special behavior for equals method
        boolean isEqualsMethod = false;
        if (m.getName().equals("equals") && m.getReturnType().equals(boolean.class)) {
            Class[] paramTypes = m.getParameterTypes();
            isEqualsMethod = paramTypes.length == 1 && paramTypes[0].equals(Object.class);
        }

        if (isEqualsMethod) {
            b.loadThis();
            b.loadLocal(b.getParameter(0));
            Label notEqual = b.createLabel();
            b.ifEqualBranch(notEqual, false);
            b.loadConstant(true);
            b.returnValue(TypeDesc.BOOLEAN);

            notEqual.setLocation();

            // Check if object is our type.
            b.loadLocal(b.getParameter(0));
            b.instanceOf(cf.getType());
            Label isInstance = b.createLabel();
            b.ifZeroComparisonBranch(isInstance, "!=");

            b.loadConstant(false);
            b.returnValue(TypeDesc.BOOLEAN);

            isInstance.setLocation();
        }

        final TypeDesc atomicRefType = TypeDesc.forClass(AtomicReference.class);

        // Load wrapped object...
        b.loadThis();
        b.loadField(REF_FIELD_NAME, atomicRefType);
        b.invokeVirtual(atomicRefType, "get", TypeDesc.OBJECT, null);
        b.checkCast(TypeDesc.forClass(mType));

        // Load parameters...
        for (int i=0; i<b.getParameterCount(); i++) {
            b.loadLocal(b.getParameter(i));
        }

        // Special behavior for equals method
        if (isEqualsMethod) {
            // Extract wrapped object.
            b.checkCast(cf.getType());
            b.loadField(REF_FIELD_NAME, atomicRefType);
            b.invokeVirtual(atomicRefType, "get", TypeDesc.OBJECT, null);
            b.checkCast(TypeDesc.forClass(mType));
        }

        // Invoke wrapped method...
        b.invoke(m);

        if (m.getReturnType() == void.class) {
            b.returnVoid();
        } else {
            b.returnValue(TypeDesc.forClass(m.getReturnType()));
        }
    }

    private class CreateThread implements Runnable {
        public void run() {
            try {
                while (true) {
                    T real = createReal();
                    if (real != null) {
                        created(real);
                        break;
                    }
                    failed();
                    if (mMinRetryDelayMillis < 0) {
                        break;
                    }
                    try {
                        Thread.sleep(mMinRetryDelayMillis);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                handleThreadExit(null);
            } catch (Throwable e) {
                handleThreadExit(e);
            }
        }
    }

    private static class TFactory implements ThreadFactory {
        private static int cCount;

        private static synchronized int nextID() {
            return ++cCount;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("BelatedCreator-" + nextID());
            return t;
        }
    }
}
