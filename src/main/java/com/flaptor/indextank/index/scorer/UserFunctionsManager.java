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

package com.flaptor.indextank.index.scorer;

import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.scorer.parser.ParserException;
import com.flaptor.indextank.index.scorer.parser.ScoreFormulaParser;
import com.flaptor.indextank.rpc.IndextankException;
import com.flaptor.util.Execute;
import com.google.common.collect.Maps;

public class UserFunctionsManager {
 	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private Scorer scorer = null;
    private Map<Integer, String> definitions = null;

    public UserFunctionsManager(Scorer scorer) {
        this.scorer = scorer;
        this.definitions = Maps.newHashMap();
    }

    /**
	 * Add a scoring function definition for the given index.
	 *
	 * @param functionIndex scoring function index
	 * @param definition scoring function definition
	 */
    public void addFunction(Integer functionIndex, String definition) throws Exception {
        try {
            ScoreFunction function = ScoreFormulaParser.parseFormula(functionIndex, definition);
            scorer.putScoringFunction(functionIndex, function);
            definitions.put(functionIndex, definition);
        } catch (ParserException e) {
            throw new IndextankException("Syntax Error");
        }
    }

    public void delFunction(Integer functionIndex) {
        scorer.removeScoringFunction(functionIndex);
        definitions.remove(functionIndex);
    }

    public Map<Integer,String> listFunctions() {
        return Collections.unmodifiableMap(definitions);
    }

    public Map<String, String> getStats() {
        Map<String, String> stats = Maps.newHashMap();
        stats.put("functions_count", String.valueOf(definitions.size()));
        return stats;
    }

}
