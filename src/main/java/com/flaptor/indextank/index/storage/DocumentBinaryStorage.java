/*
 * Copyright (c) 2012 LinkedIn, Inc
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

package com.flaptor.indextank.index.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.storage.alternatives.DocumentStorage;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;


/**
 * @author santip
 * @author dbuthay
 *
 */
public abstract class DocumentBinaryStorage implements DocumentStorage {
  private static final Logger logger = Logger.getLogger(Execute.whoAmI());
  private static final int COMPRESSION_THRESHOLD = 100;
  private static final int HEADER_COMPRESSED = 0x1;
  private static final int HEADER_HAS_TEXT = 0x2;

  protected abstract byte[] getBinaryDoc(String docId);
  protected abstract void saveBinaryDoc(String docId, byte[] bytes);
  protected abstract void deleteBinaryDoc(String docId);


	@Override
	public Document getDocument(String docId) {
		return decompress(getBinaryDoc(docId));
	}

	@Override
	public void saveDocument(String docId, Document document) {
		saveBinaryDoc(docId, compress(document));
	}

	@Override
	public void deleteDocument(String docId) {
		deleteBinaryDoc(docId);
	}

	private static byte[] compress(Document document) {
		try {
			int estimatedSize = estimateSize(document);
			boolean compress = estimatedSize >= COMPRESSION_THRESHOLD;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(estimatedSize);
	    	OutputStream os = baos;
	    	writeTo(document, os, compress);
	    	return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Document decompress(byte[] bytes) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			InputStream is = bais;
			return readFrom(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void writeTo(Document document, OutputStream os, boolean compress) throws IOException {
	    int header = 0;
	    if (compress) {
	        header |= HEADER_COMPRESSED;
	    }
		String text = document.getField("text");
		if (text != null) {
		    header |= HEADER_HAS_TEXT;
		}
		os.write(header);
		if (compress) {
		    os = new GZIPOutputStream(os);
		}
		if (text != null) {
			writeUTF(text, os);
		}
		int fs = document.asMap().size();
		if (text != null) fs -= 1;
		writeSize(fs , os);
		for (Entry<String,String> e : document.asMap().entrySet()) {
			if (!e.getKey().equals("text")) {
				writeUTF(e.getKey(), os);
				writeUTF(e.getValue(), os);
			}
		}
		os.close();
	}

	private static Document readFrom(InputStream is) throws IOException {
	    int header = is.read();
		String text = null;
		if ((header & HEADER_COMPRESSED) != 0) {
		    is = new GZIPInputStream(is);
		}
		if ((header & HEADER_HAS_TEXT) != 0) { 
		    text = readUTF(is);
		}
		int fs = readSize(is);
		Map<String, String> fields = Maps.newHashMapWithExpectedSize(fs + (text == null ? 0 : 1));
		if (text != null) {
		    fields.put("text", text);
		}
		while (fs-- > 0) {
			fields.put(readUTF(is), readUTF(is));
		}
		return new Document(fields);
	}

	private static int estimateSize(Document document) {
		int size = 0;
		for (Entry<String, String> e : document.asMap().entrySet()) {
			if (!e.getKey().equals("text")) {
				size += e.getKey().length();
			}
			size += e.getValue().length();
		}
		return size;
	}

	private static void writeUTF(String text, OutputStream os) throws IOException {
		int strlen = text.length();
		int c = 0;
	
		writeSize(strlen, os);
	
		int i=0;
		for (i=0; i<strlen; i++) {
			c = text.charAt(i);
			if (!((c >= 0x0001) && (c <= 0x007F))) break;
			os.write(c);
		}
	
		for (;i < strlen; i++){
			c = text.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				os.write(c);
	
			} else if (c > 0x07FF) {
				os.write(0xE0 | ((c >> 12) & 0x0F));
				os.write(0x80 | ((c >>  6) & 0x3F));
				os.write(0x80 | ((c >>  0) & 0x3F));
			} else {
				os.write(0xC0 | ((c >>  6) & 0x1F));
				os.write(0x80 | ((c >>  0) & 0x3F));
			}
		}
	}

	private static String readUTF(InputStream is) throws IOException {
		int size = readSize(is);
		char[] chars = new char[size];
		int c, c2, c3;
	    for (int i = 0; i < chars.length; i++) {
	        c = readNonEOF(is);
	        switch (c >> 4) {
	            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
	                /* 0xxxxxxx*/
	                chars[i]=(char)c;
	                break;
	            case 12: case 13:
	                /* 110x xxxx   10xx xxxx*/
	                c2 = readNonEOF(is);
	                if ((c2 & 0xC0) != 0x80) throw new UTFDataFormatException("malformed input around char " + i); 
	                chars[i] = (char)(((c & 0x1F) << 6) | (c2 & 0x3F));
	                break;
	            case 14:
	                /* 1110 xxxx  10xx xxxx  10xx xxxx */
	                c2 = readNonEOF(is);
	                c3 = readNonEOF(is);
	                if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80)) throw new UTFDataFormatException("malformed input around char " + i);
	                chars[i]=(char)(((c  & 0x0F) << 12) |
	                                ((c2 & 0x3F) <<  6) |
	                                ((c3 & 0x3F) <<  0));
	                break;
	            default:
	                /* 10xx xxxx,  1111 xxxx */
	                throw new UTFDataFormatException("malformed input around char " + i);
	        }
	    }
	    return String.valueOf(chars);
	}

	private static void writeSize(int size, OutputStream os) throws IOException {
		while (size >= 128) {
			os.write((size & 0x7F) | 0x80);
			size >>= 7;
		}
		os.write(size & 0x7F);
	}

	private static int readSize(InputStream is) throws IOException {
		int c = 0;
		int size = 0;
		boolean left = true;
		while (left) {
			int b = readNonEOF(is);
			left = (b & 0x80) != 0;
			b &= 0x7F;
			b <<= 7 * c;
			size |= b;
			c++;
		}
		return size;
	}

	private static int readNonEOF(InputStream is) throws IOException {
		int c = is.read();
		if (c == -1) throw new EOFException();
		return c;
	}

	/**
	 * Allows testing changes to the compression method, it first
	 * validates the correctness of the implementation and then
	 * lists the compression value and ratio for several document
	 * sizes.
	 * 
	 * First argument should be the text to use for texting, it will
	 * be clipped to different sizes for ratio testing.
	 */
  /*
	public static void main(String[] args) throws IOException {
        //testCorrectness(args);
	    //testCompressionRatio(args);
	    InMemoryStorage ims = new InMemoryStorage(new File(args[0]), true);
	    
        Scanner in = new Scanner(System.in);
        
        while (in.hasNextLine()) {
            Document document = ims.getDocument(in.nextLine());
            System.out.println(document);
        }

	}

    private static void testCompressionRatio(String[] args) {
        String text = args[0];
        int len = text.length();
        while (len > 10) {
            test(text, len);
            len -= 10;
        }
    }

    private static void testCorrectness(String[] args) throws IOException {
        InMemoryStorage storage = new InMemoryStorage(FileUtil.createTempDir("testInMemoryStorage", ".tmp"), false);
        Document doc1 = new Document();
        doc1.setField("text", args[0]);
        storage.saveDocument("a", doc1);
        Document dd1 = storage.getDocument("a");
        Preconditions.checkState(dd1.equals(doc1), dd1 + " - " + doc1);
        Document doc2 = new Document();
        doc2.setField("nottext", args[0]);
        storage.saveDocument("b", doc2);
        Document dd2 = storage.getDocument("b");
        Preconditions.checkState(dd2.equals(doc2), dd2);
        Document doc3 = new Document();
        doc3.setField("text", args[0]);
        doc3.setField("f1", "v1");
        doc3.setField("f2", "v2");
        storage.saveDocument("c", doc3);
        Document dd3 = storage.getDocument("c");
        Preconditions.checkState(dd3.equals(doc3), dd3);
    }

    private static void test(String text, int len) {
        Document d = new Document();
        d.setField("text", text.substring(0, len));
        int clen = compress(d).length;
        len *= 2;
        System.out.println(String.format("%2.2f = original: %5d - compressed: %5d", 1.0 * clen / len, len, clen));
    }
	
    @Override
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.put("in_memory_storage_count", String.valueOf(compressedMap.size()));
        return stats;
    }
    */
}
