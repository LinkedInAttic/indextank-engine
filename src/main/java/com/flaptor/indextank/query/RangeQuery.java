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

import org.apache.lucene.search.TermRangeQuery;

/**
 * This is an AQuery to filter for a range of values.
 *
 * @author Flaptor Development Team
 */
public class RangeQuery extends QueryNode {
    private static final long serialVersionUID = 1L;
    private final String field;
    private final String start;
    private final String end;
    private final boolean includeStart;
    private final boolean includeEnd;

    /**
     * Creates a RangeQuery with the given parameters
     */
    public RangeQuery(String field, String start, String end, boolean includeStart, boolean includeEnd) {
        this.field = field;
        this.start = start;
        this.end = end;
        this.includeStart = includeStart;
        this.includeEnd = includeEnd;
    }
    
    /**
     * @return a lucene query.
     * @see org.apache.lucene.search.Query
     */
    public org.apache.lucene.search.Query getLuceneQuery() {
    	//Translation from Lucene 2.4. We need to check if it is actually the exact same thing to do.
    	return new TermRangeQuery(field,start,end,includeStart,includeEnd);
        //return new ConstantScoreRangeQuery(field,start,end,includeStart,includeEnd);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        RangeQuery q = (RangeQuery) obj;
        return     field.equals(q.field)
                && start.equals(q.start)
                && end.equals(q.end)
                && includeStart == q.includeStart
                && includeEnd == q.includeEnd;
    }
    
    @Override
    public int hashCode() {
        return    field.hashCode()
                ^ start.hashCode() 
                ^ end.hashCode() 
                ^ Boolean.valueOf(includeStart).hashCode() 
                ^ Boolean.valueOf(includeEnd).hashCode()
                ^ super.hashCode();
    }

    @Override
    public QueryNode duplicate() {
        QueryNode qn = new RangeQuery(this.field, this.start, this.end, this.includeStart, this.includeEnd);
        qn.setBoost(this.getBoost());
        qn.setNorm(this.getNorm());
        return qn;
    }

}
