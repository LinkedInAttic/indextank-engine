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

package com.flaptor.indextank.index.term.query;

import java.util.List;

import com.flaptor.indextank.util.IdentityUnion;
import com.flaptor.indextank.util.SkippableIterable;

class OrMerger extends IdentityUnion<RawMatch> {

    @SuppressWarnings("unchecked")
	OrMerger(SkippableIterable<RawMatch> left, SkippableIterable<RawMatch> right) {
        super(left, right);
    }

    @Override
    protected boolean shouldUse(RawMatch result, List<RawMatch> originals) {
        double score = 0.0;
        for (RawMatch p : originals) {
            score += p.getScore();
        }
        result.setScore(score);
        
        return true; // every element of the union should be returned
    }
}
