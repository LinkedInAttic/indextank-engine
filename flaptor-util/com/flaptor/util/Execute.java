/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/

package com.flaptor.util;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class provides best-practice execution patterns to some common commands.
 */
public final class Execute {

    /**
     * Hacky class to obtain the class context (this holds the classes of the
     * current call stack)
     */
    private final static class ClassContextManager extends SecurityManager {
        @Override
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
    
    private static ClassContextManager classContextManager = new ClassContextManager();

    //so that it cannot be instantiated
    private Execute() {}

    private static final Logger defaultLogger = Logger.getLogger(Execute.whoAmI());

    /**
     * Configures Log4j
     */
    public static void configureLog4j() {
        String log4jConfigPath = FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath ) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            defaultLogger.warn("log4j.properties not found on classpath! Logging configuration will not be reloaded.");
        }
    }
    
    /**
     * Executes the method close with no parameters on the object received. If
     * the object is null, it does nothing (should this be logged?). Any
     * exception thrown by the method invocation is logged as a warning (what
     * else can we do?).
     * 
     * Logs are logged by the default logger.
     * 
     * @param o the object to be closed
	 * @todo use Closeable
     */
    public static void close(Object o) {
        close(o, defaultLogger);
    }

    /**
     * Executes the method close with no parameters on the object received. If
     * the object is null, it does nothing (should this be logged?). Any
     * exception thrown by the method invocation is logged as a warning (what
     * else can we do?).
     * 
     * It allows the caller to specify a different logger.
     * 
     * @param o the object to be closed
     * @param logger logger to use
	 * @todo close discards null objects, see if this requires a warning
     */
    public static void close(Object o, Logger logger) {
        // discards null objects (exception thrown in object creation?)
        if (o == null) {
            return;
        }
        Class<?> c = o.getClass();
        Method m = null;
        try {
            m = c.getMethod("close", new Class[0]);
        } catch (Exception e) {
            logger.error("Received object (of class " + c.getName()
                    + ") doesn't implement close() method", e);
            return;
        }
        try {
            m.invoke(o, new Object[0]);
        } catch (IllegalAccessException e) {
            //ignore this. It's probably caused by an input stream from a file inside
            //a jar.
        } catch (Exception e) {
            logger.warn("Error ocurred while closing object (of class "
                    + c.getName() + ")", e);
            return;
        }
    }
    
    /**
     * This method returns the fully qualified name of the class where it is invoked.
     * The string returned is the same as the one returned by getClass().getName() on an
     * instantiated object.
     * It works even when invoked from a static code.
     * Its main use is to identify the class using a log4j logger.
     *
     * This implementation's performance is not very good.
     *
     * The intended use is:
     *
     * private static final Logger logger = Logger.getLogger(Execute.whoAmI());
     */
    public static String whoAmI() {
        return myCallersClass().getName();
    }
    /**
     * 
     * @return the name of the method that calls this one
     */
    public static String methodName() {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    /**
     * @return the actual loaded Class of the invoker of this method.
     */
    public static Class<?> myClass() {
        return classContextManager.getClassContext()[2];
    }
    
    /**
     * Returns the unqualified name of the invoking class.
     */
    public static String whatIsMyName() {
        String name = whoCalledMe();
        return name.substring(name.lastIndexOf(".")+1);
    }

    /**
     * This method returns the fully qualified name of the class that invoked the caller.
     * The string returned is the same as the one returned by getClass().getName() on an
     * instantiated object.
     * 
     * WARNING: in some cases, the caller of a method may be an "$access" method, this case
     * has been noted to happen with inner classes and may happen in other cases not noted,
     * if your code depends on this make sure this additional method does not show up in your
     * execution stack. If you need to intentionally skip such a method or search the execution
     * stack your self you can use {@link Execute#getClassContext()}. 
     */
    public static String whoCalledMe() {
        return myCallersClass(2).getName();
    }

    /**
     * @return the actual loaded Class of the invoker of the invoker of this method.
     * 
     * WARNING: in some cases, the caller of a method may be an "$access" method, this case
     * has been noted to happen with inner classes and may happen in other cases not noted,
     * if your code depends on this make sure this additional method does not show up in your
     * execution stack. If you need to intentionally skip such a method or search the execution
     * stack your self you can use {@link Execute#getClassContext()}. 
     */
    public static Class<?> myCallersClass() {
        return classContextManager.getClassContext()[3];
    }
    
    /**
     * @param depth the depth for which we want the class, 0 would mean this method
     * invoker, 1 it's invoker and so on.
     * 
     * @return the actual loaded Class of the element in the execution stack
     * that is depth steps above the invoker of this method.
     * 
     * WARNING: in some cases, the caller of a method may be an "$access" method, this case
     * has been noted to happen with inner classes and may happen in other cases not noted,
     * if your code depends on this make sure this additional method does not show up in your
     * execution stack. If you need to intentionally skip such a method or search the execution
     * stack your self you can use {@link Execute#getClassContext()}. 
     */
    public static Class<?> myCallersClass(int depth) {
        return classContextManager.getClassContext()[2 + depth];
    }

    /**
     * @return the context of classes in the current execution stack.
     * Note that the element at [1] will be Execute.class and the element
     * at [2] will be the invoker of this method and so on
     */
    public static Class<?>[] getClassContext() {
        return classContextManager.getClassContext();
    }
    
    // Auxiliary object to synchronize a static method.
    private static byte[] synchObj = new byte[1];

    /**
     * This method prints a stack trace.
     */
    public static void printStackTrace() {
        synchronized (synchObj) {
            int level = 0;
            System.out.println("Stack Trace:");
            for (StackTraceElement e : new Throwable().getStackTrace()) {
                if (level++>0) {
                    System.out.println("  ["+Thread.currentThread().getName()+"] "+e);
                }
            }
        }
    }


    /**
     * This method puts the invoker thread to sleep.
     * This implementation wraps Thread.sleep() in a try catch exception, logging the 
     * InterruptedException in the default logger.
     * @param millis the time in milliseconds to sleep
     */
    public static void sleep(long millis) {
        Execute.sleep(millis, defaultLogger);
    }

    /**
     * This method puts the invoker thread to sleep.
     * This implementation wraps Thread.sleep() in a try catch exception, logging the 
     * InterruptedException in the default logger.
     * @param millis the time in milliseconds to sleep
     * @param logger it logs the occurrence of an InterruptedException
     */
    public static void sleep(long millis, Logger logger) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("Interrupted ", e);
        }
    }

    /**
     * requests stop and waits til it actually stops
     * @param s
     */
    public static void stop(Stoppable s) {
        s.requestStop();
        while(!s.isStopped()) {
            Execute.sleep(100);
        }
    }
    
    /**
     * Executes a task synchronously and if it takes longer than the specified timeout
     * it gets interrupted and a TimeoutException is thrown.
     * 
     * @param <T> the return type of the task
     * @param callable the task to be executed
     * @param timeout the maximum time in which the task should be completed
     * @param unit the time unit of the given timeout.
     * @return
     * @throws ExecutionException if the task throws an exception
     * @throws TimeoutException if the task doesn't complete before the timeout is reached
     * @throws InterruptedException if this thread gets interrupted while waiting for the task to complete.
     */
    public static <T> T executeWithTimeout(final Callable<T> callable, long timeout, TimeUnit unit) throws ExecutionException, TimeoutException, InterruptedException {
        return executeWithTimeout(callable, timeout, unit, "timeout");
    }
    
    /**
     * Executes a task synchronously and if it takes longer than the specified timeout
     * it gets interrupted and a TimeoutException is thrown.
     * 
     * @param <T> the return type of the task
     * @param callable the task to be executed
     * @param timeout the maximum time in which the task should be completed
     * @param unit the time unit of the given timeout.
     * @param taskName the name of the task, the task will be executed in a thread with the same name as the
     * current thread with "+taskName" appended to it.
     * @return
     * @throws ExecutionException if the task throws an exception
     * @throws TimeoutException if the task doesn't complete before the timeout is reached
     * @throws InterruptedException if this thread gets interrupted while waiting for the task to complete.
     */
    public static <T> T executeWithTimeout(final Callable<T> callable, long timeout, TimeUnit unit, String taskName) throws ExecutionException, TimeoutException, InterruptedException {
        final String newThreadName = Thread.currentThread().getName() + "+" + taskName;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Thread.currentThread().setName(newThreadName);
                return callable.call();
            }
        });
        executor.shutdown();
        
        boolean completed;
        try {
            completed = executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            // cancel the execution and propagate the interruption
            future.cancel(true);
            executor.shutdownNow();
            throw e;
        }
        if (completed) {
            return future.get();
        } else {
            future.cancel(true);
            executor.shutdownNow();
            throw new TimeoutException("Timed out");
        }
    }
 
    /**
     * Checks whether the given throwable is of the given type and if that's the case, it casts it
     * and throws it.
     * 
     * @param <T> the type to be checked
     * @param throwableType a runtime instance of the type to be checked
     * @param t the throwable to check
     * @throws T if the given throwable was an instance of the given type
     */
    public static <T extends Throwable> void checkAndThrow(Class<T> throwableType, Throwable t) throws T {
        if (throwableType.isInstance(t)) {
            throw throwableType.cast(t);
        }
    }
    
    /**
     * throws this exception if it is runtime or error, and throws a runtimeException
     * if it is an Exception
     * @param t
     */
    public static void runtimeOrError(Throwable t, Logger logger) {
        if (t instanceof RuntimeException) {
        	if (logger != null) logger.error("Unexpected RuntimeException", t);
        	throw (RuntimeException) t;
        } else if (t instanceof Error) {
        	if (logger != null) logger.error("Unexpected Error", t);
        	throw (Error) t;
        } else {
        	if (logger != null) logger.error("unexpected Exception, should be Runtime or Error", t);
        	throw new RuntimeException("unexpected Exception, should be Runtime or Error",t);
        }
    }
}

