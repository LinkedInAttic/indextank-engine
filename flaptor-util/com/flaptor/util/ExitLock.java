package com.flaptor.util;

/**
 * A lock to prevent system from exiting in the middle of a computation.
 * 
 * Usage:
 * <code>
 * private exitLock = new ExitLock();
 * ...
 * synchronized(exitLock) {
 *     if (!exitLock.isExiting) {
 *         doSomething();
 *     }
 * }
 * </code>
 * 
 * WARN: doesnt save you from kill -9
 * 
 * @author Martin Massera
 *
 */
public class ExitLock {
    
    private boolean exiting = false;
    
    public ExitLock() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                synchronized (ExitLock.this) {
                    exiting = true;
                }
            } 
        });
    }
    
    public boolean isExiting() {
        return exiting;    
    }
}
