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

package com.flaptor.indextank.index.lsi.term;

import java.io.IOException;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.Similarity;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.ScoredMatch;
import com.flaptor.indextank.index.term.DocTermMatch;
import com.flaptor.indextank.index.term.TermMatcher;
import com.flaptor.indextank.index.term.query.RawMatch;
import com.flaptor.indextank.util.AbstractSkippableIterable;
import com.flaptor.indextank.util.AbstractSkippableIterator;
import com.flaptor.indextank.util.SkippableIterable;
import com.flaptor.indextank.util.SkippableIterator;
import com.flaptor.util.Execute;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

public class IndexReaderTermMatcher implements TermMatcher {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	private final IndexReader reader;
	private Term payloadTerm;

    public IndexReaderTermMatcher(IndexReader reader, Term payloadTerm) {
		Preconditions.checkNotNull(reader);
		Preconditions.checkNotNull(payloadTerm);
        this.reader = reader;
        this.payloadTerm = payloadTerm;
    }


	@Override
    public SkippableIterable<DocTermMatch> getMatches(final String field, String termText) {
	    final Term term = new Term(field, termText);
	    return getDocTermIterator(term);
    }

    @Override
    public NavigableMap<String, SkippableIterable<DocTermMatch>> getMatches(String field, String termFrom, String termTo) {
        TermEnum terms = null;
        NavigableMap<String, SkippableIterable<DocTermMatch>> result = new TreeMap<String, SkippableIterable<DocTermMatch>>();
        try {
            terms = reader.terms(new Term(field, termFrom));
            Term rightBoundary = new Term(field, termTo);
            int numberOfTerms = 0;

            if (terms.term() != null) {
                do {
                    Term term = terms.term();
                    if (term.compareTo(rightBoundary) >= 0) {
                        break;
                    }
    
                    SkippableIterable<DocTermMatch> docTermIterator = getDocTermIterator(term);
                    result.put(term.text(), docTermIterator);
    
                    numberOfTerms++;
                    if (numberOfTerms >= 1000) {
                        break;
                    }
                    
                } while (terms.next());
            }
            
            return result;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (terms != null) {
                Execute.close(terms);
            }
        }
    }
	    
    private SkippableIterable<DocTermMatch> getDocTermIterator(final Term term) {
        return new AbstractSkippableIterable<DocTermMatch>() {
			@Override
			public SkippableIterator<DocTermMatch> iterator() {
			    
				try {
					return new AbstractSkippableIterator<DocTermMatch>() {
						final TermPositions tp = reader.termPositions(term);
						private Integer nextId = null;
						DocTermMatch m = null;
						
						private DocTermMatch match(int rawId, int freq, float norm) throws IOException {
						    if (m == null) {
						        m = new DocTermMatch(rawId, new int[freq], freq, norm);
						    } else {
						        m.setRawId(rawId);
						        m.setPositionsLength(freq);
						        m.setNormalization(norm);
						    }
						    int[] positions = m.getPositions();
						    if (freq > positions.length) {
						        positions = new int[freq];
						        m.setPositions(positions);
						    }
						    for (int i = 0; i < freq; i++) {
                                m.getPositions()[i] = tp.nextPosition();
                            }
						    return m;
						}

						@Override
						protected DocTermMatch computeNext() {
							try {
								if (nextId == null ? tp.next() : tp.skipTo(nextId)) {
								//if (tp.next()) {
									int rawId = tp.doc();
									nextId  = rawId + 1;
									int freq = tp.freq();
									float norm = Similarity.decodeNorm(reader.norms(term.field())[rawId]);
									return match(rawId, freq, norm);
								} else {
									return endOfData();
								}
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						@Override
						public void skipTo(int i) {
							nextId = i;
						}
					};
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
    }

	@Override
	public boolean hasChanges(DocId docid) {
		throw new UnsupportedOperationException();
	}


	@Override
	public Iterable<ScoredMatch> decode(Iterable<RawMatch> rawMatches, final double boostedNorm) {
		try {
			final TermPositions payloads = reader.termPositions(payloadTerm);
			return Iterables.transform(rawMatches, new Function<RawMatch, ScoredMatch>() {
			    private byte[] data = new byte[256];
			    private ScoredMatch match = new ScoredMatch(0, new DocId(data, 0, 0));
				@Override
				public ScoredMatch apply(RawMatch rawMatch) {
					int rawId = rawMatch.getRawId();
					try {
						if (payloads.skipTo(rawId) && payloads.doc() == rawId) {
							payloads.nextPosition();
							int size = payloads.getPayloadLength();
							if (size > data.length) {
							    data = new byte[size];
							}
							payloads.getPayload(data, 0);
							match.getDocId().update(data, 0, size);
							match.setScore(rawMatch.getBoostedScore() / boostedNorm);
							return match;
						} else {
							throw new IllegalArgumentException("rawId:" + rawId + " doesn't exist. Payloads.doc():" + payloads.doc());
						}
					} catch (IOException e) {
                        try {
                            org.apache.lucene.document.Document d = reader.document(rawId);
                            logger.error("Document without payload: " + d.toString());
                        } catch (Exception ee) {
                            logger.error(ee);
                        }
						throw new RuntimeException(e);
					}
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


    @Override
    public SkippableIterable<Integer> getAllDocs() {
        return new AbstractSkippableIterable<Integer>() {
            @Override
            public SkippableIterator<Integer> iterator() {
                return new AbstractSkippableIterator<Integer>() {
                    int current = -1;
                    @Override
                    public void skipTo(int i) {
                        current = i-1;
                    }
                    
                    @Override
                    protected Integer computeNext() {
                        while (++current < reader.maxDoc()) {
                            if (!reader.isDeleted(current)) {
                                return current;
                            }
                        }
                        return endOfData();
                    }
                };
            }
        };
    }
    
}
