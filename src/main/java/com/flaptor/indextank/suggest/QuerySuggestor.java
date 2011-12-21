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

package com.flaptor.indextank.suggest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.query.AToken;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.google.common.base.Preconditions;

/**
 * A suggestor that uses the previously performed queries to autocomplete.
 * This implementation doesn't use the corpus (index) at all, and does
 * not suggest anything.
 */
public class QuerySuggestor implements Suggestor {
    private final NewPopularityIndex index;
    private final IndexEngineParser parser;

    public QuerySuggestor(IndexEngineParser parser, File backupDir) throws IOException {
        Preconditions.checkNotNull(parser);
        Preconditions.checkNotNull(backupDir);
    	this.parser = parser;
        this.index = new NewPopularityIndex(backupDir);
    }

    @Override
    public void noteQuery(Query query, int matches) {
        if (matches != 0) {
            for (TermQuery tq : query.getRoot().getPositiveTerms()) {
                index.addTerm(tq.getTerm());
            }
        }
    }

    /**
     * This implementation of suggestor disregards the corpus.
     **/
    @Override
    public void noteAdd(String documentId, Document doc) {
        //Does nothing.
    }

    @Override
    public void dump() throws IOException {
        index.dump();
    }

    @Override
    public List<String> complete(String partialQuery, String field) {
        Iterator<AToken> it = parser.parseDocumentField("", partialQuery);
        if (!it.hasNext()) {
            return new ArrayList<String>(0);
        }
        List<String> retVal = index.getMostPopular(it.next().getText());
        if (null == retVal) {
            return new ArrayList<String>(0);
        } else {
            return retVal;
        }
    }
    
    @Override
    public Map<String, String> getStats() {
        return this.index.getStats();
    }

}
