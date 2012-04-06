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

import static com.flaptor.util.TestInfo.TestType.SYSTEM;
import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.File;
import java.io.IOException;

import com.flaptor.indextank.DocumentStoringIndexer;
import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.PrefixTermQuery;
import com.flaptor.indextank.query.AndQuery;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestInfo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class SnippetSearcherTest extends IndexTankTestCase {

	private File tempDir;
    private IndexEngine indexEngine;
    private DocumentStoringIndexer indexer;
	private SnippetSearcher searcher;

    @Override
	protected void setUp() throws Exception {
        super.setUp();
        this.tempDir = FileUtil.createTempDir("indextank","testcase");
        this.indexEngine = new IndexEngine(this.tempDir, 11234, 5, false, 5, IndexEngine.SuggestValues.NO, IndexEngine.StorageValues.RAM, 0, null, false, "dummyCode", "TEST-environment");
        this.indexer = new DocumentStoringIndexer(indexEngine.getIndexer(), indexEngine.getStorage());
        this.searcher = new SnippetSearcher(indexEngine.getSearcher(), indexEngine.getStorage(), indexEngine.getParser());
	}

    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    }
    
	private void indexVeryBigDoc() {
		double timestampBoost = System.currentTimeMillis() / 1000.0;
		String docId = "docid";
		Document doc = new Document(ImmutableMap.of("text", largeText()));
		indexer.add(docId, doc, (int)timestampBoost, Maps.<Integer, Double>newHashMap());
	}
	private String largeText() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			for (int j = 0; j < 30; j++) {
				sb.append(' ');
				sb.append("term");
				sb.append(j + i*30);
			}
			sb.append('.');
		}
		return sb.toString();
	}

    private String multipleLines(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("linestart");
            for (int j = 0; j < 30; j++) {
                sb.append(' ');
                sb.append("term");
                sb.append(j + i*30);
            }
            sb.append('\n');
        }
        return sb.toString();
    
    };


    @TestInfo(testType=SYSTEM)
    public void testSnippet() throws IOException, ParseException, InterruptedException {
    	indexVeryBigDoc();
    	long t = System.currentTimeMillis();
        SearchResults srs = searcher.search(new Query(new TermQuery("text","term29925"),null,null),0,10, 0, ImmutableMap.of("snippet_fields", "text"));
        srs.getResults().iterator().next();
        long dt = System.currentTimeMillis() - t;
        //this test is ignored since it fails on some machines
        //assertTrue("Snippetting took too long: " + dt + "ms. and the limit was 300ms.", dt < 300);
    }

    
    @TestInfo(testType=UNIT)
    public void testCompleteLines() throws IOException, InterruptedException {
		double timestampBoost = System.currentTimeMillis() / 1000.0;
        String docid = "docid";
        Document doc = new Document(ImmutableMap.of("text", multipleLines(), "title", "a headline!"));
		indexer.add(docid, doc, (int)timestampBoost, Maps.<Integer, Double>newHashMap());

        SearchResults srs = searcher.search(new Query(new TermQuery("text", "term29925"), null, null), 0, 10, 0, ImmutableMap.of("snippet_fields", "text", "snippet_type", "lines"));
        SearchResult sr = srs.getResults().iterator().next();
        assertNotNull("sr is null!", sr);
        assertTrue("line does not start on linestart!", sr.getField("snippet_text").startsWith("linestart")); 
        assertEquals("line does not end on a newline!", sr.getField("snippet_text").charAt(sr.getField("snippet_text").length()-1), '\n'); 
    }


    @TestInfo(testType=UNIT)
    public void testEncodesHTMLonEnd() throws IOException, InterruptedException {
		double timestampBoost = System.currentTimeMillis() / 1000.0;
        String docid = "docid";

        // have & signs before tokens, > signs between them, and < signs after.
        Document doc = new Document(ImmutableMap.of("text", "contains &&& signs >>>> and stuff <.", "title", "a headline!"));
		indexer.add(docid, doc, (int)timestampBoost, Maps.<Integer, Double>newHashMap());
    
        SearchResults srs = searcher.search(new Query(new AndQuery( new TermQuery("text", "signs"), new TermQuery("text", "stuff")), null, null), 0, 10, 0, ImmutableMap.of("snippet_fields", "text","snippet_type", "lines"));
    
        SearchResult sr = srs.getResults().iterator().next();
        assertNotNull("sr is null!", sr);
        assertTrue("signs not highlighted!", sr.getField("snippet_text").contains("<b>signs</b>"));
        assertTrue("ampersands not encoded!", sr.getField("snippet_text").contains("&amp;"));
        assertTrue("greater-than signs not encoded!", sr.getField("snippet_text").contains("&gt;"));
        assertTrue("less-than signs not encoded!", sr.getField("snippet_text").contains("&lt;"));
    }

    @TestInfo(testType=UNIT)
    public void testTokenizingChangesTokenLength() throws IOException, InterruptedException, ParseException {
        double timestampBoost = System.currentTimeMillis() / 1000.0;
        String docid = "docid";
        // \u00df is 'LATIN SMALL LETTER SHARP S'
        // ASCIIFoldingFilter converts it from 'ß' to 'ss'
        // see http://www.fileformat.info/info/unicode/char/df/index.htm
        String text = "Clown Ferdinand und der Fu\u00dfball player";
        Document doc = new Document(ImmutableMap.of("text", text));
        indexer.add(docid, doc, (int)timestampBoost, Maps.<Integer, Double>newHashMap());

        String queryText = "fussball";
        Query query = new Query(new TermQuery("text", queryText), queryText, null);

        SearchResults srs = searcher.search(query, 0, 1, 0, ImmutableMap.of("snippet_fields", "text", "snippet_type", "html"));
        SearchResult sr = srs.getResults().iterator().next();
        String snippet = sr.getField("snippet_text");
        assertNotNull("Snippet is null", snippet);
        assertTrue("Search term not highlighted", snippet.contains("<b>Fu&szlig;ball</b>"));
        assertTrue("Snippet lost space before highlighted term", snippet.contains("der "));
        assertTrue("Snippet lost space after highlighted term: " + snippet, snippet.contains(" player"));

        query = new Query(new PrefixTermQuery("text", "fu"), "fu*", null);

        srs = searcher.search(query, 0, 1, 0, ImmutableMap.of("snippet_fields", "text", "snippet_type", "html"));
        sr = srs.getResults().iterator().next();
        snippet = sr.getField("snippet_text");
        assertNotNull("Snippet is null", snippet);
        assertTrue("Search term not highlighted", snippet.contains("<b>Fu&szlig;ball</b>"));
        assertTrue("Snippet lost space before highlighted term", snippet.contains("der "));
        assertTrue("Snippet lost space after highlighted term", snippet.contains(" player"));
    }

    @TestInfo(testType=UNIT)
    public void testFetchAll() throws IOException, InterruptedException {
		double timestampBoost = System.currentTimeMillis() / 1000.0;
        String docid = "docid";
        Document doc = new Document(ImmutableMap.of("text", "this is a sample text", "title", "a headline!"));
        indexer.add(docid, doc, (int)timestampBoost, Maps.<Integer, Double>newHashMap());
        SearchResults srs = searcher.search(new Query(new TermQuery("text","sample"),null,null),0,10, 0, ImmutableMap.of("fetch_fields", "*"));
        SearchResult sr = srs.getResults().iterator().next();
        assertEquals("document data modified. fetch_fields='*' should retrieve the same data.", sr.getFields(), doc.asMap());
    }

}
