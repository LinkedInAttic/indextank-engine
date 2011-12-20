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
package com.flaptor.util.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.Execution;
import com.flaptor.util.MultiExecutor;
import com.flaptor.util.Pair;
import com.flaptor.util.Execution.Results;

/**
 * For accessing an array of remote caches. It returns the first 
 * 
 * Its configured through multiCache.properties
 * 
 * @author Martin Massera
 */
// TODO remove duplicate code in getItem and hasItem
public class MultiCache<T extends Serializable> {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    private List<RemoteCache<T>> caches = new ArrayList<RemoteCache<T>>();
    private MultiExecutor multiCacheExecutor; //this executes things that return T as well as boolean
    private long timeout; 
    
    public MultiCache(List<Pair<String, Integer>> hosts, long timeout, int workerThreads) {
        for (Pair<String, Integer> host: hosts) {
            caches.add(new RmiCacheStub<T>(host.last(), host.first()));
        }
        this.timeout = timeout;
        multiCacheExecutor = new MultiExecutor(workerThreads, "multiCache");
    }
      
    @SuppressWarnings("unchecked")
    public T getItem(final String key) {
        Execution<T> execution= new Execution<T>();
        for (final RemoteCache<T> cache : caches) {
            execution.addTask(new Callable<T>() {
                public T call() throws Exception {
                    return cache.getItem(key);
                }
            });
        }
        multiCacheExecutor.addExecution(execution);


        long start = System.currentTimeMillis();
        long now = start;
        while(true) {
            synchronized(execution) {               
                long toWait = timeout - (now - start);
                if (toWait > 0) {
                    try {
                        execution.wait(toWait);
                    } catch (InterruptedException e) {
                        logger.warn("multicache: interrupted while waiting for the responses to return. I will continue...");
                    }
                    now = System.currentTimeMillis();
                } else {
                    logger.warn("MultiCache timeout, " + execution.getResultsList().size() + " out of " + caches.size() + " finished");
                    return null;
                }

                for (Results<T> res : execution.getResultsList()) {
                    if (res.isFinishedOk()){
                        if (res.getResults() != null) {
                            execution.forget();
                            return res.getResults();
                        }
                    } else {
                        //TODO this may print the same warning multiple times
                        logger.error("Exception in remote call to cache", res.getException());
                    }
                }
                
                if (execution.getResultsList().size() == caches.size()) { //everybody returned null or there were exceptions
                    return null; 
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean hasItem(final String key) {
        Execution<Boolean> execution= new Execution<Boolean>();
        for (final RemoteCache cache : caches) {
            execution.addTask(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return cache.hasItem(key);
                }         
            });
        }
        multiCacheExecutor.addExecution(execution);

        long start = System.currentTimeMillis();
        long now = start;
        while(true) {
            synchronized(execution) {               
                long toWait = timeout - (now - start);
                if (toWait > 0) {
                    try {
                        execution.wait(toWait);
                    } catch (InterruptedException e) {
                        logger.warn("multicache: interrupted while waiting for the responses to return. I will continue...");
                    }
                    now = System.currentTimeMillis();
                } else {
                    logger.error("MultiCache timeout, " + execution.getResultsList().size() + " out of " + caches.size() + " finished");
                    return false;
                }

                for (Results<Boolean> res : execution.getResultsList()) {
                    if (res.isFinishedOk()) {
                        if (res.getResults().equals(Boolean.TRUE)) {
                            execution.forget();
                            return res.getResults();                            
                        }
                    } else {
                        //TODO this may print the same warning multiple times
                        logger.error("Exception in remote call to cache", res.getException());
                    }
                }
                
                if (execution.getResultsList().size() == caches.size()) { //everybody returned null or there were exceptions
                    return false; 
                }
            }
        }
    }
}
