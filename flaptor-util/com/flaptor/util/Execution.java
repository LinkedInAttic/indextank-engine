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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

/**
 * Represents the execution of a group of tasks. Maintains a list of 
 * tasks to be executed (executionQueue) and a list of
 * results from tasks already executed (resultsList).
 * 
 * If the task of the execution are no longer meant to be executed, the 
 * execution can be "forgotten" 
 * 
 * @param <T> the type of the results
 * 
 * @author Martin Massera
 */
public class Execution<T> {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private boolean started = false;
    private int popped = 0;
    
    protected Queue<Callable<T>> executionQueue = new LinkedList<Callable<T>>();
    protected List<Results<T>> resultsList = new ArrayList<Results<T>>();
    protected boolean forgotten = false;

    /**
     * Empty constructor
     */
    public Execution() {}
    
    /**
     * shortcut. Construct with one task (more can be added later)
     * @param task
     */
    public Execution(Callable<T> task) {
        addTask(task);
    }
    
    /**
     * shortcut. Construct with an array of tasks (more can be added later)
     * @param tasks
     */
    public Execution(Callable<T>[] tasks) {
        for (Callable<T> task:tasks) {
            addTask(task);
        }
    }
    
    public List<Results<T>> getResultsList() {
        return resultsList;
    }

    /**
     * No more tasks will be executed and the execution will be removed
     * of the execution list
     */
    public void forget() {
        forgotten = true;
    }

    public boolean isForgotten() {
        return forgotten;
    }

    public boolean hasFinished() {
        synchronized (executionQueue) {
            return (executionQueue.size() == 0) && popped == resultsList.size();
        }
    }

    public void addTask(Callable<T> task) {
        synchronized (executionQueue) {
            if (started) throw new IllegalStateException("already started, cannot add more tasks");
            if (isForgotten()) throw new IllegalStateException("forgotten, cannot add more tasks");
            executionQueue.add(task);
        }
    }
    
    public boolean hasTasks() {
        synchronized (executionQueue) {
            return executionQueue.size() > 0;
        }
    }
    
    public Callable<T> pollTask() {
        synchronized (executionQueue) {
            started = true;
            if (executionQueue.size() > 0) {
                popped++;
                return executionQueue.poll();
            } else return null;
        }
    }

    /**
     * blocking wait, no timeout nor exceptions
     */
    public void waitFor() {
        try {
            waitFor(-1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); //shouldnt happen
        }
    }
    
    /**
     * blocking wait that returns when all tasks have finished/aborted
     * @param timeout timeout in millis (-1 for no timeout)
     * @throws InterruptedException if reaches timeout
     */
    public void waitFor(long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        long now = start;
        while(true) {
            synchronized(this) {
                if (forgotten) break;
                if (hasFinished()) break;
                now = System.currentTimeMillis();
                if (timeout > 0) {
                    long toWait = timeout - (now - start);
                    if (toWait > 0) {
                        try {
                            this.wait(toWait);
                        } catch (InterruptedException e) {
                            logger.warn("interrupted while waiting, ignoring " + e);
                        }
                    } else {
                        throw new InterruptedException("timeout");
                    }
                } else {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        logger.warn("interrupted while waiting, ignoring " + e);
                    }
                }
            }
        }

    }
    
    
    /**
     * @return a list of the tasks that threw exceptions and the corresponding exception
     */
    public List<Pair<Callable<T>,Throwable>> getProblems() {
        List<Pair<Callable<T>,Throwable>> errors = new ArrayList<Pair<Callable<T>,Throwable>>();
        for (Results<T> result : resultsList) {
            if (!result.isFinishedOk()) {
                errors.add(new Pair<Callable<T>, Throwable>(
                    result.getTask(), 
                    result.getException()));
            }
        }
        return errors;
    }
    
    /**
     * represents the outcome of the execution of a task, which can 
     * finish ok or not (with exceptions). It holds the results or 
     * the exception thrown
     * 
     * @param <T> the type of the result
     */
    public static class Results<T> {
        private Callable<T> task;
        private boolean finishedOk = false;
        private T results = null;
        private Throwable exception = null;
        
        public Results(Callable<T> task) {
            super();
            this.task = task;
        }

        public Callable<T> getTask() {
            return task;
        }
                
        public boolean isFinishedOk() {
            return finishedOk;
        }
        public void setFinishedOk(boolean finishedOk) {
            this.finishedOk = finishedOk;
        }
        public Throwable getException() {
            return exception;
        }
        public void setException(Throwable exception) {
            this.exception = exception;
        }
        public T getResults() {
            return results;
        }
        public void setResults(T results) {
            this.results = results;
        }
    }
}
