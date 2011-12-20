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

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;



public abstract class ARmiClientStub extends AClientStub {
   
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private final String serviceName;
    private final boolean waitForPolicy;
    private boolean remoteInitialized;

    public ARmiClientStub(int port, String host, String context, IRetryPolicy policy) {
        this(port,host,context,policy,false);
    }

    public ARmiClientStub(int port, String host, IRetryPolicy policy) {
        this(port,host,policy,false);
    }

    public ARmiClientStub(int port, String host, IRetryPolicy policy, boolean wait) {
        super(port,host,policy);
        this.waitForPolicy = wait;
        serviceName = RmiServer.DEFAULT_SERVICE_NAME;
    }

    public ARmiClientStub(int port, String host, String context, IRetryPolicy policy, boolean wait) {
        super(port,host,policy);
        this.waitForPolicy = wait;
        serviceName = context != null ? context : RmiServer.DEFAULT_SERVICE_NAME;
    }

    private void connect() throws RemoteException{
        logger.debug("connecting to " + host + ":" + port);
    	Remote remote = RmiUtil.getRemoteService(host, port, serviceName);
    	if (null != remote) {
    		this.setRemote(remote);
            remoteInitialized = true;
    	} else {
    		throw new RemoteException("Could not get remote service: " + serviceName + "@" + host + ":" + port);
    	}
    };


    /**
     Reconnects to the searcher if necessary, and return wheather the rpc should be performed or not.
    **/
    public boolean checkConnection() throws RemoteException{
        boolean needToReconnect = policy.shouldReconnect() || !remoteInitialized;
        if (needToReconnect) {
            try {
                connect();
                return true;
            } catch (RemoteException e) {
                connectionFailure();
                throw e;
            }
        }
        
        // If policy does not allow to connect
        if (!policy.callServer())  {
            // if we have to wait for the policy to allow us
            if (waitForPolicy) {
                //wait
                while (!policy.callServer()) {
                    Execute.sleep(100);
                }
            // otherwise, just fail
            } else {
                return false;
            }
        }

        if (!remoteInitialized) {
            return false;
        }

        return true;
    }

    public void connectionSuccess() {
        policy.markSuccess();
    }
    public void connectionFailure() {
        policy.markFailure();
    }


    protected abstract void setRemote(Remote stub);


    public String toString() {
        return host + ":" + port;
    }

    public boolean equals(Object other){
        if (null == other) return false;
        if (! (other instanceof ARmiClientStub)) return false;
        ARmiClientStub stub = (ARmiClientStub)other;
        return (port == stub.port && host.equals(stub.host) && serviceName.equals(stub.serviceName));
    }

    public int hashCode(){
        int hash = port;
        hash ^= (null == host)?7919:host.hashCode();
        hash ^= (null == serviceName)? 7919: serviceName.hashCode();
        return hash;
    }
}
