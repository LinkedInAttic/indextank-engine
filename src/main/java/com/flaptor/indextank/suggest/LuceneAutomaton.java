package com.flaptor.indextank.suggest;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.util.automaton.Automaton;

import com.flaptor.indextank.util.AbstractIterable;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;


public class LuceneAutomaton extends com.flaptor.indextank.suggest.Automaton {
	private static class State implements com.flaptor.indextank.suggest.Automaton.State {
		private org.apache.lucene.util.automaton.State innerState; 
		
		public State(org.apache.lucene.util.automaton.State innerState) {
			this.innerState = innerState;
		}
		
		@Override
		public Iterable<Transition> getTransitions() {
			final List<org.apache.lucene.util.automaton.Transition> innerTransitions =
			    Ordering.from(new Comparator<org.apache.lucene.util.automaton.Transition>() {
	                @Override
	                public int compare(org.apache.lucene.util.automaton.Transition o1, org.apache.lucene.util.automaton.Transition o2) {
	                    return o1.getMin() - o2.getMin();
	                }
	            }).sortedCopy(innerState.getTransitions());
			
			return new AbstractIterable<Transition>() {
				@Override
				public Iterator<Transition> iterator() {
					final Iterator<org.apache.lucene.util.automaton.Transition> iterator = innerTransitions.iterator();
					return new AbstractIterator<Transition>() {
						private org.apache.lucene.util.automaton.Transition currentTransition = null;
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
			org.apache.lucene.util.automaton.State step = innerState.step((int)symbol);
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
