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


package com.flaptor.indextank.index.scorer;

import static com.flaptor.util.TestInfo.TestType.UNIT;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.indextank.index.scorer.CategoryMaskManager.CategoryValueInfo;
import com.flaptor.util.Pair;
import com.flaptor.util.TestInfo;


public class CategoryMaskManagerTest extends IndexTankTestCase {

    @TestInfo(testType=UNIT)
    public void testBitmapsColision() throws IOException {
        CategoryMaskManager manager = new CategoryMaskManager(new ReentrantReadWriteLock());
    	
    	String[] categories = new String[] {"type", "value", "type2", "type3", "value3", "type23", "type4", "value4", "type24", "type5", "value5", "type25", "type6", "value6", "type26"};
    	String[] values = new String[] {"1", "ONE", "TwO", "12", "ONE2", "TwO2"};
    	Set<Pair<String, CategoryValueInfo>> categoryValueInfos = new HashSet<Pair<String, CategoryValueInfo>>();
    	for (int i = 0; i < categories.length; i++) {
			for (int j = 0; j < values.length; j++) {
				CategoryValueInfo categoryValueInfo = manager.getCategoryValueInfo(categories[i], values[j]);
				assertEquals(j + 1, categoryValueInfo.getValueCode());
				categoryValueInfos.add(new Pair(categories[i],categoryValueInfo));
			}
		}
    	
    	for (Pair<String, CategoryValueInfo> categoryValueInfo : categoryValueInfos) {
			for (Pair<String, CategoryValueInfo> categoryValueInfo2 : categoryValueInfos) {
				if (!categoryValueInfo.first().equals(categoryValueInfo2.first())) {
					int[] bitmask = categoryValueInfo.last().getBitmask();
					int[] bitmask2 = categoryValueInfo2.last().getBitmask();
					
					for (int i = 0; i < Math.min(bitmask.length, bitmask2.length); i++) {
						assertEquals(0, (int)(bitmask[i] & bitmask2[i]));  
					}
				}
			}    		
		}
    }
}
