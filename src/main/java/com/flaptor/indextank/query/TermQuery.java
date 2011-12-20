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

import org.apache.lucene.index.Term;

/**
 * Query that searches for a specific term in a specific field.
 * The term passed to the constructor does not suffer any modifications, no case conversions,
 * no tokenization. So great care has to be taken to be sure the term passed is consistent with
 * the tokenization made at index time.
 * @author spike
 *
 */
public final class TermQuery extends QueryNode implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String field;
	protected String term;
    protected float boost;
    
    /**
     * Basic constructor.
     * @param field the field where to look the term in. Must not be null.
     * @param term the term to search for. Must not be null.
     * @throws IllegalArgumentException if term or field are null.
     */
    public TermQuery(final String field, final String term) {
        if (null == field) throw new IllegalArgumentException("constructor: field must not be null.");
        if (null == term) throw new IllegalArgumentException("constructor: term must not be null.");
        this.field = field;
        this.term = term;
    }
    
    public String getField() {
    	return field;
    }
    
    public String getTerm() {
    	return term;
    }

    public void setField(String field) {
        this.field = field;
    }
    
    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public Set<TermQuery> getPositiveTerms() {
        Set<TermQuery> retval = new HashSet<TermQuery>();
        retval.add(this);
        return retval;
    }

    @Override
    public org.apache.lucene.search.Query getLuceneQuery() {
        org.apache.lucene.search.TermQuery luceneQuery = new org.apache.lucene.search.TermQuery(new Term(field, term));
        luceneQuery.setBoost(boost);
        return luceneQuery;
    }

    @Override
    public String toString() {
        return "field: " + field + "; term: " + term + boostString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj))
            return false;
        TermQuery tq = (TermQuery) obj;
        return field.equals(tq.field) && term.equals(tq.term);
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + field.hashCode();
        hash = 17 * hash + term.hashCode();
        hash = hash ^ super.hashCode();
        return hash;
    }

    @Override
    public QueryNode duplicate() {
        QueryNode qn = new TermQuery(this.field, this.term);
        qn.setBoost(this.getBoost());
        qn.setNorm(this.getNorm());
        return qn;
    }

}
