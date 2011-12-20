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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

/**
 * @author Flaptor Development Team
 */
public final class SimplePhraseQuery extends QueryNode implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String field;
    private final String[] terms;
    private final int[] termPositions;

    public SimplePhraseQuery(final String field, final String[] terms,
            int[] positions) {
        if (null == field)
            throw new IllegalArgumentException(
                    "constructor: field must not be null.");
        if (null == terms)
            throw new IllegalArgumentException(
                    "constructor: terms must not be null.");
        if (terms.length == 0)
            throw new IllegalArgumentException(
                    "constructor: terms must have at least one document.");
        if (terms.length != positions.length)
            throw new IllegalArgumentException(
                    "constructor: terms and positions size is different.");

        this.field = field;
        // I'm going to make a defensive copy of terms, since array is not
        // immutable.
        this.terms = new String[terms.length];
        this.termPositions = new int[terms.length];
        for (int i = 0; i < terms.length; i++) {
            if (null == terms[i])
                throw new IllegalArgumentException("constructor: term number "
                        + i + " is null.");
            this.terms[i] = terms[i];
            this.termPositions[i] = positions[i];
        }

    }

    public List<String> getTerms() {
        return ImmutableList.copyOf(terms);
    }

    /**
     * Terms getter intended to modify the phrase query
     * @return
     */
    public String[] getTermsArray() {
        return terms;
    }
    
    public int[] getTermPositions() {
        return this.termPositions;
    }

    public String getField() {
        return field;
    }

    @Override
    public Set<TermQuery> getPositiveTerms() {
        Set<TermQuery> retval = new HashSet<TermQuery>();
        for (String term : terms) {
            retval.add(new TermQuery(field, term));
        }
        return retval;
    }

    @Override
    public org.apache.lucene.search.Query getLuceneQuery() {
        org.apache.lucene.search.PhraseQuery pq = new org.apache.lucene.search.PhraseQuery();
        for (int i = 0; i < terms.length; i++) {
            pq.add(new org.apache.lucene.index.Term(field, terms[i]),
                    termPositions[i]);
        }
        return pq;
    }

    @Override
    public String toString() {
        return "field: " + field + "; terms: " + terms + "; positions: "
                + termPositions + boostString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj))
            return false;
        SimplePhraseQuery other = (SimplePhraseQuery) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (!Arrays.equals(termPositions, other.termPositions))
            return false;
        if (!Arrays.equals(terms, other.terms))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + Arrays.hashCode(termPositions);
        result = prime * result + Arrays.hashCode(terms);
        result = result ^ super.hashCode();
        return result;
    }

    @Override
    public QueryNode duplicate() {
        String[] newTerms = new String[this.terms.length];
        int[] newPositions = new int[this.termPositions.length];
        
        System.arraycopy(this.terms, 0, newTerms, 0, this.terms.length);
        System.arraycopy(this.termPositions, 0, newPositions, 0, this.termPositions.length);
        
        QueryNode qn = new SimplePhraseQuery(this.field, newTerms, newPositions);
        qn.setBoost(this.getBoost());
        qn.setNorm(this.getNorm());
        return qn;
    }

}
