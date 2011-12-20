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

import com.flaptor.util.Execute;
import org.apache.log4j.Logger;

/**
 * Implements a retry policy that, after a fail, waits before reconnecting
 * in exponentially increasing intervals, up to a maximum of 4 seconds.
 *
 */
public class ExponentialFallbackPolicy implements IRetryPolicy {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final long MIN_WAIT = 128;
    private static final long MAX_WAIT = 16384; //Approx. 16 seconds.
    

    private long failTime = 0;
    private long wait = 0;
    private CallingState callingState = CallingState.SKIP;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED; 
    
    
    public synchronized boolean callServer() {
        if (logger.isDebugEnabled()) {
            logger.debug("policy: " + toString() + " callServer: currentCallState is " + callingState);
        }
    	return callingState == CallingState.CALL;
    }

    public synchronized void markFailure() {
        CallingState startState = callingState;
        if (callingState == CallingState.CALL) { //first failure
            wait = MIN_WAIT;
            failTime = System.currentTimeMillis();
            callingState = CallingState.SKIP;
            connectionState = ConnectionState.DISCONNECTED;
        } else { //not the first failure
        	if (connectionState == ConnectionState.RECONECTING) {
                connectionState = ConnectionState.DISCONNECTED;
        		wait *= 2;
        		if (wait > MAX_WAIT) {
        			wait = MAX_WAIT;
        		}
        	}
        }
        if (logger.isDebugEnabled()) {
            logger.debug("policy: " + toString() + " markFailure: from " + startState + " to " + callingState);
        }
    }

    public synchronized void markSuccess() {
        logger.debug("policy: " + toString() + " markingSuccess");
    	callingState = CallingState.CALL;
    	connectionState = ConnectionState.CONNECTED;
    }

    public synchronized boolean shouldReconnect() {
        boolean retVal;
        ConnectionState startState = connectionState;
    	if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.RECONECTING) {
    		retVal = false;
    	} else if (System.currentTimeMillis() > (failTime + wait)) {
            connectionState = ConnectionState.RECONECTING;
            retVal = true;
        } else {
        	retVal = false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("policy: " + toString() + " reconnect: from " + startState + " to " + connectionState + "returning " + retVal);
        }
        return retVal;
    }
    
    private enum CallingState {CALL, SKIP};
    private enum ConnectionState {CONNECTED, DISCONNECTED, RECONECTING};

}
