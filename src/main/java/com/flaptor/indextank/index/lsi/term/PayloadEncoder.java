package com.flaptor.indextank.index.lsi.term;

import java.nio.charset.Charset;

import com.flaptor.indextank.index.DocId;

public class PayloadEncoder {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	public static DocId decodePayloadId(final byte[] data, int size) {
		return new DocId(data, 0, size);
	}

	public static byte[] encodePayloadId(String docid) {
		return docid.getBytes(UTF8);
	}

}
