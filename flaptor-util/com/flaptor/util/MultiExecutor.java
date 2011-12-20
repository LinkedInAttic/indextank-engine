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

import com.flaptor.util.Execution.Results;


/**
 * A threadpool of workers to execute tasks in parallel. The tasks are grouped in
 * objects of the Execution class  
 *
 * @param <T> the type of the result of the tasks
 * 
 * @author Flaptor Development Team
 */
public class MultiExecutor<T> implements Stoppable {

    protected List<Worker> workers = new ArrayList<Worker>();
    protected Queue<Execution<T>> executionQueue = new LinkedList<Execution<T>>();
    
    /**
     * Constructs a MultiExecutor 
     * 
     * @param numWorkerThreads the number of worker threads
     * @param workerNamePrefix the name prefix for worker threads (for debugging purposes)
     */
    public MultiExecutor(int numWorkerThreads, String workerNamePrefix) {
        
        for (int i = 0; i < numWorkerThreads; ++i) {
            Worker worker = new Worker(workerNamePrefix + (i+1));
            workers.add(worker);
            worker.start();
        }
    }
    /**
     * Adds an execution to the executionQueue
     */
    public void addExecution(Execution<T> e) {
        synchronized (executionQueue) {
            executionQueue.add(e);
            executionQueue.notifyAll();
        }
    }

    @Override
    public void requestStop() {
        for (Worker w : workers) {
            w.requestStop();
        }
    }

    @Override
    public boolean isStopped() {
        for (Worker w : workers) {
            if (!w.isStopped()) {
                return false;
            }
        }
        return true;
    }

    /**
     * A worker thread of the MultiExecutor. It peeks the executions from the queue. 
     * If the execution is empty it discards it, otherwise it executes the next task 
     * of that execution
     *
     * @param <T> the return type of the execution tasks
     * 
     * @author Martin Massera
     */
    private class Worker extends com.flaptor.util.AStoppableThread {
        public Worker(String threadName) {
            thrd.setName(threadName);
            thrd.setDaemon(true);
        }

        public void run() {
            while (true) {
                if (signaledToStop) {
                    stopped = true;
                    return;
                }
                Execution<T> execution = null;
                Callable<T> task = null;
                
                synchronized(executionQueue) {
                    execution = executionQueue.peek();
                    while (null == execution) {
                    	if (signaledToStop) {
                    		stopped = true;
                    		return;
                    	}
                    	try {
                    		executionQueue.wait(200);
                    	} catch (InterruptedException e) {}
                    	execution = executionQueue.peek();
                    }
                    
                    synchronized(execution) {
                        task = execution.pollTask();

                        //if it has been forgotten or been finished, remove it from the queue and notify
                        if (execution.hasFinished() || execution.isForgotten()) {
                            executionQueue.poll();
                            execution.notifyAll();
                            continue;
                        }
                        //if there ara no more tasks,
                        //it was the last task, take it out of the queue
                        if (!execution.hasTasks()) {
                            executionQueue.poll();
                        }
                    }
                }
                
                Results<T> results = new Results<T>(task);
                try {
                    results.setResults(task.call());
                    results.setFinishedOk(true);
                } catch (Throwable t) {
                    results.setException(t);
                }
                
                synchronized(execution) {
                    //dont add results to a forgotten execution
                    if (!execution.isForgotten()) {
                        execution.getResultsList().add(results);
                    }
                    //if we are done, we notify
                    if (execution.hasFinished() || execution.isForgotten()) {
                        execution.notifyAll();
                    }
                }
            }
        }
    }
}
