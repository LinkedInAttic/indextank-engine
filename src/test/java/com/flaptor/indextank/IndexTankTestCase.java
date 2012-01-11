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

import java.util.List;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.TopMatches;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.util.TestCase;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public abstract class IndexTankTestCase extends TestCase {

	private IndexEngineParser parser;

	@Override
	protected void setUp() throws Exception {
		parser = new IndexEngineParser("text");
	}
	
	public Query query(String queryStr) throws ParseException {
		return new Query(parser.parseQuery(queryStr),queryStr,null);
	}
	
	private static final Function<ScoredMatch, String> GET_ID_FUNCTION = new Function<ScoredMatch, String>() {
		@Override
		public String apply(ScoredMatch r) {
			return r.getDocId().toString();
		}
	};
	
	public static void assertSameSets(Iterable<?> a, Iterable<?> b) {
		assertTrue(Sets.newHashSet(a).equals(Sets.newHashSet(b)));
	}
	public static void assertSameSets(String message, Iterable<?> expected, Iterable<?> actaul) {
		assertEquals(message, Sets.newHashSet(expected),  Sets.newHashSet(actaul));
	}
	
	public static Iterable<String> toDocIds(TopMatches result) {
		return Iterables.transform(result, GET_ID_FUNCTION);
	}

	public static Document createDocument(String text) {
		return new Document(ImmutableMap.of("text", text));
	}

	public static void assertResultIds(String message, TopMatches results, String... ids) {
		List<String> expected = list(ids);
		Iterable<String> actual = toDocIds(results);
		assertSameSets(message, expected, actual);
	}

	public static <T> List<T> list(T... ts) {
        return ImmutableList.copyOf(ts);
	}

}
