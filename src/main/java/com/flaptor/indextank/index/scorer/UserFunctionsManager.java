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
