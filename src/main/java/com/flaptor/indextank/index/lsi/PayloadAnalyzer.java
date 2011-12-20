package com.flaptor.indextank.index.lsi;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

import com.flaptor.indextank.index.lsi.term.PayloadEncoder;

public class PayloadAnalyzer extends KeywordAnalyzer {

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new Filter(super.tokenStream(fieldName, reader));
	}
	public TokenStream reusabletokenStream(String fieldName, Reader reader) {
		return new Filter(super.tokenStream(fieldName, reader));
	}
	
	
	
	static class Filter extends TokenFilter {
		private TermAttribute termAtt;
		private PayloadAttribute payAtt;
	
		public Filter(TokenStream input) {
			super(input);
			termAtt = addAttribute(TermAttribute.class);
			payAtt = addAttribute(PayloadAttribute.class);
		}
	
		@Override
		public boolean incrementToken() throws IOException {
			boolean result = false;
			if (input.incrementToken()) {
				String docid = termAtt.term();
				termAtt.setTermBuffer(LsiIndex.PAYLOAD_TERM_TEXT);
				payAtt.setPayload(new Payload(PayloadEncoder.encodePayloadId(docid)));
				return true;
			}
			return result;
		}
	}
}
