package com.flaptor.indextank.suggest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.query.AToken;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.google.common.base.Preconditions;

/**
 * A suggestor that uses the indexed terms to autocomplete.
 * This implementation doesn't use the queries at all, and does
 * not suggest anything.
 */
public class TermSuggestor implements Suggestor {
    private final NewPopularityIndex index;
    private final IndexEngineParser parser;

    public TermSuggestor(IndexEngineParser parser, File backupDir) throws IOException {
		Preconditions.checkNotNull(parser);
        Preconditions.checkNotNull(backupDir);
    	this.parser = parser;
        this.index = new NewPopularityIndex(backupDir);
    }

    @Override
    public void noteQuery(Query query, int matches) {
        //this implementation doesn't use queries
        return;

    }

    @Override
    public void noteAdd(String documentId, Document doc) {
    	Set<String> fieldNames = doc.getFieldNames();
    	for (String field : fieldNames) {
    		addContent(field, doc.getField(field));
		}
    }

    @Override
    public void dump() throws IOException {
        index.dump();
    }

    public List<String> complete(String partialQuery, String field) {
        Iterator<AToken> it = parser.parseDocumentField(field, partialQuery);
        AToken lastToken = null;
        while (it.hasNext()) {
            lastToken = it.next();
        }
        
        if (lastToken == null) {
            return Collections.emptyList();
        } else {
            List<String> popularTerms = index.getMostPopular(field + ":" + lastToken.getText());
            if (null == popularTerms) {
                return Collections.emptyList();
            } else {
                int start = lastToken.getStartOffset();
                String prefix = partialQuery.substring(0, start);
                List<String> suggestions = new ArrayList<String>(popularTerms.size());
                for (String term : popularTerms) {
                	term = term.substring(field.length() + 1);
                    suggestions.add(prefix + term);
                }
                return suggestions;
            }
        }
    }

    private void addContent(String fieldName, String content) {
        Iterator<AToken> it = parser.parseDocumentField(fieldName, content);
        while (it.hasNext()) {
            AToken token = it.next();
            index.addTerm(fieldName + ":" + token.getText());
        }
    }

    /*package */ NewPopularityIndex getPopularityIndex(){
        return this.index;
    }
    
    @Override
    public Map<String, String> getStats() {
        return this.index.getStats();
    }

}
