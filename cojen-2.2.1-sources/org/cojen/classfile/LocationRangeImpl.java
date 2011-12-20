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
 * 
 * @author Brian S O'Neill
 */
public class LocationRangeImpl implements LocationRange {
    private final Location mStart;
    private final Location mEnd;

    public LocationRangeImpl(Location a, Location b) {
        if (a.compareTo(b) <= 0) {
            mStart = a;
            mEnd = b;
        } else {
            mStart = b;
            mEnd = a;
        }
    }

    public LocationRangeImpl(LocationRange a, LocationRange b) {
        mStart = (a.getStartLocation().compareTo(b.getStartLocation()) <= 0) ?
            a.getStartLocation() : b.getStartLocation();

        mEnd = (b.getEndLocation().compareTo(a.getEndLocation()) >= 0) ?
            b.getEndLocation() : a.getEndLocation();
    }

    public Location getStartLocation() {
        return mStart;
    }

    public Location getEndLocation() {
        return mEnd;
    }

    public int hashCode() {
        return getStartLocation().hashCode() * 31 + getEndLocation().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocationRangeImpl) {
            LocationRangeImpl other = (LocationRangeImpl)obj;
            return getStartLocation() == other.getStartLocation() &&
                getEndLocation() == other.getEndLocation();
        }
        return false;
    }

    public String toString() {
        int start = getStartLocation().getLocation();
        int end = getEndLocation().getLocation() - 1;

        if (start == end) {
            return String.valueOf(start);
        } else {
            return start + ".." + end;
        }
    }
}
