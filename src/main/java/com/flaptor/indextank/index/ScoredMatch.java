package com.flaptor.indextank.index;

import java.util.Comparator;

import com.flaptor.indextank.search.SearchResult;
import com.google.common.base.Function;

/**
 *
 * @author Flaptor Team
 */
public class ScoredMatch implements Comparable<ScoredMatch> {

    private double score;
	private DocId docId;


    public ScoredMatch(double score, DocId docId) {
        this.score = score;
        this.docId = docId;
    }
    
    public double getScore() {
        return score;
    }

    public DocId getDocId() {
        return docId;
    }
    
    public void setScore(double score) {
		this.score = score;
	}

    public int compareTo(ScoredMatch other) {
        int c = Double.compare(other.getScore(), score);
        if (c == 0) {
        	c = docId.compareTo(other.docId);
        }
        return c;
    }

    @Override
    public String toString() {
        return "docId: " + docId + ", score: " + score + ".";
    }

    public static Function<ScoredMatch, DocId> DOC_ID_FUNCTION = new Function<ScoredMatch, DocId>() {
		@Override
		public DocId apply(ScoredMatch r) {
			return r.getDocId();
		}
	};
    
	public static Function<ScoredMatch, SearchResult> SEARCH_RESULT_FUNCTION = new Function<ScoredMatch, SearchResult>() {
	    @Override
        public SearchResult apply(ScoredMatch sdid){
            return new SearchResult(sdid.getScore(), sdid.getDocId().toString());
        }
	};
	
	public final static Comparator<ScoredMatch> INVERSE_ORDER = new Comparator<ScoredMatch>() {
        public int compare(ScoredMatch o1, ScoredMatch o2) {
            return -o1.compareTo(o2);
        }
    };
	
}
