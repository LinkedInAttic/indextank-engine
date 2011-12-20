/*
 *  Copyright 2004-2010 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.classfile;

/**
 * Implementation of a Location with a fixed, constant address.
 *
 * @author Brian S O'Neill
 */
public class FixedLocation implements Location {
    private int mLocation;
    
    public FixedLocation(int location) {
        mLocation = location;
    }
    
    public int getLocation() {
        return mLocation;
    }

    public int compareTo(Location other) {
        if (this == other) {
            return 0;
        }
        
        int loca = getLocation();
        int locb = other.getLocation();
        
        if (loca < locb) {
            return -1;
        } else if (loca > locb) {
            return 1;
        } else {
            return 0;
        }
    }
}
