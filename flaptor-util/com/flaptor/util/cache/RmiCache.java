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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for RMI access to a cache 
 * 
 * @author Martin Massera
 */
public interface RmiCache<T> extends Remote {
    
    public void addItem (String key, T value) throws RemoteException;    
    
    public boolean hasItem (String key)  throws RemoteException;
    
    public T getItem (String key) throws RemoteException;
    
    public boolean removeItem (String key) throws RemoteException;
}
