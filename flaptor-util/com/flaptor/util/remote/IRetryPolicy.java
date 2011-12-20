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

/**
 * A ClientStub has to deal with potential losses of the connection.
 * In some situations, it's useful to actually attempt the rpc every
 * time, but in others, as the cost of a failed rpc is high (we may
 * to wait for a long timeout) we want to skip calling the server for
 * some time after a failure, and attempt the reconnection some time
 * later.
 * Important: any implementation of this interface must be thread
 * safe.
 * @see AClientStub
 */
public interface IRetryPolicy {
    /**
     * Called after a succesful rpc call.
     */
    public void markSuccess();

    /**
     * Called after a failed rpc call.
     */
    public void markFailure();
    
    /**
     * Called before trying to perform the actual rpc call.
     * @return true if the ClientStub is to issue the actual rpc call
     *  to the server.
     */
    public boolean callServer();
    
    /**
     * Called to ask if the ClientStub is to try to reconnect to the server.
     * By convention, any implementation of IRetryPolicy must return true before
     * the first time it's called.
     * @return true if the ClientStub is to try to reconnect to the server.
     */
    public boolean shouldReconnect();
}
