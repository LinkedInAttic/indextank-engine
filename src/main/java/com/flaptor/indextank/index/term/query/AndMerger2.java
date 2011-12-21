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

import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.PeekingSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;

final class AndMerger2 extends AbstractSkippableIterable<RawMatch> {
	
	AndMerger2(SkippableIterable<RawMatch> l, SkippableIterable<RawMatch> r, double boost) {
	    this.left = l;
	    this.right = r;
        this.boost = boost;
    }

    private final SkippableIterable<RawMatch> left;
    private final SkippableIterable<RawMatch> right;
    private final double boost;

    @Override
    public SkippableIterator<RawMatch> iterator() {
        return new AbstractSkippableIterator<RawMatch>() {
            private PeekingSkippableIterator<RawMatch> it1 = new PeekingSkippableIterator<RawMatch>(left.iterator());
            private PeekingSkippableIterator<RawMatch> it2 = new PeekingSkippableIterator<RawMatch>(right.iterator());
            
            @Override
            protected RawMatch computeNext() {
                while (true) {
                    if (!it1.hasNext()) return endOfData();
                    if (!it2.hasNext()) return endOfData();
                    
                    RawMatch t1 = it1.peek();
                    RawMatch t2 = it2.peek();
                    int c = t1.compareTo(t2);
                    
                    if (c == 0) {
                        it1.next();
                        it2.next();
                        //System.out.println("AND: [S:"+t1.getScore()+", B:"+t1.getBoost()+", N:"+t1.getNorm()+"] + [S:"+t2.getScore()+", B:"+t2.getBoost()+", N:"+t2.getNorm()+"] = [S:"+(t1.getBoostedScore()+t2.getBoostedScore())+", B:"+boost+", N:"+(t1.getBoostedNorm()+t2.getBoostedNorm())+"]");
                        t1.setScore(t1.getBoostedScore() + t2.getBoostedScore());
                        t1.setBoost(boost);
                        return t1;
                    } else if (c < 0) {
                        it1.skipTo(t2.getRawId());
                        it1.next();
                    } else {
                        it2.skipTo(t1.getRawId());
                        it2.next();
                    }
                }
            }
            
            @Override
            public void skipTo(int i) {
                it1.skipTo(i);
                it2.skipTo(i);
            }
        };
    }

}
