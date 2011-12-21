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
