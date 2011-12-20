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

package com.flaptor.util.remote;

import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.util.AStoppableThread;
import com.flaptor.util.Execute;
import com.flaptor.util.Stoppable;

/**
 * This class imprements several convenience methods to implement a generic server.
 * A generic server is a transport (rpc) wrapper class that takes one or many objects (the
 * handlers) and exports their functionality through rpc.
 * @see com.flaptor.util.Stoppable
 */
public abstract class AServer implements Stoppable {

    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private final boolean keepRunning;
    private KeepAliveThread keepAliveThread = null;
    protected final int port;
    protected enum RunningState {NOT_STARTED, RUNNING, STOPPING, STOPPED};
    protected volatile RunningState runningState = RunningState.NOT_STARTED;

    /**
     * Default constructor.
     * Equivalent to AServer(port, false).
     * @param port the port to bind the server. Must be > 0. Ports <= 1024 will log a warning.
     * @throws IllegalArgumentException if the port is an invalid number.
     */
    protected AServer(final int port) {
        this(port, false);
    }

    /**
     * Constructor.
     * @param port the port to bind the server. Must be > 0. Ports <= 1024 will log a warning.
     * @param keepRunning tells the server if it has to start a new thread to keep the server running.
     * @throws IllegalArgumentException if the port is an invalid number.
     */
    protected AServer(final int port, boolean keepRunning) {
        if (port <=0 || port > 65536) {
            throw new IllegalArgumentException("port must be > 0 and < 65536");
        }
        if (port <= 1024) {
            logger.warn("starting a server in a well known port");
        }
        this.port = port;
        this.keepRunning = keepRunning;
    }

    /**
     * Starts the server.
     */
    synchronized public final void start() {
        startServer();
        if (keepRunning) {
            keepAliveThread = new KeepAliveThread();
            new Thread(keepAliveThread).start();
        }
        runningState = RunningState.RUNNING;
    }

    synchronized public void requestStop() {
        if (RunningState.NOT_STARTED == runningState) {
            throw new IllegalArgumentException("requestStop: cannot request stop, the server was never started.");
        }
        if (RunningState.STOPPED == runningState || RunningState.STOPPING == runningState) {
            return;
        }
        runningState = RunningState.STOPPING;
        Thread stopperThread = new Thread() {
            public void run() {
                logger.debug("running stopThread");
                logger.debug("requestStopServer");
                requestStopServer(); //This call is blocking. Important because I want to shut down the RPC
                //before shutting down the handlers.
                //Now, shut down every handler that can be shut down.
                for (Map.Entry<String, ? extends Object> entry : getHandlers().entrySet()) {
                    Object handler = entry.getValue();
                    if (!(handler instanceof Stoppable)) continue;
                    Stoppable toStop = (Stoppable) handler;
                    if (!toStop.isStopped()) {
                        logger.debug("requestStop of handler " + handler);
                        ((Stoppable)handler).requestStop();
                    }
                }
                if (null != keepAliveThread) {
                    keepAliveThread.requestStop();
                }

                //Now we wait until every handler is stopped.
                boolean allStopped = false;
                while (!allStopped) {
                    Execute.sleep(20);
                    logger.debug("sleeping - waiting for all stopped");
                    allStopped = true;
                    for (Map.Entry<String, ? extends Object> entry : getHandlers().entrySet()) {
                        Object handler = entry.getValue();
                        if (!(handler instanceof Stoppable)) continue;
                        if (!((Stoppable)handler).isStopped()) {
                            allStopped = false;
                            break;
                        }
                    }
                }
                runningState = RunningState.STOPPED;
                logger.debug("ending stopThread");
            }
        };

        stopperThread.setName(this.getClass().getName() + "-StopperThread");
        stopperThread.start();
    }

    public boolean isStopped() {
        return RunningState.STOPPED == runningState;
    }

    /**
     * Request the stopping of the RPC server
     * This method must not return until the RPC server is completely stopped.
     */
    protected abstract void requestStopServer();

    /**
     * Starts the actual server.
     */
    protected abstract void startServer();

    /**
     * Returns the handler created by the derived server.
     */
    protected abstract Map<String, ? extends Object> getHandlers();


    /**
     * A simple class used to retain a reference of the server in case the
     * main thread quits.
     */
    private class KeepAliveThread extends AStoppableThread { 
        public void run() {
            while (!signaledToStop) {
                if (AServer.this.isStopped()) {
                    requestStop();
                }
                sleep(1000);
            }
            stopped = true;
        }
    }
}
