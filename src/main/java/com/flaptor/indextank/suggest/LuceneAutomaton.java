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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


import com.flaptor.indextank.util.AbstractIterable;
import com.flaptor.org.apache.lucene.util.automaton.Automaton;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;


public class LuceneAutomaton extends com.flaptor.indextank.suggest.Automaton {
	private static class State implements com.flaptor.indextank.suggest.Automaton.State {
		private com.flaptor.org.apache.lucene.util.automaton.State innerState; 
		
		public State(com.flaptor.org.apache.lucene.util.automaton.State innerState) {
			this.innerState = innerState;
		}
		
		@Override
		public Iterable<Transition> getTransitions() {
			final List<com.flaptor.org.apache.lucene.util.automaton.Transition> innerTransitions =
			    Ordering.from(new Comparator<com.flaptor.org.apache.lucene.util.automaton.Transition>() {
	                @Override
	                public int compare(com.flaptor.org.apache.lucene.util.automaton.Transition o1, com.flaptor.org.apache.lucene.util.automaton.Transition o2) {
	                    return o1.getMin() - o2.getMin();
	                }
	            }).sortedCopy(innerState.getTransitions());
			
			return new AbstractIterable<Transition>() {
				@Override
				public Iterator<Transition> iterator() {
					final Iterator<com.flaptor.org.apache.lucene.util.automaton.Transition> iterator = innerTransitions.iterator();
					return new AbstractIterator<Transition>() {
						private com.flaptor.org.apache.lucene.util.automaton.Transition currentTransition = null;
						private int lastSymbol;
						
						@Override
						protected Transition computeNext() {
							
							do {
								
								if (currentTransition == null || lastSymbol == currentTransition.getMax()) {
									if (iterator.hasNext()) {
										currentTransition = iterator.next();
										lastSymbol = currentTransition.getMin() - 1;
									} else {
										return endOfData();
									}
								}
								
							lastSymbol++;
							
							} while (!isAcceptable(lastSymbol));
							
							final int currentSymbol = lastSymbol;
							
							Transition edge = new Transition() {
								@Override
								public char getSymbol() {
									return (char)currentSymbol;
								}
								@Override
								public com.flaptor.indextank.suggest.Automaton.State getState() {
									return new LuceneAutomaton.State(currentTransition.getDest());
								}
							};
							
							return edge;
						}

						private boolean isAcceptable(int lastSymbol) {
							return lastSymbol <= 127 && Character.isLetterOrDigit(lastSymbol) && Character.isLowerCase(lastSymbol);
						}
					};
				}
			};
		}

		@Override
		public boolean isAccept() {
			return innerState.isAccept();
		}

		@Override
		public com.flaptor.indextank.suggest.Automaton.State step(char symbol) {
			com.flaptor.org.apache.lucene.util.automaton.State step = innerState.step((int)symbol);
			if (step != null) {
				return new State(step);
			} else {
				return null;
			}
		}
	}
	
	public static LuceneAutomaton adapt(Automaton automaton) {
		return new LuceneAutomaton(new State(automaton.getInitialState()));
	}
	
	private LuceneAutomaton(State startState) {
		super(startState);
	}

}
