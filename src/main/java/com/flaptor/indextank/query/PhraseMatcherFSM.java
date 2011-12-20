/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/
package com.flaptor.indextank.query;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/** 
 * This class represents the finite state machine for the phrase recognition
 * Nodes are connected to other nodes by a token 
 * Final nodes indicate that a phrase ends there
 * 
 * NOTE: phrases are translated and stored in lower case
 * @author Flaptor Development Team
 */
public class PhraseMatcherFSM {
	
	private Node initNode;
	
	/**
	 * Constructs the FSM from a collection of phrases
	 * 
	 * NOTE: phrases are translated to lower case
	 * 
	 * @param phrases the phrases to recognize
	 */
	public void construct(Iterable<String> phrases) {
		initNode = new Node();
		
		for(String phrase : phrases) {
			StringTokenizer tokenizer = new StringTokenizer(phrase);
			Node node = initNode;
			while(tokenizer.hasMoreElements()) {
				String token = (String) tokenizer.nextElement();
				node = node.addNode(token);
			}
			node.setFinalNode(true);
		}
	}
	
	/**
	 * @return the initial node
	 */
	public Node getInitNode() {
		return initNode;
	}
	
	/**
	 * the Node class
	 */
	static public class Node {
		private Map<String, Node> nextNodes = new HashMap<String, Node>();
		private boolean finalNode = false;
	
		/**
		 * create and add a node connected to this one through the token. 
		 * If there was a node, returns that one
		 * 
		 * @param token that connects this node
		 * @return the newly created node
		 */
		public Node addNode(String token) {
            token = token.toLowerCase();
            
            Node node = getNode(token);

			if (node == null) {
				node = new Node();
				nextNodes.put(token, node);
			}
			return node;
		}
		
		/**
		 * @param token
		 * @return the node thats connected to this one through the token
		 */
		public Node getNode(String token) {
            token = token.toLowerCase();
			if (nextNodes.containsKey(token)) return nextNodes.get(token);
			else return null;
		}

		public boolean isFinalNode() {
			return finalNode;
		}

		public void setFinalNode(boolean finalNode) {
			this.finalNode = finalNode;
		}
	}
}
