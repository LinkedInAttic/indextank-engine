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
import com.flaptor.util.CollectionsUtil.PeekingIterator;

class DifferenceMerger extends AbstractSkippableIterable<RawMatch> {
    private final SkippableIterable<RawMatch> included;
    private final SkippableIterable<RawMatch> excluded;
    private final double boost;

    DifferenceMerger(SkippableIterable<RawMatch> left, SkippableIterable<RawMatch> right, double boost) {
        this.included = left;
        this.excluded = right;
        this.boost = boost;
    }

    @Override
    public SkippableIterator<RawMatch> iterator() {
        return new DifferenceIterator(included.iterator(), excluded.iterator(), boost);
    }

    private static class DifferenceIterator extends AbstractSkippableIterator<RawMatch> {
        private final PeekingSkippableIterator<RawMatch> included;
        private final PeekingSkippableIterator<RawMatch> excluded;
        private final double boost;

        public DifferenceIterator(SkippableIterator<RawMatch> included, SkippableIterator<RawMatch> excluded, double boost) {
            this.included = new PeekingSkippableIterator<RawMatch>(included);
            this.excluded = new PeekingSkippableIterator<RawMatch>(excluded);
            this.boost = boost;
        }
        
        private static int id(RawMatch r) {
        	return r.getRawId();
        }
        private static int id(PeekingIterator<RawMatch> i) {
        	return id(i.peek());
        }
        
        @Override
        protected RawMatch computeNext() {
            while (included.hasNext()) {
            	RawMatch candidate = included.next();

            	// check if the candidate is excluded
				while (excluded.hasNext() && id(excluded) < id(candidate)) {
					excluded.skipTo(id(candidate));
            		excluded.next(); // skip exclusions lower than candidate
            	}
				// if candidate was excluded search for another candidate
				if (excluded.hasNext() && id(candidate) == id(excluded)) {
					continue;
				}
                candidate.setScore(candidate.getBoostedScore());
                candidate.setBoost(boost);
				return candidate;
            }
            
            return endOfData();
        }

		@Override
		public void skipTo(int i) {
			this.included.skipTo(i);
			this.excluded.skipTo(i-1);
		}
    }
}
