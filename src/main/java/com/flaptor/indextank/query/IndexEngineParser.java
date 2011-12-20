package com.flaptor.indextank.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.util.Version;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;

public class IndexEngineParser {
	private final Analyzer analyzer;
	private final String defaultField;

	public IndexEngineParser(String defaultField, Analyzer analyzer) {
		this.defaultField = defaultField;
		this.analyzer = analyzer;
	}
	
	public IndexEngineParser(String defaultField) {
		this(defaultField, new IndexEngineAnalyzer());
	}
	
    @SuppressWarnings("deprecation")
    public QueryNode parseQuery(final String queryStr) throws ParseException {
        org.apache.lucene.queryParser.QueryParser qp = new org.apache.lucene.queryParser.QueryParser(Version.LUCENE_CURRENT, defaultField, getAnalyzer());
        qp.setDefaultOperator(org.apache.lucene.queryParser.QueryParser.Operator.AND);
        org.apache.lucene.search.Query luceneQuery;
        try {
            luceneQuery = qp.parse(queryStr);
        } catch (Exception e) {
            throw new ParseException("lucene failed parsing. " + e);
        }

        return internalParse(luceneQuery, queryStr);
    }

    /**
     * Returns a lucene Analyzer that behaves like the analyzer used
     * internally in this class.
     * Try not to use this method.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }


    public Iterator<AToken> parseDocumentField(String fieldName, String content) {
        final TokenStream tkstream = analyzer.tokenStream(fieldName, new StringReader(content));
        final TermAttribute termAtt = tkstream.addAttribute(TermAttribute.class);
        final PositionIncrementAttribute posIncrAttribute = tkstream.addAttribute(PositionIncrementAttribute.class);
        final OffsetAttribute offsetAtt = tkstream.addAttribute(OffsetAttribute.class);

        return new AbstractIterator<AToken>() {
        	int currentPosition = 0;
            @Override
            protected AToken computeNext() {
                try {
                    if (!tkstream.incrementToken()) {
                        tkstream.end();
                        tkstream.close();
                        return endOfData();
                    }
                } catch (IOException e) {
                    //This should never happen, as the reader is a StringReader
                }
                //final org.apache.lucene.analysis.Token luceneTk = tkstream.getAttribute(org.apache.lucene.analysis.Token.class);
                currentPosition += posIncrAttribute.getPositionIncrement();
                final int position = currentPosition;
                final int startOffset = offsetAtt.startOffset();
                final int endOffset = offsetAtt.endOffset();
                final String text = termAtt.term();
                return new AToken() {
                    @Override
                    public String getText() {
                        return text; //luceneTk.term();
                    }
                    @Override
                    public int getPosition() {
                        return position; //luceneTk.getPositionIncrement();
                    }
                    @Override
                    public int getStartOffset() {
                        return startOffset;
                    }
                    @Override
                    public int getEndOffset() {
                        return endOffset;
                    }
                };
            }
        };

    }

    private QueryNode internalParse(org.apache.lucene.search.Query luceneQuery, final String originalStr) throws ParseException {
        QueryNode node;
        if (luceneQuery instanceof org.apache.lucene.search.TermQuery) {
            Term t = ((org.apache.lucene.search.TermQuery) luceneQuery).getTerm();
            String field = t.field();
            String text = t.text();
            node = new TermQuery(field, text);
        } else if (luceneQuery instanceof org.apache.lucene.search.PrefixQuery) {
            Term t = ((org.apache.lucene.search.PrefixQuery) luceneQuery).getPrefix();
            String field = t.field();
            String text = t.text();
            node = new PrefixTermQuery(field, text);
        } else if (luceneQuery instanceof org.apache.lucene.search.BooleanQuery) {
            List<BooleanClause> clauses = ((org.apache.lucene.search.BooleanQuery) luceneQuery).clauses();
            if (clauses.isEmpty()) {
                throw new ParseException("error parsing: " + originalStr);
            }
            node = internalParseBooleanQuery(clauses, originalStr);
        } else if (luceneQuery instanceof org.apache.lucene.search.PhraseQuery) {
            org.apache.lucene.search.PhraseQuery phraseQuery = (org.apache.lucene.search.PhraseQuery) luceneQuery;
            int[] positions = phraseQuery.getPositions();
            node = internalParsePhraseQuery(phraseQuery.getTerms(), positions, originalStr);
        } else {
            throw new ParseException("unimplemented");
        }
        node.setBoost(luceneQuery.getBoost());
        return node;
    }

    private QueryNode internalParsePhraseQuery(Term[] terms, int[] positions, final String originalStr) {
        Preconditions.checkArgument(terms.length > 0, "too few terms to build a phrase query");
        String[] strs = new String[terms.length];
        for (int i = 0; i < terms.length; i++) {
            strs[i] = terms[i].text();
        }
        return new SimplePhraseQuery(terms[0].field(), strs, positions);
    }

    private QueryNode internalParseBooleanQuery(List<BooleanClause> list, final String originalStr) throws ParseException {
        Preconditions.checkArgument(list.size() > 0, "too few terms to build a boolean query");
        List<BooleanClause> positiveClauses = new ArrayList<BooleanClause>();
        List<BooleanClause> negativeClauses = new ArrayList<BooleanClause>();
        for (BooleanClause clause : list) {
            if (clause.isProhibited()) {
                negativeClauses.add(clause);
            } else {
                positiveClauses.add(clause);
            }
        }
        if (positiveClauses.isEmpty()) {
            throw new ParseException("No positive clauses.");
        }
        QueryNode retVal = internalParsePositive(positiveClauses, originalStr);

        for (BooleanClause clause : negativeClauses) {
            retVal = new DifferenceQuery(retVal, internalParse(clause.getQuery(), null));
        }
        return retVal;
    }

    private QueryNode internalParsePositive(List<BooleanClause> list, final String originalStr) throws ParseException {
        Preconditions.checkArgument(list.size() > 0, "too few terms to build a boolean query");
        QueryNode firstQuery = internalParse(list.get(0).getQuery(), null);
        if (1 == list.size()) {
            return firstQuery;
        }
        if (list.get(1).isRequired()) {
            return new AndQuery(firstQuery, internalParseBooleanQuery(list.subList(1,list.size()), null));
        } else if (list.get(1).isProhibited()) {
            return new DifferenceQuery(firstQuery, internalParseBooleanQuery(list.subList(1,list.size()), null));
        } else {
            return new OrQuery(firstQuery, internalParseBooleanQuery(list.subList(1,list.size()), null));
        }
    }






    public static void main(String[] args) throws Exception {
        System.out.println("Parsing \"" + args[0] + "\" ...");
        IndexEngineParser parser = new IndexEngineParser("text");
        QueryNode query = parser.parseQuery(args[0]);
        System.out.println(query);
    }
}
