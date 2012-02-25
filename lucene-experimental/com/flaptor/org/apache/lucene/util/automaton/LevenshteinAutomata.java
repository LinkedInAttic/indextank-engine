package com.flaptor.org.apache.lucene.util.automaton;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class to construct DFAs that match a word within some edit distance.
 * <p>
 * Implements the algorithm described in:
 * Schulz and Mihov: Fast String Correction with Levenshtein Automata
 * <p>
 * @lucene.experimental
 */
public class LevenshteinAutomata {
  /** @lucene.internal */
  public static final int MAXIMUM_SUPPORTED_DISTANCE = 2;
  /* input word */
  final String input;
  final int word[];
  /* the automata alphabet. */
  final int alphabet[];

  /* the unicode ranges outside of alphabet */
  final int rangeLower[];
  final int rangeUpper[];
  int numRanges = 0;
  
  ParametricDescription descriptions[]; 
  
  /**
   * Create a new LevenshteinAutomata for some input String.
   */
  public LevenshteinAutomata(String input) {
    this.input = input;
    int length = Character.codePointCount(input, 0, input.length());
    word = new int[length];
    for (int i = 0, j = 0, cp = 0; i < input.length(); i += Character.charCount(cp)) {
      word[j++] = cp = input.codePointAt(i);
    }
    
    // calculate the alphabet
    SortedSet<Integer> set = new TreeSet<Integer>();
    for (int i = 0; i < word.length; i++)
      set.add(word[i]);
    alphabet = new int[set.size()];
    Iterator<Integer> iterator = set.iterator();
    for (int i = 0; i < alphabet.length; i++)
      alphabet[i] = iterator.next();
      
    rangeLower = new int[alphabet.length + 2];
    rangeUpper = new int[alphabet.length + 2];
    // calculate the unicode range intervals that exclude the alphabet
    // these are the ranges for all unicode characters not in the alphabet
    int lower = 0;
    for (int i = 0; i < alphabet.length; i++) {
      int higher = alphabet[i];
      if (higher > lower) {
        rangeLower[numRanges] = lower;
        rangeUpper[numRanges] = higher - 1;
        numRanges++;
      }
      lower = higher + 1;
    }
    /* add the final endpoint */
    if (lower <= Character.MAX_CODE_POINT) {
      rangeLower[numRanges] = lower;
      rangeUpper[numRanges] = Character.MAX_CODE_POINT;
      numRanges++;
    }

    descriptions = new ParametricDescription[] {
        null, /* for n=0, we do not need to go through the trouble */
        new Lev1ParametricDescription(word.length),
        new Lev2ParametricDescription(word.length),
    };
  }
  
  /**
   * Compute a DFA that accepts all strings within an edit distance of <code>n</code>.
   * <p>
   * All automata have the following properties:
   * <ul>
   * <li>They are deterministic (DFA).
   * <li>There are no transitions to dead states.
   * <li>They are not minimal (some transitions could be combined).
   * </ul>
   * </p>
   */
  public Automaton toAutomaton(int n) {
    if (n == 0)
      return BasicAutomata.makeString(input);
    
    if (n >= descriptions.length)
      return null;
    
    final int range = 2*n+1;
    ParametricDescription description = descriptions[n];
    // the number of states is based on the length of the word and n
    State states[] = new State[description.size()];
    // create all states, and mark as accept states if appropriate
    for (int i = 0; i < states.length; i++) {
      states[i] = new State();
      states[i].number = i;
      states[i].setAccept(description.isAccept(i));
    }
    // create transitions from state to state
    for (int k = 0; k < states.length; k++) {
      final int xpos = description.getPosition(k);
      if (xpos < 0)
        continue;
      final int end = xpos + Math.min(word.length - xpos, range);
      
      for (int x = 0; x < alphabet.length; x++) {
        final int ch = alphabet[x];
        // get the characteristic vector at this position wrt ch
        final int cvec = getVector(ch, xpos, end);
        int dest = description.transition(k, xpos, cvec);
        if (dest >= 0)
          states[k].addTransition(new Transition(ch, states[dest]));
      }
      // add transitions for all other chars in unicode
      // by definition, their characteristic vectors are always 0,
      // because they do not exist in the input string.
      int dest = description.transition(k, xpos, 0); // by definition
      if (dest >= 0)
        for (int r = 0; r < numRanges; r++)
          states[k].addTransition(new Transition(rangeLower[r], rangeUpper[r], states[dest]));      
    }

    Automaton a = new Automaton(states[0]);
    a.setDeterministic(true);
    // we create some useless unconnected states, and its a net-win overall to remove these,
    // as well as to combine any adjacent transitions (it makes later algorithms more efficient).
    // so, while we could set our numberedStates here, its actually best not to, and instead to
    // force a traversal in reduce, pruning the unconnected states while we combine adjacent transitions.
    //a.setNumberedStates(states);
    a.reduce();
    // we need not trim transitions to dead states, as they are not created.
    //a.restoreInvariant();
    return a;
  }
  
  /**
   * Get the characteristic vector <code>X(x, V)</code> 
   * where V is <code>substring(pos, end)</code>
   */
  int getVector(int x, int pos, int end) {
    int vector = 0;
    for (int i = pos; i < end; i++) {
      vector <<= 1;
      if (word[i] == x)
        vector |= 1;
    }
    return vector;
  }
    
  /**
   * A ParametricDescription describes the structure of a Levenshtein DFA for some degree n.
   * <p>
   * There are four components of a parametric description, all parameterized on the length
   * of the word <code>w</code>:
   * <ol>
   * <li>The number of states: {@link #size()}
   * <li>The set of final states: {@link #isAccept(int)}
   * <li>The transition function: {@link #transition(int, int, int)}
   * <li>Minimal boundary function: {@link #getPosition(int)}
   * </ol>
   */
  static abstract class ParametricDescription {
    protected final int w;
    protected final int n;
    private final int[] minErrors;
    
    ParametricDescription(int w, int n, int[] minErrors) {
      this.w = w;
      this.n = n;
      this.minErrors = minErrors;
    }
    
    /**
     * Return the number of states needed to compute a Levenshtein DFA
     */
    int size() {
      return minErrors.length * (w+1);
    };

    /**
     * Returns true if the <code>state</code> in any Levenshtein DFA is an accept state (final state).
     */
    boolean isAccept(int absState) {
      // decode absState -> state, offset
      int state = absState/(w+1);
      int offset = absState%(w+1);
      assert offset >= 0;
      return w - offset + minErrors[state] <= n;
    }

    /**
     * Returns the position in the input word for a given <code>state</code>.
     * This is the minimal boundary for the state.
     */
    int getPosition(int absState) {
      return absState % (w+1);
    }
    
    /**
     * Returns the state number for a transition from the given <code>state</code>,
     * assuming <code>position</code> and characteristic vector <code>vector</code>
     */
    abstract int transition(int state, int position, int vector);

    private final static long[] MASKS = new long[] {0x1,0x3,0x7,0xf,
                                                    0x1f,0x3f,0x7f,0xff,
                                                    0x1ff,0x3ff,0x7ff,0xfff,
                                                    0x1fff,0x3fff,0x7fff,0xffff,
                                                    0x1ffff,0x3ffff,0x7ffff,0xfffff,
                                                    0x1fffff,0x3fffff,0x7fffff,0xffffff,
                                                    0x1ffffff,0x3ffffff,0x7ffffff,0xfffffff,
                                                    0x1fffffff,0x3fffffff,0x7fffffffL,0xffffffffL,
                                                    0x1ffffffffL,0x3ffffffffL,0x7ffffffffL,0xfffffffffL,
                                                    0x1fffffffffL,0x3fffffffffL,0x7fffffffffL,0xffffffffffL,
                                                    0x1ffffffffffL,0x3ffffffffffL,0x7ffffffffffL,0xfffffffffffL,
                                                    0x1fffffffffffL,0x3fffffffffffL,0x7fffffffffffL,0xffffffffffffL,
                                                    0x1ffffffffffffL,0x3ffffffffffffL,0x7ffffffffffffL,0xfffffffffffffL,
                                                    0x1fffffffffffffL,0x3fffffffffffffL,0x7fffffffffffffL,0xffffffffffffffL,
                                                    0x1ffffffffffffffL,0x3ffffffffffffffL,0x7ffffffffffffffL,0xfffffffffffffffL,
                                                    0x1fffffffffffffffL,0x3fffffffffffffffL,0x7fffffffffffffffL};
  
    protected int unpack(long[] data, int index, int bitsPerValue) {
      final long bitLoc = bitsPerValue * index;
      final int dataLoc = (int) (bitLoc >> 6);
      final int bitStart = (int) (bitLoc & 63);
      //System.out.println("index=" + index + " dataLoc=" + dataLoc + " bitStart=" + bitStart + " bitsPerV=" + bitsPerValue);
      if (bitStart + bitsPerValue <= 64) {
        // not split
        return (int) ((data[dataLoc] >> bitStart) & MASKS[bitsPerValue-1]);
      } else {
        // split
        final int part = 64-bitStart;
        return (int) (((data[dataLoc] >> bitStart) & MASKS[part-1]) +
                      ((data[1+dataLoc] & MASKS[bitsPerValue-part-1]) << part));
      }
    }
  }
}
