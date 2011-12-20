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
 *  This class represents a triad of objects.
 *  It's useful when you need to make clear that the number of
 *  objects you're storing is exactly 3.
 *  One or both objects can be null.
 *  
 *  @author Martin Massera
 */
public final class Triad<T1, T2, T3> implements Serializable, Comparable<Triad<? extends Comparable<T1>, ? extends Comparable<T2>, ? extends Comparable<T3>>>{
    private static final long serialVersionUID = 1L;
    private T1 fst;
    private T2 scnd;
    private T3 thrd;
    
    /**
    Constructor.
     */
    public Triad(final T1 first, final T2 second, final T3 third) {
        this.fst = first;
        this.scnd = second;
        this.thrd = third;
    }

    public T1 first() {
        return fst;
    }

    public T2 second() {
        return scnd;
    }

    public T3 third() {
        return thrd;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("(")
            .append(fst)
            .append(" ; ")
            .append(scnd)
            .append(" ; ")
            .append(thrd)
            .append(")");
        return buf.toString();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (o == null) return false;
        if (! o.getClass().equals(this.getClass())) return false;
        Triad<T1,T2,T3> obj = (Triad<T1,T2,T3>)o;
        if (fst == null) {
            if (obj.fst != null) return false; 
        } else {
            if (!fst.equals(obj.fst)) return false;
        }
        if (scnd == null) {
            if (obj.scnd != null) return false; 
        } else {
            if (!scnd.equals(obj.scnd)) return false;
        }
        if (thrd == null) {
            if (obj.thrd != null) return false; 
        } else {
            if (!thrd.equals(obj.thrd)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h1 = (null == fst) ? 0 : fst.hashCode();
        int h2 = (null == scnd) ? 0 : scnd.hashCode();
        int h3 = (null == thrd) ? 0 : thrd.hashCode();
        return h1 ^ h2 ^ h3;
    }
 
    /**
     * compares the first element and if it equal, compares the second and if equal, compares the third
     */
	public int compareTo(Triad<? extends Comparable<T1>, ? extends Comparable<T2>, ? extends Comparable<T3>> o) {
		int ret = -o.fst.compareTo(fst);
		if (ret != 0) return ret;
		else {
			ret = -o.scnd.compareTo(scnd);
			if (ret != 0) return ret;
			else return -o.thrd.compareTo(thrd);
		}
	}
}

