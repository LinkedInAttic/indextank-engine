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


package com.flaptor.indextank.index.storage;

import java.io.IOException;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.storage.InMemoryStorage;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestInfo.TestType;

public class InMemoryStorageTest extends TestCase {
	
	private InMemoryStorage storage;
    private String text;

	@Override
	protected void setUp() throws Exception {
        super.setUp();
		storage = new InMemoryStorage(FileUtil.createTempDir("testInMemoryStorage", ".tmp"), false);
		text = "The main doubt we have regarding the add-on is how should we support the service for testing environments. Currently, an IndexTank account allows you to create several indexes, either via the control panel or the api. The indextank client needs to be configured with the index code to use. Would it be ok for each Heroku user to create his own indexes and configure them for each environment on their own? or should we provide that out-of-the-box somehow";
	}
	
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();        
    }
    
    @TestInfo(testType=TestType.UNIT)
    public void testTextOnlyDocument() throws InterruptedException, IOException {
        InMemoryStorage storage = new InMemoryStorage(FileUtil.createTempDir("testInMemoryStorage", ".tmp"), false);
        Document doc1 = new Document();
        doc1.setField("text", text);
        storage.saveDocument("a", doc1);
        Document dd1 = storage.getDocument("a");
        assertEquals("document retrieved didn't match document stored", doc1, dd1);
    }
    @TestInfo(testType=TestType.UNIT)
    public void testNonTextOnlyDocument() throws InterruptedException {
        Document doc2 = new Document();
        doc2.setField("nottext", text);
        storage.saveDocument("b", doc2);
        Document dd2 = storage.getDocument("b");
        assertEquals("document retrieved didn't match document stored", doc2, dd2);
    }
    @TestInfo(testType=TestType.UNIT)
    public void testMixedDocument() throws InterruptedException {
        Document doc3 = new Document();
        doc3.setField("text", text);
        doc3.setField("f1", "v1");
        doc3.setField("f2", "v2");
        storage.saveDocument("c", doc3);
        Document dd3 = storage.getDocument("c");
        assertEquals("document retrieved didn't match document stored", doc3, dd3);
    }
    @TestInfo(testType=TestType.UNIT)
    public void testEmptyTextDocument() throws InterruptedException {
        Document doc3 = new Document();
        doc3.setField("text", "");
        doc3.setField("f1", "v1");
        doc3.setField("f2", "v2");
        storage.saveDocument("c", doc3);
        Document dd3 = storage.getDocument("c");
        assertEquals("document retrieved didn't match document stored", doc3, dd3);
    }

}
