grammar ScoreFormula;

options {
	backtrack=false;
	memoize=true;
	k=1;
}


@rulecatch {
        catch (RecognitionException e) {
            throw e;
        }
}

@lexer::header {
package com.flaptor.indextank.index.scorer.parser;
}

@header {
package com.flaptor.indextank.index.scorer.parser;

import com.flaptor.indextank.index.scorer.ScoreFunction;
import com.flaptor.indextank.index.scorer.Boosts;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.indextank.query.QueryVariablesImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;
import org.cojen.classfile.Opcode;
import org.cojen.classfile.Label;
import java.lang.reflect.Constructor;
}

@lexer::members {

    private Exception error = null;

    public void emitErrorMessage(String msg) {
        error = new ParserException(msg);
    }

    public Exception getError() {
        return error;
    }

}

@members {

    private CodeBuilder b = null;
    private TypeDesc docVarType = null;
    private TypeDesc queryVarType = null;
    private TypeDesc[] params = null;
    private LocalVariable scoreParam = null;
    private LocalVariable ageParam = null;
    private LocalVariable docVarParam = null;
    private LocalVariable queryVarParam = null;
    private Exception error = null;

    private Class<?> generateClass(ScoreFormulaLexer lexer, int funcNum) throws Exception {
        // initialize the code generator
        RuntimeClassFile cf = new RuntimeClassFile("UserDefinedFunction_"+funcNum);
        cf.addInterface("com.flaptor.indextank.index.scorer.ScoreFunction");
        cf.addDefaultConstructor();
        docVarType = TypeDesc.forClass("com.flaptor.indextank.index.scorer.Boosts"); // DocVariables
        queryVarType = TypeDesc.forClass("com.flaptor.indextank.query.QueryVariables"); // Variables
        params = new TypeDesc[] {TypeDesc.DOUBLE, TypeDesc.INT, docVarType, queryVarType};
        MethodInfo mi = cf.addMethod(Modifiers.PUBLIC, "score", TypeDesc.DOUBLE, params);
        b = new CodeBuilder(mi);
        scoreParam = b.getParameter(0);
        ageParam = b.getParameter(1);
        docVarParam = b.getParameter(2);
        queryVarParam = b.getParameter(3);

        // parse and generate code
        start();

        error = error == null ? lexer.getError() : error;
        if (null != error) {
            throw error;
        }

        // return the generated class
        Class<?> clazz = cf.defineClass();
        return clazz;
    }

    public void emitErrorMessage(String msg) {
        error = new ParserException(msg);
    }

    public static ScoreFunction parseFormula(int funcNum, String definition) throws Exception {
        InputStream reader = new ByteArrayInputStream(definition.getBytes());
        ANTLRInputStream input = new ANTLRInputStream(reader);
        ScoreFormulaLexer lexer = new ScoreFormulaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ScoreFormulaParser parser = new ScoreFormulaParser(tokens);
        Class<?> funcClass = parser.generateClass(lexer, funcNum);
        Constructor cons = funcClass.getConstructor();
        return (ScoreFunction)cons.newInstance();
    }


    public static void main(String[] args) throws Exception {
        Boosts doc_vars = new Boosts() {
                public float getBoost(int idx) {return idx/10f;}
                public int getBoostCount() {return 1;}
                public int getTimestamp() {return 0;}
            };
        QueryVariables query_vars = new QueryVariablesImpl(new Double[] {0.0d,1.0d,2.0d,3.0d,4.0d,5.0d,6.0d,7.0d,8.0d,9.0d});
        double score = 0.5d;
        int age = 100;

        ScoreFunction func = parseFormula(1,args[0]);
        double result = func.score(score, age, doc_vars, query_vars);
        System.out.println("result: "+result);
    }

    private void emit_double(double value) {
        b.loadConstant(value);
    }

    private void emit_integer(int value) {
        b.loadConstant(value);
    }

    private void emit_relevance() {
        b.loadLocal(scoreParam); // push the textualScore parameter
    }

    private void emit_age() {
        b.loadLocal(ageParam); // push the age parameter
        b.convert(TypeDesc.INT, TypeDesc.DOUBLE);
    }

    private void emit_docVar() {
        b.loadLocal(docVarParam); // push the docVar parameter
        b.swap(); // swap the top two words on the stack, so the var id is on top
        params = new TypeDesc[] {TypeDesc.INT}; // the type of the id parameter (int)
        b.invokeInterface(docVarType, "getBoost", TypeDesc.FLOAT, params);
        b.convert(TypeDesc.FLOAT, TypeDesc.DOUBLE);
    }

    private void emit_queryVar() {
        b.loadLocal(queryVarParam); // push the queryVar parameter
        b.swap(); // swap the top two words on the stack, so the var id is on top
        params = new TypeDesc[] {TypeDesc.INT}; // the type of the id parameter (int)
        b.invokeInterface(queryVarType, "getValue", TypeDesc.DOUBLE, params);
    }

    private void emit_return() {
        b.returnValue(TypeDesc.DOUBLE);
    }

    private void emit_plus() {
        b.math(Opcode.DADD);
    }

    private void emit_minus() {
        b.math(Opcode.DSUB);
    }

    private void emit_mult() {
        b.math(Opcode.DMUL);
    }

    private void emit_div() {
        b.math(Opcode.DDIV);
    }

    private void emit_pow() {
        params = new TypeDesc[] {TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "pow", TypeDesc.DOUBLE, params);
    }

    private void emit_sqrt() {
        params = new TypeDesc[] {TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "sqrt", TypeDesc.DOUBLE, params);
    }

    private void emit_log() {
        params = new TypeDesc[] {TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "log", TypeDesc.DOUBLE, params);
    }

    private void emit_max() {
        params = new TypeDesc[] {TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "max", TypeDesc.DOUBLE, params);
    }

    private void emit_min() {
        params = new TypeDesc[] {TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "min", TypeDesc.DOUBLE, params);
    }

    private void emit_abs() {
        params = new TypeDesc[] {TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "abs", TypeDesc.DOUBLE, params);
    }

    private void emit_neg() {
        params = new TypeDesc[] {TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "neg", TypeDesc.DOUBLE, params);
    }

    private void emit_km() {
        params = new TypeDesc[] {TypeDesc.DOUBLE,TypeDesc.DOUBLE,TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "km", TypeDesc.DOUBLE, params);
    }

    private void emit_miles() {
        params = new TypeDesc[] {TypeDesc.DOUBLE,TypeDesc.DOUBLE,TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "miles", TypeDesc.DOUBLE, params);
    }

    private void emit_bit() {
        params = new TypeDesc[] {TypeDesc.DOUBLE, TypeDesc.DOUBLE};
        b.invokeStatic("com.flaptor.indextank.index.scorer.ScoreMath", "bit", TypeDesc.DOUBLE, params);
    }

    private void emit_cond(String comp) {
        Label lbl1 = b.createLabel();
        Label lbl2 = b.createLabel();
        b.ifComparisonBranch(lbl1, comp, TypeDesc.DOUBLE);
        b.loadConstant(1);
        b.branch(lbl2);
        lbl1.setLocation();
        b.loadConstant(0);
        lbl2.setLocation();
    }

    private void emit_if() {
        LocalVariable var1 = b.createLocalVariable("var1", TypeDesc.DOUBLE);
        LocalVariable var2 = b.createLocalVariable("var2", TypeDesc.DOUBLE);
        b.storeLocal(var2);
        b.storeLocal(var1);
        Label lbl1 = b.createLabel();
        Label lbl2 = b.createLabel();
        b.ifZeroComparisonBranch(lbl1, "!=");
        b.loadLocal(var1);
        b.branch(lbl2);
        lbl1.setLocation();
        b.loadLocal(var2);
        lbl2.setLocation();
    }


}

/* PARSER RULES */
start
	:	expr EOF { emit_return(); }
	;
expr
	:	term
		( PLUS term { emit_plus(); }
		| MINUS term { emit_minus(); }
		)*
	;
term
	:	factor
		( MULT factor { emit_mult(); }
		| DIV factor { emit_div(); }
		)*
	;
factor
	:	number
	|	var
	| 	'(' expr ')'
	| 	'log(' expr ')' { emit_log(); }
	| 	'pow(' expr ',' expr ')' { emit_pow(); }
	| 	'max(' expr ',' expr ')' { emit_max(); }
	| 	'min(' expr ',' expr ')' { emit_min(); }
	| 	'abs(' expr ')' { emit_abs(); }
	|	'sqrt(' expr ')' { emit_sqrt(); }
	|	'if(' cond ',' expr ',' expr ')' { emit_if(); }
	|	'km(' expr ',' expr ',' expr ',' expr ')' { emit_km(); }
	|	'miles(' expr ',' expr ',' expr ',' expr ')' { emit_miles(); }
	|	'bit(' expr ',' expr ')' { emit_bit(); }
	|	MINUS factor { emit_neg(); }
	;
cond
	:	expr
		( '==' expr { emit_cond("=="); }
		| '<=' expr { emit_cond("<="); }
		| '>=' expr { emit_cond(">="); }
		| '<>' expr { emit_cond("!="); }
		| '!=' expr { emit_cond("!="); }
		| '=' expr { emit_cond("=="); }
		| '<' expr { emit_cond("<"); }
		| '>' expr { emit_cond(">"); }
		)
	;
var
	:	RELEVANCE { emit_relevance(); }
	|	AGE { emit_age(); }
	|	BOOST '(' integer ')' { emit_docVar(); }
	|	DOC_VAR '[' integer ']' { emit_docVar(); }
	|	QUERY_VAR '[' integer ']' { emit_queryVar(); }
	;

number
	:	INT { emit_double(Double.parseDouble($INT.text)); } 
	|	DBL { emit_double(Double.parseDouble($DBL.text)); } 
	;

integer
	:	INT { emit_integer(Integer.parseInt($INT.text)); }
	;


/* LEXER RULES */

PLUS
	:	'+'
	;
MINUS
	:	'-'
	;
MULT
	:	'*'
	;
DIV
	:	'/'
	;
RELEVANCE
	:	'R' | 'r' | 'rel' | 'relevance'
	;
AGE
	:	'A' | 'a' | 'age' | 'doc.age'
	;
BOOST
	:	'B' | 'b' | 'boost'
	;
DOC_VAR
	:	'D' | 'd' | 'dvar' | 'doc.var'
	;
QUERY_VAR
	:	'Q' | 'q' | 'qvar' | 'query.var'
	;

INT
	:	INTEGER
	;
DBL
	:	DOUBLE
	;
fragment DOUBLE
	:	INTEGER '.' INTEGER
	;
fragment INTEGER
	:	'0'..'9'+
	;
WS
	:	 ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ { skip(); }
	;
