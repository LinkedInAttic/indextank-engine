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


package com.flaptor.indextank.index.rti;


import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.IOException;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.scorer.MockScorer;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.util.TestInfo;

public class RealTimeIndexQueriesTest extends IndexTankTestCase {

	private String id1 = "id1", id2 = "id2", id3 = "id3", id4 = "id4";
    private RealTimeIndex rti;
	private QueryMatcher session;

	@Override
	protected void setUp() throws Exception {
    	super.setUp();
    	rti = new RealTimeIndex(new MockScorer(), new IndexEngineParser("text"), 4, new NoFacetingManager());
    	rti.add(id1, createDocument("hola que tal 1"));
    	rti.add(id2, createDocument("hola que tal 2"));
    	rti.add(id3, createDocument("hola que tal 3"));
    	rti.add(id4, createDocument("hola que tal 4"));
    	session = rti.getSearchSession();
	}
	
    @Override
	protected void tearDown() throws Exception {
    	super.tearDown();
	}
    
	@TestInfo(testType=UNIT)
	public void testTerm() throws IOException, ParseException, InterruptedException {
		assertResultIds("term query failed", session.findMatches(query("hola"), 10, 0), id1, id2, id3, id4);
		assertResultIds("term query failed (2)", session.findMatches(query("halo"), 10, 0));
		assertResultIds("term query failed (3)", session.findMatches(query("3"), 10, 0), id3);
	}
	
	@TestInfo(testType=UNIT)
	public void testAnd() throws IOException, ParseException, InterruptedException {
		assertResultIds("and query failed", session.findMatches(query("hola AND tal"), 10, 0), id1, id2, id3, id4);
		assertResultIds("and query failed (2)", session.findMatches(query("hola AND nunca"), 10, 0));
		assertResultIds("and query failed (3)", session.findMatches(query("hola AND 2"), 10, 0), id2);
	}
	
	@TestInfo(testType=UNIT)
	public void testOr() throws IOException, ParseException, InterruptedException {
		assertResultIds("or query failed", session.findMatches(query("nada OR tal"), 10, 0), id1, id2, id3, id4);
		assertResultIds("or query failed (2)", session.findMatches(query("nada OR nunca"), 10, 0));
		assertResultIds("or query failed (3)", session.findMatches(query("nada OR 4"), 10, 0), id4);
	}
	@TestInfo(testType=UNIT)
	public void testPhrase() throws IOException, ParseException, InterruptedException {
		assertResultIds("phrase query failed", session.findMatches(query("\"hola que\""), 10, 0), id1, id2, id3, id4);
		assertResultIds("phrase query failed (2)", session.findMatches(query("\"que tal 3\""), 10, 0), id3);
		assertResultIds("phrase query failed (3)", session.findMatches(query("\"tal que 3\""), 10, 0));
	}
	@TestInfo(testType=UNIT)
	public void testDifference() throws IOException, ParseException, InterruptedException {
		assertResultIds("phrase query failed", session.findMatches(query("hola -que"), 10, 0));
		assertResultIds("phrase query failed (2)", session.findMatches(query("tal -3"), 10, 0), id1, id2, id4);
		assertResultIds("phrase query failed (3)", session.findMatches(query("tal -nada"), 10, 0), id1, id2, id3, id4);
	}
	@TestInfo(testType=UNIT)
	public void testComplex() throws IOException, ParseException, InterruptedException {
		assertResultIds("complex query failed", session.findMatches(query("\"hola que\" AND 3"), 10, 0), id3);
		assertResultIds("complex query failed (2)", session.findMatches(query("\"hola que\" AND (3 OR 2)"), 10, 0), id2, id3);
		assertResultIds("complex query failed (3)", session.findMatches(query("\"hola que tal\" -3"), 10, 0), id1, id2, id4);
		assertResultIds("complex query failed (4)", session.findMatches(query("(hola AND 3) OR (hola AND 4)"), 10, 0), id3, id4);
		assertResultIds("complex query failed (5)", session.findMatches(query("((hola AND 3) OR (hola AND 4)) AND 4"), 10, 0), id4);
		assertResultIds("complex query failed (6)", session.findMatches(query("((hola AND 3) OR (hola AND 4)) -4"), 10, 0), id3);
		assertResultIds("complex query failed (7)", session.findMatches(query("((hola AND 3) OR (hola AND 4)) AND (2 OR 3)"), 10, 0), id3);
	}
	
}
