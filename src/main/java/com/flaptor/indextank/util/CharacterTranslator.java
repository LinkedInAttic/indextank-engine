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

package com.flaptor.indextank.util;

import java.util.HashMap;

import org.apache.commons.lang3.text.translate.EntityArrays;

public class CharacterTranslator {

    public static final CharacterTranslator HTML4 = new CharacterTranslator(
            EntityArrays.BASIC_ESCAPE(),
            EntityArrays.ISO8859_1_ESCAPE(),
            EntityArrays.HTML40_EXTENDED_ESCAPE()
    );

    private HashMap<Character, CharSequence> lookupMap;

    /**
     * Merges parameter arrays into a map. 
     * Assumes that parameter arrays have a 1-char key.
     * @param lookup
     */
    public CharacterTranslator(CharSequence[][] ... lookup) {
        lookupMap = new HashMap<Character, CharSequence>();
        for(CharSequence[][] l: lookup) {
            for(CharSequence[] seq: l) {
                lookupMap.put(seq[0].charAt(0), seq[1]);
            }
        }
    }

    public void escape(StringBuilder output, String input, int start, int offset) {
        for (int i = start; i < offset; i++) {
            Character c = input.charAt(i);
            CharSequence seq = lookupMap.get(c);
            if (seq != null) {
                output.append(seq);
            } else if (c > 0x7F) {
                output.append("&#");
                output.append(Integer.toString(c, 10));
                output.append(';');
            } else {
                output.append(c);
            }
        }
    }

}
