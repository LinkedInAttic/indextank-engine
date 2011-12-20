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

import java.io.Serializable;

/**
    This class represents a pair of objects.
    It's useful when you need to make clear that the number of
    objects you're storing is exactly 2.
    One or both objects can be null.
*/
@SuppressWarnings("serial")
public class Pair<T1, T2> implements Serializable, Comparable<Pair<? extends Comparable<? super T1>, ? extends Comparable<? super T2>>> {
    final private T1 fst;
    final private T2 lst;
    
    /**
        Constructor.
    */
    public Pair(T1 first, T2 last) {
        fst = first;
        lst = last;
    }

    /**
        Returns the first object of the pair.
    */
    public T1 first() {
        return fst;
    }

    /**
        Return the last object of the pair.
    */
    public T2 last() {
        return lst;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("(")
            .append(fst)
            .append(" ; ")
            .append(lst)
            .append(")");
        return buf.toString();
    }
       
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((null == fst) ? 0 : fst.hashCode());
        result = PRIME * result + ((null == lst) ? 0 : lst.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (null == obj)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Pair<?,?> other = (Pair<?,?>) obj;
        if (null == fst) {
            if (null != other.fst)
                return false;
        } else if (!fst.equals(other.fst))
            return false;
        if (null == lst) {
            if (null != other.lst)
                return false;
        } else if (!lst.equals(other.lst))
            return false;
        return true;
    }

    /**
     * compares the first element and if it is equal, compares the last
     */
	public int compareTo(Pair<? extends Comparable<? super T1>, ? extends Comparable<? super T2>> p) {
		int ret = -p.first().compareTo(this.first());
		if (ret != 0) return ret;
		else return -p.last().compareTo(this.last());
	}
	
}

