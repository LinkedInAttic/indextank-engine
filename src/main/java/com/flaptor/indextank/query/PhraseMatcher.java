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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * This class implements phrase recognition. 
 * 
 * If we have the phrase: the sun is shinning
 * and we run the algorithm on: today the sun is shinning in Madrid
 * we will get: today "the sun is shinning" in Madrid
 * 
 * @author Flaptor Development Team
 */
public class PhraseMatcher {
	PhraseMatcherFSM fsm;
	
	/**
	 * Constructs the phrase matcher from a collection of phrases
	 * @param phrases the phrases to be recognized
	 */
	public void construct(Iterable<String> phrases) {
		fsm = new PhraseMatcherFSM();
		fsm.construct(phrases);
	}
	
	/**
	 * Constructs the phrase matcher from a text file. Each line will be used as a phrase 
	 * @param phraseFile
	 * @throws IOException
	 */
	public void construct(File phraseFile) throws IOException {
		List<String> list = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(phraseFile));
		while (true) {
			String line = reader.readLine();
			if (line == null) break;
			line = line.trim();
			if (line.length() == 0) continue;
			list.add(line);
		}
		construct(list);
	}
	
	/**
	 * Run the phrase recognition on the string
	 * @param s the string
	 * @return the string with phrases between ""
	 */
	public String recognize(String s)
	{
		PhraseMatcherFSM.Node initNode = fsm.getInitNode();
		PhraseMatcherFSM.Node node = initNode;
		Stack<String> stack = new Stack<String>();
		StringTokenizer tokenizer = new StringTokenizer(s);
        List<String> accumTokens = new ArrayList<String>();
		
		StringBuffer ret = new StringBuffer();
		
		while (true) {
			String token = getNext(stack, tokenizer);		
			if (token == null) break;
			
			PhraseMatcherFSM.Node nextNode = node.getNode(token);
						
			if (nextNode == null) {
				if (accumTokens.size() > 0){
					stack.push(token);
					for (int i = accumTokens.size() - 1; i > 0; i--) {
						stack.push(accumTokens.get(i));
					}
					ret.append(accumTokens.get(0));
				}
				else {
					ret.append(token);
				}
                if (hasNext(stack, tokenizer)) ret.append(" "); 					
				node = initNode;
                accumTokens.clear();
			} else {
                accumTokens.add(token);

                if (nextNode.isFinalNode()) {
           			ret.append("\"");
    				for (int i = 0; i < accumTokens.size(); ++i){
    					ret.append(accumTokens.get(i));
    					if (i < accumTokens.size()-1) ret.append(" "); 
    				}
    				ret.append("\"");
    				if (hasNext(stack, tokenizer)) ret.append(" "); 
    			
                    node = initNode;
                    accumTokens.clear();
    			}else {
    				node = nextNode;
    			}
            }
			
		}
		//dump the accumulated tokens
		for (int i = 0; i < accumTokens.size(); ++i){
			ret.append(accumTokens.get(i));
			if (i < accumTokens.size()-1) ret.append(" "); 
		}
		return ret.toString();
	}

	private static boolean hasNext(Stack<String> stack, StringTokenizer tokenizer) {
		return stack.size() > 0 || tokenizer.hasMoreElements();
	}

	private static String getNext(Stack<String> stack, StringTokenizer tokenizer) {
		if (stack.size() > 0) return stack.pop();
		else if (tokenizer.hasMoreElements()) return (String) tokenizer.nextElement();
		else return null;
	}
}
