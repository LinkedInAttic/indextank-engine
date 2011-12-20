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

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Helper class to register local services and to get references to remote services via RMI.
 */
public final class RmiUtil {

    private static Logger LOGGER = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private static Map<Integer,Registry> localRegistries = new HashMap<Integer,Registry>();

    /**
     * Private default constructor, prevents inheritance and instantiation.
     */
    private RmiUtil() {}

    /**
     * (Re)connects to a remote service.
     * It seems that server's IP address is cached by the Registry implementation used 
     * (this affects lookups if the service's IP address changed).
     * @param server the registry server's name or IP address (see warning above)
     * @param port the registry port number
     * @param serviceName the service remote name
     *
     */
    public static synchronized Remote getRemoteService(String server, int port, String serviceName) {
        Exception problem = null;
        try {
            Registry registry = LocateRegistry.getRegistry(server, port);
            return (registry.lookup(serviceName));
        } catch (NotBoundException e) {
            problem = e;
        } catch (RemoteException e) {
            problem = e;
        }
        if (problem != null) {
            LOGGER.warn("Couldn't get remote service " +serviceName+ " at server " +server+ ", port " +port+ ": " +problem,problem);
        }
        return null;
    }

    /**
     * Exports a service in a local registry.
     * @param port the registry port number
     * @param serviceName the service remote name
     * @param service an instance of the service to be exported.  A strong reference to the service must be kept in order to prevent it being gc'ed. 
     * @return true if successful, false otherwise
     */
    public static synchronized boolean registerLocalService(int port, String serviceName, Remote service) {
        try {
            Registry registry = localRegistries.get(port);
            if (null == registry) {
                registry = LocateRegistry.createRegistry(port);
                localRegistries.put(port,registry);
            } else {
                LOGGER.debug("There was a local registry on port " + port);
            }
            try {registry.unbind(serviceName);} catch (Exception e) {LOGGER.debug("Problem unbinding service "+serviceName+" from RMI registry");}
            registry.rebind(serviceName,UnicastRemoteObject.exportObject(service,0));
            return true;
        } catch (RemoteException e) {
            LOGGER.error("Couldn't export service " +serviceName+ " at port " +port+ " as " +service,e);
            return false;
        }
    }


    public static boolean registerLocalService(String serviceName, Remote service) {
        return registerLocalService(java.rmi.registry.Registry.REGISTRY_PORT,serviceName,service);
    }

    /**
     * Unregisters a local service.
     * @param port the registry port number.
     * @param serviceName the service remote name
     * @return true if successful, false otherwise
     */
    public static synchronized boolean unregisterLocalService(int port, String serviceName) {
        try {
            Registry registry = localRegistries.get(port);
            if (null != registry) {
                Remote service = registry.lookup(serviceName);
                registry.unbind(serviceName);
                try { UnicastRemoteObject.unexportObject(service,true); } catch (NoSuchObjectException e) { /* good */ }
            } else {
                throw new IllegalArgumentException("There is no registry running on port " + port);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Couldn't unregister service " +serviceName+ " at port " +port+": "+e,e);
            return false;
        }
    }


    public static synchronized void unregisterRegistry(int port) {
    
        Registry registry = localRegistries.get(port);
        if (null == registry) {
            throw new IllegalArgumentException("There is no registry on port " + port);
        }

        try { 
            UnicastRemoteObject.unexportObject(registry,true); // que devuelve ?
            localRegistries.remove(port);
        } catch (Exception e ) {
            throw new IllegalStateException(e);
        }
    }
}

