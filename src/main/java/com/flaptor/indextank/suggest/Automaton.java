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
