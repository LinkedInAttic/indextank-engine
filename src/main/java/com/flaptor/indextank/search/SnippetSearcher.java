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

package com.flaptor.indextank.search;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.query.AToken;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.indextank.storage.alternatives.DocumentStorage;
import com.flaptor.indextank.util.CharacterTranslator;
import com.flaptor.util.Execute;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SnippetSearcher extends AbstractDocumentSearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final DocumentSearcher delegate;
	private final DocumentStorage storage;
	private final IndexEngineParser parser;
    private final Map<SnippeterType, Snippeter> snippeters;

    public enum SnippeterType { HTML_AWARE, LINE_AWARE }

    public SnippetSearcher(DocumentSearcher searcher, DocumentStorage storage, IndexEngineParser parser){
		Preconditions.checkNotNull(searcher);
		Preconditions.checkNotNull(storage);
        this.delegate = searcher;
        this.storage = storage;
        this.parser = parser;


        this.snippeters = ImmutableMap.of(  SnippeterType.HTML_AWARE, new HtmlAwareSnippeter(),
                                            SnippeterType.LINE_AWARE, new LineAwareSnippeter());

    }
   

    /**
     * @see AbstractDocumentSearcher#search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters).
     *
     * @param extraParameters: It will process 'fetch_fields', 'snippet_fields' and 'snippet_type'.
     *      'fetch_fields' and 'snippet_fields' are comma-separated lists of field names to fetch an snippet.
     *      'snippet_type' can be either 'html' or 'lines'. 'html' is the default.
     * 
     */

    @Override
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException {
    	// call delegate searcher
        SearchResults results = this.delegate.search(query, start, limit, scoringFunctionIndex, extraParameters);
        
        long startTime = System.currentTimeMillis();
        
        String[] fetchFields = parseFields(extraParameters, "fetch");
        String[] snippetFields = parseFields(extraParameters, "snippet");
        Set<TermQuery> positiveTerms = query.getRoot().getPositiveTerms();
                
        // find out which snippeter type is the right one for this query
        String snType = extraParameters.get("snippet_type");
        Snippeter sn = null;
        if (null == snType || "html".equalsIgnoreCase(snType)) {
            sn = this.snippeters.get(SnippeterType.HTML_AWARE);
        } else if ("lines".equalsIgnoreCase(snType)) {
            sn = this.snippeters.get(SnippeterType.LINE_AWARE);
        } else {
            throw new IllegalArgumentException("snippet_type has to be either 'html' or 'lines'");
        }
        
        if (fetchFields.length + snippetFields.length > 0) {
        	for (SearchResult result : results.getResults()) {
        		Document data = storage.getDocument(result.getDocId());


        		// fetch fields
        		for (String field : fetchFields) {
                    // handle '*', as a fetch all
                    if ("*".equals(field.trim())){
                        // assume we get the actual fields, not a copy.
                        result.getFields().putAll(data.asMap());
                        break;
                    }
                    String text = data.getField(field);
                    if (null != text) {
                        result.setField(field, text);
                    }
				}

        		// snippet fields
        		for (String field : snippetFields) {
                    String text = data.getField(field);
                    if (null != text) {
                        result.setField("snippet_" + field, sn.snippet(positiveTerms, field, text));
                    }
        		}
        	}
        }
        long endTime = System.currentTimeMillis();
        logger.debug("(search) fetching & snippeting took: " + (endTime - startTime) + " ms.");

        return results;

    }

    @Override
    public int countMatches(Query query) throws InterruptedException {
        return this.delegate.countMatches(query);
    }
    
	private static String[] parseFields(Map<String, String> extraParameters, String key) {
		if (extraParameters.containsKey(key + "_fields")) {
        	return extraParameters.get(key + "_fields").split(",");
        } else { 
        	return new String[0];
        }
	}
	
    private abstract class Snippeter { 

        protected abstract int adjustStart(int position, String text);
        protected abstract int adjustEnd(int position, String text);

        private String snippet(Set<TermQuery> terms, String fieldName, String text) {
            Set<String> termsForField = getTermsForField(terms, fieldName);
            long t1 = System.currentTimeMillis();
            List<AToken> tokens = Lists.newArrayList(parser.parseDocumentField(fieldName, text));
            long t2 = System.currentTimeMillis(); 
            logger.debug(String.format("Parsing field %s took %d ms.", fieldName, t2 - t1));
            List<Integer> matches = Lists.newArrayList();
            
            for (int i = 0; i < tokens.size(); i++) {
                String termInText = tokens.get(i).getText();
                
                for (String termInQuery : termsForField) {
                    if ((termInQuery.endsWith("*") && termInText.startsWith(termInQuery.substring(0, termInQuery.length() - 1))) 
                            || termInQuery.equals(termInText)) {
                        matches.add(i);
                    }
                }
            }
            if (matches.size() == 0) {
                return "";
            }
            Window window = findBestWindow(tokens, matches, 200);
            long t3 = System.currentTimeMillis(); 
            logger.debug(String.format("Finding best window for %d matches took %d ms.", matches.size(), t3 - t2));
            String markedText = mark(window, text);
            long t4 = System.currentTimeMillis(); 
            logger.debug(String.format("Marking text %d chars in %d ms.", markedText.length(), t4 - t3));
            return markedText;
        }


        private String mark(Window window, String text) {
            Preconditions.checkArgument(!window.matches.isEmpty(), "Cannot mark an empty window");
            StringBuilder buff = new StringBuilder(500);
            String open = "<b>";
            String close = "</b>";
            int current = window.start;

            // let subclasses handle where snippets start 
            current = adjustStart(current, text);
            for (AToken token : window.matches) {
                escapeAndAppend(buff, text, current, token.getStartOffset());
                buff.append(open);
                int start = token.getStartOffset();
                int endOffset = token.getEndOffset();
                escapeAndAppend(buff, text, start, endOffset);
                buff.append(close);
                current = endOffset;
            }

            // let subclasses handle where snippets end
            int finish = window.end;
            finish = adjustEnd(finish, text);
            escapeAndAppend(buff, text, current, finish);
            return buff.toString();
        }
        
        private Window findBestWindow(List<AToken> tokens, List<Integer> matches, int maxSize) {
            if (matches.size() == 0) {
                return null;
            }
            List<AToken> mtokens = asTokens(matches, tokens);
            List<Integer> best = null;
            float bestScore = 0f;
            int left = 0;
            int right = 0;
    		while (right < matches.size()) {
	    	    right++;
	            while (mtokens.get(right - 1).getEndOffset() - mtokens.get(left).getStartOffset() > maxSize) {
    		    	left++;
	    	    }
	            List<AToken> candidate = mtokens.subList(left, right);
    		    float score = scoreWindow(candidate);
	    	    if (score > bestScore) {
		        	bestScore = score;
    		    	best = matches.subList(left, right);
	    	    }
    		}
            return getWindowContext(tokens, best);
        } 
 
        private Window getWindowContext(List<AToken> tokens, List<Integer> best) {
            int left = best.get(0);
            int right = best.get(best.size()-1);
            Window window = new Window();
            window.matches = asTokens(best, tokens);
            window.start = tokens.get(Math.max(0, left - 5)).getStartOffset();
            window.end = tokens.get(Math.min(right + 24, tokens.size()-1)).getEndOffset();
            return window;
        }
        
        private float scoreWindow(List<AToken> candidate) {
            Set<String> terms = Sets.newHashSet();
            for (AToken token : candidate) {
                terms.add(token.getText());
            }
            return candidate.size() * terms.size() * terms.size();
        }

    } 


    /**
     * A Snippeter that tries not to cut HTML entities
     */
    private class HtmlAwareSnippeter extends Snippeter { 

        // Snippeter abstract methods
       
        protected int adjustStart(int position, String text) {

            // tokenizers may cut off & on entities  .. fix that
            if (position > 0 && text.charAt(position -1) == '&') {
                return position -1;
            }

            return position;
        }
        
        protected int adjustEnd(int position, String text){

            // tokenizers miss final ; on entities. Try to fix that
            return position;
        }

    }

    /**
     * A Snippeter that returns complete lines.
     */
    private class LineAwareSnippeter extends Snippeter {

        // Snippeter abstract methods
        
        protected int adjustEnd(int finish, String text) {
            while (finish < text.length() && text.charAt(finish) != '\n') {
                finish++;
            }

            if (finish < text.length()) {
                // loop above ended because of text.charAt ..
                // return the endline
                finish++;
            }
            return finish;
        } 

        protected int adjustStart(int current, String text) {
            while (current > 0 && text.charAt(current-1) != '\n') {
                current--;
            }
            return current;
        }

    }


    private List<AToken> asTokens(List<Integer> matches, final List<AToken> tokens) {
        return Lists.transform(matches, new Function<Integer, AToken>() {
            @Override
            public AToken apply(Integer pair) {
                return tokens.get(pair);
            }
        });
    }
    
    private static class Window {
    	int start;
    	int end;
        List<AToken> matches = Lists.newArrayList();
    }
    
    private Set<String> getTermsForField(Set<TermQuery> terms, String fieldName) {
        Set<String> retval = new HashSet<String>();
        for (TermQuery t : terms) {
            if (t.getField().equals(fieldName)) {
                retval.add(t.getTerm());
            }
        }
        return retval;
    }

    private void escapeAndAppend(StringBuilder dest, String str, int start, int offset) {
        if (dest == null ) {
            throw new IllegalArgumentException ("The Writer must not be null.");
        }
        if (str == null) {
            return;
        }

        CharacterTranslator.HTML4.escape(dest, str, start, offset);

    }

}
