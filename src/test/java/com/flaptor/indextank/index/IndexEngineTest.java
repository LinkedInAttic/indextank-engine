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


package com.flaptor.indextank.index;

import static com.flaptor.util.TestInfo.TestType.SYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flaptor.indextank.BoostingIndexer;
import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.index.scorer.VariablesRangeFilter;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.indextank.search.DocumentSearcher;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestInfo;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.Arrays;

public class IndexEngineTest extends IndexTankTestCase {

    private IndexEngine indexEngine;
    private File tempDir;

    private static final int DOCS = 12;

    @Override
	protected void setUp() throws Exception {
        super.setUp();
        this.tempDir = FileUtil.createTempDir("indextank","testcase");
        this.indexEngine = new IndexEngine(this.tempDir, 11234, 5, false, 5, IndexEngine.SuggestValues.DOCUMENTS, null, true, "dummyCode", "TEST-environment");
	}

	private void indexTwelveDocs(BoostingIndexer indexer) {
		long timestamp = System.currentTimeMillis()/ 1000L;
        for (int i = 0; i < DOCS; i++){
            Document doc = new Document();
            doc.setField("text","term"+i+" fixed");
            doc.setField("timestamp", String.valueOf(timestamp));
            indexer.add("doc_"+i,doc, (int)timestamp, Maps.<Integer, Double>newHashMap());
            timestamp -= 1000;
        }
	}

	private List<String[]> getFacetingDocumentsData() {
		List<String[]> result = Lists.newArrayList();
		
		result.add(new String[] {"D1", "A", "BAJO", "NAH"});
		result.add(new String[] {"D2", "A", "BAJO", "SI"});
		result.add(new String[] {"D3", "A", "ALTO", "NAH"});
		result.add(new String[] {"D4", "A B", "ALTO", "SI"});
		result.add(new String[] {"D5", "A B", "BAJO", "NAH"});
		result.add(new String[] {"D6", "B", "BAJO", "SI"});
		result.add(new String[] {"D7", "B", null, "NAH"});
		result.add(new String[] {"D8", "B", null, "TERCERO"});

		return result;
	}
	
	private void indexForwardFacetedToDocuments(BoostingIndexer indexer) {
		List<String[]> facetingDocumentsData = getFacetingDocumentsData();
		
		indexFacetedDocuments(facetingDocumentsData, indexer);
	}

	private void indexBackwardsFacetedToDocuments(BoostingIndexer indexer) {
		List<String[]> facetingDocumentsData = getFacetingDocumentsData();
		
		indexFacetedDocuments(Lists.newArrayList(Iterables.reverse(facetingDocumentsData)), indexer);
	}
	
	private void indexFacetedDocuments(List<String[]> data, BoostingIndexer indexer) {
		long timestamp = System.currentTimeMillis()/ 1000L;
		for (String[] datum : data) {
            Document doc = new Document();
            doc.setField("text", datum[1]);
            doc.setField("timestamp", String.valueOf(timestamp));
            
            indexer.add(datum[0],doc, (int)timestamp, Maps.<Integer, Double>newHashMap());
            
            timestamp -= 1000;
            
            Map<String, String> categories = Maps.newHashMap();
            
            if (datum[2] != null) {
            	categories.put("PRECIO", datum[2]);
            }
            if (datum[3] != null) {
            	categories.put("TIPO", datum[3]);
            }
            
			indexer.updateCategories(datum[0], categories);
		}
	}
	
	private void indexLengthDifferentDocs(BoostingIndexer indexer, int count, int k) {
		long timestamp = System.currentTimeMillis()/ 1000L;
		for (int i = 0; i < count; i++){
			Document doc = new Document();
			String text = "foo";
			for (int j = 0; j < i*k; j++) {
				text += " bar";
			}
			doc.setField("text", text);
			doc.setField("timestamp", String.valueOf(timestamp));
			indexer.add("http://"+i,doc, (int)timestamp, Maps.<Integer, Double>newHashMap());
			timestamp -= 1000;
		}
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();
        indexEngine = null;
	}
	
    @TestInfo(testType=SYSTEM)
    public void testSwitch() throws IOException, ParseException, InterruptedException {
    	indexTwelveDocs(this.indexEngine.getIndexer());

        DocumentSearcher searcher = this.indexEngine.getSearcher();

        SearchResults srs = searcher.search(new Query(new TermQuery("text","term1"),null,null),0,10, 0);
		assertEquals("Number of historic results doesn't match", 1, srs.getMatches());
        
        srs = searcher.search(new Query(new TermQuery("text","term11"),null,null),0,10, 0);
		assertEquals("Number of real time results doesn't match", 1, srs.getMatches());

        Query query = new Query(this.indexEngine.getParser().parseQuery("term1 OR term2 OR term3 OR term4 OR term5"),null,null);
        srs = searcher.search(query,0,10, 0);
		assertEquals("Number of real time results doesn't match", 5, srs.getMatches());
	}

    @TestInfo(testType=SYSTEM)
    public void testSwitchedDuplicates() throws IOException, ParseException, InterruptedException {
    	indexLengthDifferentDocs(this.indexEngine.getIndexer(), 33, 1);
    	indexLengthDifferentDocs(this.indexEngine.getIndexer(), 33, 2);
    	
    	Query query = new Query(this.indexEngine.getParser().parseQuery("foo"),null,null);
    	SearchResults srs = this.indexEngine.getSearcher().search(query,0,100, 0);
    	
    	
    	assertEquals("Number of results doesn't match", 33, srs.getMatches());
    	Multiset<String> ids = HashMultiset.create();
    	for (SearchResult r : srs.getResults()) {
			ids.add(r.getDocId());
		}
    	assertEquals("Number of actual results doesn't match", 33, ids.size());
    	assertEquals("Number of different results doesn't match", 33, ids.elementSet().size());
    }
    
    private void checkResults(DocumentSearcher searcher, int start, int len, int[] expectedIds) throws InterruptedException {
        SearchResults srs = searcher.search(new Query(new TermQuery("text","fixed"),"fixed",null),start,len, 0);
        Set<Integer> expIds = Sets.newHashSet();
        for (int i : expectedIds) {
            expIds.add(i);
        }
        int n = 0;
        Set<Integer> actIds = Sets.newHashSet();
        for (SearchResult r : srs.getResults()) {
            String docid = r.getDocId();
            int i = Integer.parseInt(docid.substring(docid.indexOf('_')+1));
            actIds.add(i);
            n++;
        }
        assertEquals("Results page doesn't contain the expected docids", expIds, actIds);
    }

    @TestInfo(testType=SYSTEM)
    public void testPagination() throws IOException, ParseException, InterruptedException {
    	indexTwelveDocs(this.indexEngine.getIndexer());
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        checkResults(searcher,0,5,new int[]{0,1,2,3,4});
        checkResults(searcher,5,5,new int[]{5,6,7,8,9});
        checkResults(searcher,10,5,new int[]{10,11});
    }

    @TestInfo(testType=SYSTEM)
    public void testPromotion() throws IOException, ParseException,InterruptedException {
    	indexTwelveDocs(this.indexEngine.getIndexer());
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        BoostingIndexer indexer = this.indexEngine.getIndexer();
        indexer.promoteResult("doc_6", "fixed");
        checkResults(searcher,0,1,new int[]{6});
        checkResults(searcher,0,5,new int[]{0,1,2,3,6});
        checkResults(searcher,5,5,new int[]{4,5,7,8,9});
        checkResults(searcher,10,5,new int[]{10,11});
        indexer.promoteResult("doc_10", "fixed");
        checkResults(searcher,10,5,new int[]{9,11});
    }
    
    @TestInfo(testType=SYSTEM)
    public void testFacetedSearch() throws InterruptedException {
    	indexForwardFacetedToDocuments(this.indexEngine.getIndexer());
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        
        SearchResults searchResults = searcher.search(new Query(new TermQuery("text","a"),"a",null), 0, 10, 0);
        
        Map<String, Multiset<String>> facets = searchResults.getFacets();
        
        //System.out.println("Matches: " + searchResults.getMatches());
        //System.out.println(facets);
        
        assertEquals(2, facets.keySet().size());
        
        Multiset<String> precioFacet = facets.get("PRECIO");
     
        assertEquals(2, precioFacet.elementSet().size());
        assertEquals(2, precioFacet.count("ALTO"));
        assertEquals(3, precioFacet.count("BAJO"));
        
        Multiset<String> tipoFacet = facets.get("TIPO");
        
        assertEquals(2, tipoFacet.elementSet().size());
        assertEquals(3, tipoFacet.count("NAH"));
        assertEquals(2, tipoFacet.count("SI"));
        
        
        searchResults = searcher.search(new Query(new TermQuery("text","b"),"b",null), 0, 10, 0);
        
        facets = searchResults.getFacets();
        
        //System.out.println("Matches: " + searchResults.getMatches());
        //System.out.println(facets);
        
        assertEquals(2, facets.keySet().size());
        
        precioFacet = facets.get("PRECIO");
     
        assertEquals(2, precioFacet.elementSet().size());
        assertEquals(1, precioFacet.count("ALTO"));
        assertEquals(2, precioFacet.count("BAJO"));
        
        tipoFacet = facets.get("TIPO");
        
        assertEquals(3, tipoFacet.elementSet().size());
        assertEquals(2, tipoFacet.count("NAH"));
        assertEquals(2, tipoFacet.count("SI"));
        assertEquals(1, tipoFacet.count("TERCERO"));
    }
    
    @TestInfo(testType=SYSTEM)
    public void testBackwardsFacetedSearch() throws InterruptedException {
    	indexBackwardsFacetedToDocuments(this.indexEngine.getIndexer());
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        
        SearchResults searchResults = searcher.search(new Query(new TermQuery("text","a"),"a",null), 0, 10, 0);
        
        Map<String, Multiset<String>> facets = searchResults.getFacets();
        
        //System.out.println("Matches: " + searchResults.getMatches());
        //System.out.println(facets);
        
        assertEquals(2, facets.keySet().size());
        
        Multiset<String> precioFacet = facets.get("PRECIO");
     
        assertEquals(2, precioFacet.elementSet().size());
        assertEquals(2, precioFacet.count("ALTO"));
        assertEquals(3, precioFacet.count("BAJO"));
        
        Multiset<String> tipoFacet = facets.get("TIPO");
        
        assertEquals(2, tipoFacet.elementSet().size());
        assertEquals(3, tipoFacet.count("NAH"));
        assertEquals(2, tipoFacet.count("SI"));
        
        
        searchResults = searcher.search(new Query(new TermQuery("text","b"),"b",null), 0, 10, 0);
        
        facets = searchResults.getFacets();
        
        //System.out.println("Matches: " + searchResults.getMatches());
        //System.out.println(facets);
        
        assertEquals(2, facets.keySet().size());
        
        precioFacet = facets.get("PRECIO");
     
        assertEquals(2, precioFacet.elementSet().size());
        assertEquals(1, precioFacet.count("ALTO"));
        assertEquals(2, precioFacet.count("BAJO"));
        
        tipoFacet = facets.get("TIPO");
        
        assertEquals(3, tipoFacet.elementSet().size());
        assertEquals(2, tipoFacet.count("NAH"));
        assertEquals(2, tipoFacet.count("SI"));
        assertEquals(1, tipoFacet.count("TERCERO"));
    }


    private void checkSearchResults(Iterable<SearchResult> results, String[] strings) {
		int i = 0;
		for (SearchResult searchResult : results) {
			assertEquals("Wrong results", strings[i], searchResult.getDocId().toString());
			i++;
		}
	}

    @TestInfo(testType=SYSTEM)
    public void testFacetFiltering() throws InterruptedException, ParseException {
    	indexBackwardsFacetedToDocuments(this.indexEngine.getIndexer());
        DocumentSearcher searcher = this.indexEngine.getSearcher();
        
        Multimap<String, String> categoriesFilter = HashMultimap.create();
        
        categoriesFilter.put("PRECIO", "BAJO");
        
		SearchResults searchResults = searcher.search(new Query(indexEngine.getParser().parseQuery("a OR b"),"a OR b", null, categoriesFilter, VariablesRangeFilter.NO_FILTER), 0, 10, 0);
		
		int matches = searchResults.getMatches();
		Map<String, Multiset<String>> facets = searchResults.getFacets();
		//System.out.println(matches);
		//System.out.println(searchResults.getResults());
		//System.out.println(searchResults.getFacets());
		
		assertEquals(4, matches);
		checkSearchResults(searchResults.getResults(), new String[] {"D6", "D5", "D2", "D1"});
		
		categoriesFilter = HashMultimap.create();
		
		categoriesFilter.put("TIPO", "NAH");
		categoriesFilter.put("TIPO", "TERCERO");
		
		searchResults = searcher.search(new Query(indexEngine.getParser().parseQuery("a OR b"),"a OR b", null, categoriesFilter, VariablesRangeFilter.NO_FILTER), 0, 10, 0);
		
		matches = searchResults.getMatches();
		//System.out.println(matches);
		//System.out.println(searchResults.getResults());
		//System.out.println(searchResults.getFacets());

		assertEquals(5, matches);
		checkSearchResults(searchResults.getResults(), new String[] {"D8", "D7", "D5", "D3", "D1"});

		categoriesFilter = HashMultimap.create();
		
		categoriesFilter.put("TIPO", "NAH");
		categoriesFilter.put("TIPO", "TERCERO");
		categoriesFilter.put("PRECIO", "BAJO");
		
		searchResults = searcher.search(new Query(indexEngine.getParser().parseQuery("a OR b"),"a OR b", null, categoriesFilter, VariablesRangeFilter.NO_FILTER), 0, 10, 0);
		
		matches = searchResults.getMatches();
		//System.out.println(matches);
		//System.out.println(searchResults.getResults());
		//System.out.println(searchResults.getFacets());
		
		assertEquals(2, matches);
		checkSearchResults(searchResults.getResults(), new String[] {"D5", "D1"});

    }
    
}
