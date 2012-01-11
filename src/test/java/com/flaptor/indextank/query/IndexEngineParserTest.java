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


package com.flaptor.indextank.query;

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.util.Iterator;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.util.TestInfo;

public class IndexEngineParserTest extends IndexTankTestCase {

    private IndexEngineParser parser;

	@Override
	protected void setUp() throws Exception {
        super.setUp();
        parser = new IndexEngineParser("text");
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();        
	}
	
	@TestInfo(testType=UNIT)
    public void testBasicQuery() throws Exception {
        //Very basic test, I just want to make sure it doesn't throw an exception.
        parser.parseQuery("foo bar AND (foobar OR barfoo) -foofoo +\"barbar\"");
    }

	@TestInfo(testType=UNIT)
    public void testUnevenQuotes() {
        try {
            parser.parseQuery("\"");
            fail();
        } catch (ParseException e) {
            //OK
        }
    }

	@TestInfo(testType=UNIT)
    public void testEmptyPhrase() {
        try {
            parser.parseQuery("\"\"");
            fail();
        } catch (ParseException e) {
            //OK
        }
    }

	@TestInfo(testType=UNIT)
	public void testTokenizer() {
		Iterator<AToken> tokens = parser.parseDocumentField("field", "word1 word2 word3. word4 5.6");
		
		int initialPos = checkFirstToken(tokens.next(), "word1");
		checkToken(tokens.next(), "word2", initialPos+1);
		checkToken(tokens.next(), "word3", initialPos+2);
		checkToken(tokens.next(), "word4", initialPos+3);
		checkToken(tokens.next(), "5.6", initialPos+4);
	}
	
	private int checkFirstToken(AToken token, String expectedText) {
		assertEquals(expectedText, token.getText());
		return token.getPosition();
	}
	private void checkToken(AToken token, String expectedText, int expectedPosition) {
		assertEquals(expectedText, token.getText());
		assertEquals(expectedPosition, token.getPosition());
		
	}
	
}
