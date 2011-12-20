package com.flaptor.indextank.suggest;

import java.util.Set;
import java.util.Stack;

import com.google.common.collect.Sets;

public class Automaton {
	private final State startState;
	
	public static interface Transition {
		public char getSymbol();
		public State getState();
	}

	public interface State {
		/**
		 * @return transitions sorted by transition symbol
		 */
		public Iterable<Transition> getTransitions();
		public State step(char symbol);
		public boolean isAccept();
	}

	public Automaton(State startState) {
		this.startState = startState;
	}
	
	public State getStartState() {
		return startState;
	}
	
	@SuppressWarnings("unchecked")
	public static Set<String> intersectPaths(Automaton small, Automaton big){
		class Element {
			String path;
			State smallState;
			State bigState;

			public Element(String path, State smallState, State bigState) {
				this.path = path;
				this.smallState = smallState;
				this.bigState = bigState;
			}
		}
		
		Set<String> results = Sets.newHashSet();
		Stack<Element> stack = new Stack<Element>();
		stack.push(new Element("", small.getStartState(), big.getStartState()));
		
		
		while (!stack.isEmpty()) {
			Element element = stack.pop();
			
			Iterable<Transition> transitions = element.smallState.getTransitions();
			
			for (Transition smallTransition : transitions) {
				char symbol = smallTransition.getSymbol();
				State smallState = smallTransition.getState();
				State bigState = element.bigState.step(symbol);
				
				if (bigState != null) {
					String path = element.path + symbol;
					stack.push(new Element(path, smallState, bigState));
					
					if (smallState.isAccept() && bigState.isAccept()) {
						results.add(path);
					}
				}
				
			}
		}
		
		return results;
	}

}
