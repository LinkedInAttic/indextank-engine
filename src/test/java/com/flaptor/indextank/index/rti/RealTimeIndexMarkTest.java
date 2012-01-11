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
import com.flaptor.indextank.index.scorer.MockScorer;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.util.TestInfo;

public class RealTimeIndexMarkTest extends IndexTankTestCase {

	private String id1 = "id1", id2 = "id2", id3 = "id3", id4 = "id4", id5 = "id5";

	@Override
	protected void setUp() throws Exception {
    	super.setUp();
	}
	
    @Override
	protected void tearDown() throws Exception {
    	super.tearDown();
	}
    
	@TestInfo(testType=UNIT)
	public void testMark() throws IOException, ParseException, InterruptedException {
		RealTimeIndex rti = new RealTimeIndex(new MockScorer(), new IndexEngineParser("text"), 4, new NoFacetingManager());
		rti.add(id1, createDocument("hola que tal 1"));
		rti.add(id2, createDocument("hola que tal 2"));
		rti.add(id3, createDocument("hola que tal 3"));
		rti.add(id4, createDocument("hola que tal 4"));

		assertResultIds("match previous", rti.getSearchSession().findMatches(query("hola AND tal"), 10, 0), id1, id2, id3, id4);
		
		rti.mark();
		
		assertResultIds("match after mark", rti.getSearchSession().findMatches(query("hola AND tal"), 10, 0), id1, id2, id3, id4);
		
		rti.add(id5, createDocument("hola que tal 5"));
		
		assertResultIds("match after mark and add", rti.getSearchSession().findMatches(query("hola AND tal"), 10, 0), id1, id2, id3, id4, id5);
		
		rti.add(id3, createDocument("hola que tul 3"));

		assertResultIds("match after mark and update (didn't match the new version)", rti.getSearchSession().findMatches(query("tul"), 10, 0), id3);
		assertResultIds("match after mark and update (didn't match the new version (2))", rti.getSearchSession().findMatches(query("3"), 10, 0), id3);
		assertResultIds("match after mark and update (matched old, updated, version)", rti.getSearchSession().findMatches(query("tal AND 3"), 10, 0));

		rti.clearToMark();
		
		assertResultIds("match after clear", rti.getSearchSession().findMatches(query("hola"), 10, 0), id3, id5);
		assertResultIds("match after clear (2)", rti.getSearchSession().findMatches(query("hola AND tal"), 10, 0), id5);
		assertResultIds("match after clear (3)", rti.getSearchSession().findMatches(query("tul"), 10, 0), id3);
		assertResultIds("match after clear (4)", rti.getSearchSession().findMatches(query("3 OR 5"), 10, 0), id3, id5);
		
		
	}
	
}
