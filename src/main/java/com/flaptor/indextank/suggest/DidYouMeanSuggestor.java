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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.QueryNode;
import com.flaptor.indextank.query.SimplePhraseQuery;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.org.apache.lucene.util.automaton.LevenshteinAutomata;
import com.flaptor.util.Pair;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * A suggestor that uses the corpus (index) and a VP-tree to suggest queries
 * based on Levenshtein distance on each term.
 */
public class DidYouMeanSuggestor {
    private final IndexEngineParser parser;
    private final NewPopularityIndex npi;
    private final NewPopularityIndex.PopularityIndexAutomaton dictAutomaton;

    public DidYouMeanSuggestor(TermSuggestor suggestor) {
        Preconditions.checkNotNull(suggestor);
        this.parser = new IndexEngineParser("text");
        this.npi = suggestor.getPopularityIndex();
        this.dictAutomaton = NewPopularityIndex.PopularityIndexAutomaton.adapt(this.npi);
    }

    public List<Pair<Query, String>> suggest(Query query) {
        Query newQuery = query.duplicate();
        
        String newOriginal = traverseNode(newQuery.getRoot(), newQuery.getOriginalStr());
        
        if (newQuery.equals(query)) {
            return Lists.newArrayList();
        }

        return Lists.newArrayList(new Pair<Query, String>(newQuery, newOriginal));
    }

    private String traverseNode(QueryNode node, String queryString) {
        if (node instanceof TermQuery) {
            TermQuery termQuery = (TermQuery)node;
            String term = termQuery.getTerm();
            
            String suggestedTerm = suggestWord(term);
            if (suggestedTerm != null) {
                queryString = replaceSuggestion(queryString, term, suggestedTerm);
                if (queryString == null) {
                    return null;
                }
                termQuery.setTerm(suggestedTerm);
            }
            
        } else if (node instanceof SimplePhraseQuery) {
            SimplePhraseQuery phraseQuery = (SimplePhraseQuery) node;
            
            String[] termsArray = phraseQuery.getTermsArray();
            
            for (int i = 0; i < termsArray.length; i++) {
                String term = termsArray[i];
                String suggestedTerm = suggestWord(term);

                if (suggestedTerm != null) {
                    queryString = replaceSuggestion(queryString, term, suggestedTerm);
                    if (queryString == null) {
                        return null;
                    }
                    termsArray[i] = suggestedTerm;
                }

            }
        }
        
        Iterable<QueryNode> children = node.getChildren();
        for (QueryNode queryNode : children) {
            queryString = traverseNode(queryNode, queryString);
            if (queryString == null) {
                return null;
            }
        }
        
        return queryString;
    }

    private String replaceSuggestion(String queryString, String term, String suggestedTerm) {
        Pattern pattern = Pattern.compile("\\b(" + term + ")\\b(?!\\:)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(queryString);
        
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            matcher.appendReplacement(sb, suggestedTerm);
            matcher.appendTail(sb);
            if (matcher.find()) {
                return null;
            }
        } else {
            return queryString;
        }
        return sb.toString();
    }
    
    private String suggestWord(String term) {
        String bestSuggestion = null;
        if (term.length() > 3) { 
            com.flaptor.org.apache.lucene.util.automaton.Automaton lev = new LevenshteinAutomata(term).toAutomaton(1);
            LuceneAutomaton levAutomaton = LuceneAutomaton.adapt(lev);
        
            int max = 0;
            for (String suggestion: com.flaptor.indextank.suggest.Automaton.intersectPaths(dictAutomaton, levAutomaton)){
                if (term.equals(suggestion)){
                    // don't suggest anything for words seen on the corpus
                    bestSuggestion = null;
                    break;
                }

                int count = this.npi.getCount("text:" + suggestion);
                if (count > max) {
                   bestSuggestion = suggestion;
                   max = count;
                }
            }
        }
        return bestSuggestion;
    }
}
