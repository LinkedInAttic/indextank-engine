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


package com.flaptor.indextank;

import static com.flaptor.util.TestInfo.TestType.SYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.search.DocumentSearcher;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestInfo;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;

public class ResultsOrderingTest extends IndexTankTestCase {

    private File tempDir;
    private IndexEngine indexEngine;

    @Override
	protected void setUp() throws Exception {
        super.setUp();
        this.tempDir = FileUtil.createTempDir("indextank","testcase");
        this.indexEngine = new IndexEngine(this.tempDir, 11234, 100, false, 5, IndexEngine.SuggestValues.DOCUMENTS, null, false, "dummyCode", "TEST-environment");
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();        
	}

    private void index(int id, long t) {
        String text = "text word" + id;
        for (int i = 0; i < new Random().nextInt(25); i++) {
            text += " text";
        }
        Document doc = new Document(ImmutableMap.of("text", text, "timestamp", String.valueOf(t)));
        this.indexEngine.getIndexer().add(String.valueOf(id), doc, (int)t, ImmutableMap.<Integer, Double>of());
    }

    private void checkResults(String name, DocumentSearcher searcher, Query query, int scoringFunction, String[] expectedIds) throws InterruptedException {
        //System.out.println("TESTING query: ["+query.toString()+"]");
        SearchResults srs = searcher.search(query, 0, 100, scoringFunction);
    	int i = 0;
        boolean equals = true;
        String actualStr = "";
    	for (SearchResult r : srs.getResults()) {
            equals = equals && (i < expectedIds.length) && (r.getDocId().equals(expectedIds[i]));
            actualStr += r.getDocId()+" ";
    		i++;
    	}
        assertTrue(name+": Results out of order: expected "+Arrays.toString(expectedIds)+", actual ["+actualStr.trim().replaceAll(" ",", ")+"]", equals);
    }

    @TestInfo(testType=SYSTEM)
    public void testAgeOrder() throws IOException, ParseException, InterruptedException {
        long t = System.currentTimeMillis() / 1000;
        for (int i = 0; i < 100; i++) {
            index(i, t+i);
        }
        
        Query q = new Query(this.indexEngine.getParser().parseQuery("text"),null,null);
        SearchResults rs = this.indexEngine.getSearcher().search(q, 0, 100, 0);
        
        Iterable<SearchResult> results = rs.getResults();
        
        int n = 99;
        for (SearchResult r : results) {
            assertEquals("Age: Results out of order", String.valueOf(n), r.getDocId());
            n--;
        }
    }

    @TestInfo(testType=SYSTEM)
    public void testRelevanceOrder() throws Exception {
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        BoostingIndexer indexer = this.indexEngine.getIndexer();
        indexer.addScoreFunction(1, "relevance");

        Document doc = new Document(ImmutableMap.of("relevance_test", "Twinkle twinkle you better work"));
        indexer.add("1", doc, 0, ImmutableMap.<Integer, Double>of());
        doc = new Document(ImmutableMap.of("relevance_test", "Twinkle you better work or else I'll hack you"));    	
        indexer.add("2", doc, 1, ImmutableMap.<Integer, Double>of());
        doc = new Document(ImmutableMap.of("relevance_test", "Twinkle you better work"));    	
        indexer.add("3", doc, 2, ImmutableMap.<Integer, Double>of());
        
        Query q = new Query(this.indexEngine.getParser().parseQuery("relevance_test:twinkle"),null,null);

        checkResults("Relevance", searcher, q, 1, new String[] {"1","3","2"});
    }
    

    @TestInfo(testType=SYSTEM)
    public void testCaretQueries() throws InterruptedException, ParseException, Exception {
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        BoostingIndexer indexer = this.indexEngine.getIndexer();
        indexer.addScoreFunction(1, "relevance");
        Document doc;
        Query query;

        doc = new Document(ImmutableMap.of("text", "aaa"));
        indexer.add("1", doc, 0, ImmutableMap.<Integer, Double>of());

        doc = new Document(ImmutableMap.of("text", "aaa bbb"));
        indexer.add("2", doc, 0, ImmutableMap.<Integer, Double>of());

        doc = new Document(ImmutableMap.of("text", "aaa aaa bbb"));
        indexer.add("3", doc, 0, ImmutableMap.<Integer, Double>of());

        doc = new Document(ImmutableMap.of("text", "aaa bbb ccc eee"));
        indexer.add("4", doc, 0, ImmutableMap.<Integer, Double>of());

        doc = new Document(ImmutableMap.of("text", "ccc ddd"));
        indexer.add("5", doc, 0, ImmutableMap.<Integer, Double>of());

        query = new Query(indexEngine.getParser().parseQuery("aaa"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"1","3","2","4"});

        query = new Query(indexEngine.getParser().parseQuery("aaa AND bbb"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"2","3","4"});

        query = new Query(indexEngine.getParser().parseQuery("aaa OR bbb"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"2","3","1","4"});

        query = new Query(indexEngine.getParser().parseQuery("aaa^2 OR bbb"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"3","2","1","4"});

        query = new Query(indexEngine.getParser().parseQuery("aaa OR bbb^2"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"2","3","4","1"});

        query = new Query(indexEngine.getParser().parseQuery("ccc OR ddd"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"5","4"});

        query = new Query(indexEngine.getParser().parseQuery("ccc OR ddd OR eee^2"),null,null);
        checkResults("Caret", searcher, query, 1, new String[] {"4","5"});

    }
    
}
