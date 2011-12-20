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

import org.apache.log4j.Logger;

/**
 * Provides a mechanism to stop a thread. Subclasses must provide a run() method
 * that should exit as soon as possible after signaledToStop has been set to true.
 */
public abstract class AStoppableThread implements Runnable, Stoppable {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    protected Thread thrd = new Thread(this);
    protected volatile boolean signaledToStop = false;
    protected volatile boolean stopped = false;

    /**
     * @inheritDoc
     * @see java.lang.Runnable#run()
     */
    public abstract void run();

    /**
     * Starts the stoppable thread.
     * @see java.lang.Thread#start()
     */
    public void start() {
        thrd.start();
    }

    /**
     * @inheritDoc
     */
    public void requestStop() {
        signaledToStop = true;
    }

    /**
     * @inheritDoc
     */
    public final boolean isStopped() {
        return stopped;
    }

    /**
     * Puts the thread to sleep. Unlike the Thread.sleep method, this one throws
     * no exception, and ensures that at least the indicated amount of time has
     * passed before returning (unless signaled to stop). It will wake up every second to see if a stop 
     * has been requested.
     * @param ms the time to sleep, in milliseconds.
     */
    public void sleep(final long ms) {
        long startTime = System.currentTimeMillis();
        long elapsed = 0;
        while (elapsed < ms) {
            try {
                //never sleep for more than a second at a time, to check for a stop request
                Thread.sleep(1000 > ms - elapsed ? ms  - elapsed : 1000);
            } catch (InterruptedException e) {
                logger.error("Got awakened while sleeping.", e);
            }
            elapsed = System.currentTimeMillis() - startTime;
            if (signaledToStop) {
                logger.debug("stop requested, exiting sleep after " + elapsed + " ms instead of waiting the full " + ms);
                break;
            }
        }
    }
}
