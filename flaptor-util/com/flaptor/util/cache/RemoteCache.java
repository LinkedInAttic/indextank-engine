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

import com.flaptor.util.remote.RpcException;

/**
 * Interface for remote caches 
 * 
 * @author Martin Massera
 */
public interface RemoteCache<T extends Serializable> {

    public boolean hasItem (String key) throws RpcException;
    
    public T getItem (String key) throws RpcException;
    
    public void addItem (String key, T value) throws RpcException;    
    
    public boolean removeItem (String key) throws RpcException;
}
