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

/**
 * class related utils 
 * @author Martin Massera
 */
public class ClassUtil {
	
	/**
	 *   
	 * @param className
	 * @return an instance of the specified class, using the empty constructor
	 * @throws RuntimeException if there is a problem
	 */
	public static Object instance(String className) {
		try {
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
