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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;

/**
 * Represents de mathematical difference (lt - rt).
 * @author Flaptor Development Team
 */
public final class DifferenceQuery extends BinaryQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    public DifferenceQuery(final QueryNode lt, final QueryNode rt) {
        super(lt, rt);
        this.setNorm(lt.getBoostedNorm());
    }

    public org.apache.lucene.search.Query getLuceneQuery() {
        BooleanQuery bq = new BooleanQuery();
        bq.add(leftQuery.getLuceneQuery(), BooleanClause.Occur.MUST);
        bq.add(rightQuery.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        return bq;
    }
    
    @Override
    public Set<TermQuery> getPositiveTerms() {
        return leftQuery.getPositiveTerms();
    }

    @Override
    public String toString() {
        return "( " + leftQuery.toString() + " ) - ( " + rightQuery.toString() + " )" + boostString();
    }

    /**
     * Implements a very simple equality.
     * a - b != b - a
     */
    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj))
            return false;
        BinaryQuery bq = (BinaryQuery) obj;
        if (leftQuery.equals(bq.leftQuery) && rightQuery.equals(bq.rightQuery)) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 17;
        hash = 3 * hash + leftQuery.hashCode();
        hash = 3 * hash + rightQuery.hashCode();
        hash = hash ^ super.hashCode();
        return hash;
    }
    
    public QueryNode duplicate() {
        QueryNode qn = new DifferenceQuery(this.leftQuery.duplicate(), this.rightQuery.duplicate());
        qn.setBoost(this.getBoost());
        qn.setNorm(this.getNorm());
        return qn;
    }
}
