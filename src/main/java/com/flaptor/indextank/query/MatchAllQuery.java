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

/**
 * Returns all documents.
 * Matches all document from the index with exactly the same score.
 * Ignores the boost.
 * @author Flaptor Development Team
 */
public final class MatchAllQuery extends QueryNode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public org.apache.lucene.search.Query getLuceneQuery() {
        return new org.apache.lucene.search.MatchAllDocsQuery();
    }

    @Override
    public String toString() {
        return "<MatchAllQuery>";
    }
    
    /**
     * All instances of this object are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }
    
    /**
     * Returns a constant. @see equals()
     */
    @Override
    public int hashCode() {
        return 55; //an arbitrary number.
    }

    @Override
    public QueryNode duplicate() {
        return new MatchAllQuery();
    }
        
}
