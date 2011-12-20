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
 * This interface represents a class that can be stopped.
 * The stopping process this interface describes has 2 phases: first the
 * class is requested to stop, and after some period of time it is stopped.
 * This behavior is useful for classes having a long stop sequence for which
 * we want to implement a non bloking stop mechanism.
 * The 2 phases stop procedure leads to think of the class as having 3
 * distinct states: running, stop requested and stopped. So it may be
 * useful to use internally RunningState to describe this situation.
 * @see RunningState 
 */
public interface Stoppable {
    /**
     * Signals to stop. Note that the actual stopping may take some time,
     * depending of the implementation of the run method, BUT the method must
     * return immediately (i.e. be non-blocking).
     */
	public void requestStop();

    /**
     * Says whether or not the class is stopped.
     * @return true if stopped, false otherwise.
     */
    public boolean isStopped();
}
