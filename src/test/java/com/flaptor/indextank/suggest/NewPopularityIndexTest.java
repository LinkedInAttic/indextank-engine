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

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestInfo;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class NewPopularityIndexTest extends IndexTankTestCase {

    private NewPopularityIndex index;

    private String[] words = {  "hola",
                                "chau",
                                "chacinado",
                                "chacinado",
                                "chancho",
                                "chanchon",
                                "chancha",
                                "chanchito",
                                "chanchito",
                                "chanchitos",
                                "chanchos",
                                "chanchos",
                                "chanchos",
                                "chanchas",
                                "chapalmalal",
                                "chapelco",
                                "chapelco",
                                "chapelco",
                                "chapelco",
                                "charco",
                                "chaucha",
                                "chino",
                                "chino",
                                "chino",
                                "chino",
                                "chino",
                                "choique",
                                };
    @Override
	protected void setUp() throws Exception {
        super.setUp();
        Random r = new Random(2);
        List<String> wordslist = Lists.newArrayList(words);
        Collections.shuffle(wordslist, r);
        index = new NewPopularityIndex(FileUtil.createTempDir("NewPopularityIndexTest", ".tmp"));
        for (String s : wordslist) {
            index.addTerm(s);
        }
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();
	}
	
	@TestInfo(testType=UNIT)
	public void testSomething() {
        assertEquals("hola", index.getMostPopular("hol").get(0));
        assertEquals("chapelco", index.getMostPopular("cha").get(0));
        assertEquals("chino", index.getMostPopular("ch").get(0));
        assertEquals("chapelco", index.getMostPopular("ch").get(1));
        assertEquals("chanchos", index.getMostPopular("ch").get(2));
        assertEquals("chanchos", index.getMostPopular("chan").get(0));
        assertEquals("chanchito", index.getMostPopular("chan").get(1));
        assertTrue(ImmutableSet.of("chancho", "chanchon", "chancha", "chanchas").contains(index.getMostPopular("chan").get(2)));
	}
	
	@TestInfo(testType=UNIT)
	public void testSerialization() throws Exception {
        File f = FileUtil.createTempDir("NewPopularityIndexTest", ".tmp");
        index = new NewPopularityIndex(f);
        for (String s : words) {
            index.addTerm(s);
        }
        index.dump();
        index = new NewPopularityIndex(f);
        //Now, we execute the basic test
        testSomething();
	}

    @TestInfo(testType=UNIT)
    public void testGetCount() throws Exception {
        for (String word: words){
            List<String> tmpList = Lists.newArrayList(words);
            tmpList.retainAll(Lists.newArrayList(word));
            assertEquals(tmpList.size(), index.getCount(word));
        } 
    } 
}
