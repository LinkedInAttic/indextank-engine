package com.flaptor.indextank.index.term;

import java.util.NavigableMap;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.util.SkippableIterable;


public interface TermMatcher extends RawMatchesDecoder {

	public SkippableIterable<DocTermMatch> getMatches(String field, String term);
	/**
	 * Range (prefix) matching. All terms (existing in the index) between termFrom and termTo
	 * (lexicographically speaking) will be matched for the field.  
	 * 
	 * @param field the field to match in
	 * @param termFrom the left boundary of the range (inclusive)
	 * @param termTo the rigth boundary of the range (exclusive)
	 * 
	 * @return a NavigableMap from the terms to the resulting Iterables of DocTermMatch
	 */
	public NavigableMap<String, SkippableIterable<DocTermMatch>> getMatches(String field, String termFrom, String termTo);
	public SkippableIterable<Integer> getAllDocs();
	public boolean hasChanges(DocId docid);

}