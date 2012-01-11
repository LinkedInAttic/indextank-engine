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


package com.flaptor.indextank.blender;

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.util.List;
import java.util.Set;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.index.DummyPromoter;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.index.lsi.LargeScaleIndex;
import com.flaptor.indextank.index.lsi.LargeScaleIndexStub;
import com.flaptor.indextank.index.results.MockSearchResults;
import com.flaptor.indextank.index.rti.RealTimeIndex;
import com.flaptor.indextank.index.rti.RealTimeIndexStub;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.TermQuery;
import com.flaptor.indextank.scorer.DummyBoostsManager;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.indextank.suggest.NoSuggestor;
import com.flaptor.util.TestInfo;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BlenderTest extends IndexTankTestCase {

    private MockSearchResults res1,res2;
    private Blender blender;
    private Query dummyQuery;

    @Override
	protected void setUp() throws Exception {
        super.setUp();
        dummyQuery = new Query(new TermQuery("foo", "bar"),null,null);
        res1 = new MockSearchResults();
        res2 = new MockSearchResults();
        LargeScaleIndex lsi = new LargeScaleIndexStub(res1);
        RealTimeIndex rti = new RealTimeIndexStub(res2, 100);
        blender = new Blender(lsi,rti, new NoSuggestor(), new DummyPromoter(), new DummyBoostsManager());
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();
	}
	
	@TestInfo(testType=UNIT)
	public void testSomething() throws ParseException, InterruptedException {
        res1.addResult(1.0f, "1");
        res2.addResult(0.9f, "2");
        res1.addResult(0.8f, "3");
        res2.addResult(0.7f, "4");
        res2.addResult(0.6f, "5");
        res1.addResult(0.5f, "6");
        TopMatches res = blender.findMatches(dummyQuery, 10, 0);
		assertEquals("Number of results doesn't match", res.getTotalMatches(), res1.getTotalMatches()+res2.getTotalMatches());
        int i = 0;
        for (ScoredMatch r : res) {
            assertEquals("blend out of order", r.getDocId().toString(), String.valueOf(++i));
        }
	}
	
    @TestInfo(testType=UNIT)
	public void testDedupIgnoresOldVersionEvenIfItHasHigherScore() throws ParseException, InterruptedException {
        res1.addResult(1.0,"1");
        res2.addResult(0.7,"1");

        TopMatches res = blender.findMatches(dummyQuery, 10, 0);
		assertEquals("Number of results doesn't match", res.getTotalMatches(), 1);

        assertEquals("Did not choose the freshest document",0.7, Lists.newArrayList(res).get(0).getScore());

    } 
    
    @TestInfo(testType=UNIT)
	public void testPaging() throws ParseException, InterruptedException {
        res1.addResult(1.0f,"1");
        res1.addResult(1.0f,"2");
        res1.addResult(1.0f,"3");
        res1.addResult(1.0f,"4");
        res1.addResult(1.0f,"5");
        res2.addResult(0.7f,"11");
        res2.addResult(0.7f,"12");
        res2.addResult(0.7f,"13");
        res2.addResult(0.7f,"14");
        res2.addResult(0.7f,"15");

        SearchResults res = blender.search(dummyQuery, 0, 5, 0);
		assertEquals("Number of total results doesn't match", res.getMatches(), 10);

        List<SearchResult> lsr1 = Lists.newArrayList(res.getResults());
        assertEquals("Number of results doesn't match",lsr1.size(),5);

        res = blender.search(dummyQuery,5,10, 0);
		assertEquals("Number of total results doesn't match", res.getMatches(), 10);
        
        List<SearchResult> lsr2 = Lists.newArrayList(res.getResults());
        assertEquals("Number of results doesn't match",lsr1.size(),5);

        Set<String> docids = Sets.newHashSet();
        // concatenate all the doc ids, and put them on a set
        docids.addAll(Lists.newArrayList(Iterables.transform(Iterables.concat(lsr1,lsr2),new Function<SearchResult,String>(){
            public String apply(SearchResult sr){
                return sr.getDocId();
            }
        })));
        assertEquals("Number of results doesn't match",docids.size(),10);
        
    } 
    
}
