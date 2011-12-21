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

/**
 * 
 */
package com.flaptor.indextank.index.rti.inverted;

import com.flaptor.indextank.index.term.DocTermMatch;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;

class DocTermMatchList implements SkippableIterable<DocTermMatch> {
    static class DocTermMatchEntry {
        DocTermMatchEntry(int idx, int[] positions, double contextSize) {
            this.match = new DocTermMatch(idx, positions, positions.length, Math.sqrt(1.0 / contextSize));
        }
        DocTermMatch match;
        DocTermMatchEntry next;
    }
	private final DocTermMatchEntry first;
	private DocTermMatchEntry last;
	DocTermMatchList(int idx, int[] positions, int contextSize) {
		this.first = new DocTermMatchEntry(idx, positions, contextSize);
		this.last = this.first;
	}
	synchronized void add(int idx, int[] positions, int contextSize) {
		last.next = new DocTermMatchEntry(idx, positions, contextSize);
		last = last.next;
	}
	@Override
	public SkippableIterator<DocTermMatch> iterator() {
		return new AbstractSkippableIterator<DocTermMatch>() {
		    DocTermMatchEntry current = first;
			@Override 
			protected DocTermMatch computeNext() {
				if (current == null) return endOfData();
				try {
					return current.match;
				} finally {
					current = current.next;
				}
			}
			@Override
			public void skipTo(int i) {
				// no optimization here, could be implemented in 
				// the future by keeping references to larger id
				// deltas, doesn't seem necessary given the RTI's
				// size and may become complicated to do it in a
				// concurrency-friendly way
			}
		};
	}
}
