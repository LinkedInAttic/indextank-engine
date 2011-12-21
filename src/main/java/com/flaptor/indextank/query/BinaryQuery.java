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
import java.util.Set;

import com.google.common.collect.Sets;


/**
 * A QueryNode composed of 2 QueryNodes joined by a binary operator.
 * This class simple adds the notion of a left operand and a right operand, and it's used to
 * implement ands, ors, -, etc.
 * @author Flaptor Development Team
 */
public abstract class BinaryQuery extends QueryNode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected final QueryNode leftQuery;
    protected final QueryNode rightQuery;
    
    /**
     * Constructor.
     * @param lq the left query (operand)
     * @param rq the right query (operand)
     */
    BinaryQuery(final QueryNode lq, final QueryNode rq) {
        if (null == lq) throw new IllegalArgumentException("constructor: left term must not be null");
        if (null == rq) throw new IllegalArgumentException("constructor: right term must not be null");
        leftQuery = lq;
        rightQuery = rq;
        this.setNorm(lq.getBoostedNorm() + rq.getBoostedNorm());
    }
    
    @Override
    public Set<TermQuery> getPositiveTerms() {
        Set<TermQuery> l = leftQuery.getPositiveTerms();
        Set<TermQuery> r = rightQuery.getPositiveTerms();
        l.addAll(r);
        return l;
    }

    public QueryNode getLeftQuery(){
        return leftQuery;
    }
    public QueryNode getRightQuery(){
        return rightQuery;
    }
    
    @Override
    public Iterable<QueryNode> getChildren() {
        return Sets.newHashSet(leftQuery, rightQuery);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += ((leftQuery == null) ? 0 : leftQuery.hashCode());
        result += ((rightQuery == null) ? 0 : rightQuery.hashCode());
        result = result ^ super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        BinaryQuery other = (BinaryQuery) obj;
        
        return ((leftQuery.equals(other.leftQuery) &&
                rightQuery.equals(other.rightQuery)) ||
                (leftQuery.equals(other.rightQuery) &&
                 rightQuery.equals(other.leftQuery)));
    }
    
    
}
