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
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.FileUtil;

/**
 * This class implements a generic server that exports an object via Rmi.
 * The exported object must be an instance of remote.
 */
public class RmiServer extends AServer {

    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
	private final Map<String, Remote> handlers = new HashMap<String, Remote>(); 
    
    public static final String DEFAULT_SERVICE_NAME = "default service";

    /**
     * Runs the server from the command line.
     * 
     * @param args
     *            Second: Handler class (must implement Remote).
     *            Third: port the port where to start the server (actually, the rmiRegister).
	 * @see java.rmi.Remote
     */
    public static void main(final String[] args) throws Exception{
        if (args.length != 2) {
            System.err.println("usage: RmiServer handler-class port");
            return;
        }

        String log4jConfigPath = FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found in classpath! Reload disabled.");
        }
        Object handler = null;
        handler = Class.forName(args[0]).newInstance();
        if (!(handler instanceof Remote)) {
            System.err.println("Invalid handler, must implement java.rmi.Remote");
            return;
        }
        
        RmiServer server = new RmiServer(Integer.parseInt(args[1]));
        server.addHandler(DEFAULT_SERVICE_NAME, (Remote)handler); 
        server.start(); // RmiServer doesn't implement a new thread, ask the server to do it.
    }

    /**
     * Constructor.
     * Uses the name of the class passed as handler as the name to register (actually
     *  it usess .getClass.getName() ).
     * @param h the objet to handle the requests.
     * @param p the port where to create a rmi registry to listen
     */
    public RmiServer(final int p) {
        super(p, true);
        Policy.setPolicy(new BadPolicy());
    }

	/**
	 * adds a handler to the server
	 * @param context
	 * @param handler
	 * @throws IllegalStateException if the server has been requested to stop or is stopped 
	 */
	synchronized public void addHandler(String context, Remote handler) {
        if (RunningState.STOPPING == runningState || RunningState.STOPPED == runningState) {
            throw new IllegalStateException("Server has already been signaled to stop, cannot add handler");
        }
		handlers.put(context, handler);
		if (RunningState.RUNNING == runningState) registerHandler(context, handler);
	}
    
    /**
     * Starts the server.
     */
    @Override 
    synchronized protected void startServer() {
		for (Map.Entry<String, Remote> entry : handlers.entrySet()) {
			registerHandler(entry.getKey(),entry.getValue());
		}
    }

    private void registerHandler(String context, Remote handler) {
        logger.debug("Registering service " + context + " on registry running on port " + port);
        boolean ok = RmiUtil.registerLocalService(port, context, handler);
        if (ok) {
            logger.debug("service " + context + " registered on registry running on port " + port);
        } else {
            logger.error("Couldn't register service " + context + " on registry running on port " + port);
        }
    }
    

    private static class BadPolicy extends Policy {
        private final PermissionCollection perms;
        
        public BadPolicy() {
            Permissions p = new Permissions();
            p.add(new java.security.AllPermission());
            perms = p;
        }
        
        @Override
        public PermissionCollection getPermissions(CodeSource c) {
            return perms;
        }
        
        @Override
        public void refresh() {
            
        }
    }

	@Override
	protected Map<String, Remote> getHandlers() {
		return handlers;
	}

	@Override
	protected void requestStopServer() {
		for (Map.Entry<String, Remote> entry : handlers.entrySet()) {
			RmiUtil.unregisterLocalService(port, entry.getKey());
		}
        RmiUtil.unregisterRegistry(port);
	}
}

