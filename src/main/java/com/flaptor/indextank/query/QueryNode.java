/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
package com.flaptor.indextank.query;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Node of a query tree
 */
public abstract class QueryNode implements Serializable {
    private static final long serialVersionUID = 1L;
    private double boost = 1d;
    private double norm = 1d;
    
    /**
     * Abstract method that returns a lucene query that represents this Hounder query.
     * @return a lucene query.
     * @see org.apache.lucene.search.Query
     */
    public abstract org.apache.lucene.search.Query getLuceneQuery();

    public Set<TermQuery> getPositiveTerms() {
        return new HashSet<TermQuery>();
    }

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public double getBoost() {
        return this.boost;
    }

    public void setNorm(double norm) {
        this.norm = norm;
    }

    public double getNorm() {
        return this.norm;
    }

    public double getBoostedNorm() {
        return this.boost * this.norm;
    }

    @Override
    public String toString() {
        return getLuceneQuery().toString() + boostString();
    }

    public String boostString() {
        return boost != 1f ? "; boost: "+boost : "";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        QueryNode qn = (QueryNode) obj;
        return boost == qn.boost;
    }
    
    @Override
    public int hashCode() {
        int hash = (int)(10000*boost);
        hash = 17 * hash + super.hashCode();
        return hash;
    }


    /*
     * Cloning methods.
     */
    public abstract QueryNode duplicate();
    
    public Iterable<QueryNode> getChildren() {
        return Sets.newHashSet();
    }
    
}
