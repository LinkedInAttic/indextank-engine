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
public class InMemoryStorage extends DocumentBinaryStorage {
  private static final Logger logger = Logger.getLogger(Execute.whoAmI());
  private static final String MAIN_FILE_NAME = "InMemoryStorage";

  private final File backupDir;
  private ConcurrentMap<String, byte[]> compressedMap = new MapMaker().makeMap();

  @SuppressWarnings("unchecked")
  public InMemoryStorage(File backupDir, boolean load) throws IOException {
    Preconditions.checkNotNull(backupDir);
    checkDirArgument(backupDir);
    this.backupDir = backupDir;
    File f = new File(this.backupDir, MAIN_FILE_NAME);
    if (load && f.exists()) {
      ObjectInputStream is = null;
      try {
        is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
          compressedMap = (ConcurrentMap<String, byte[]>) is.readObject();
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
        logger.info("State loaded.");
      } finally {
        Execute.close(is);
      }
    } else if (load) {
      logger.warn("Starting a new(empty) InMemoryStorage. Load was requested but no file was found.");
    } else {
      logger.info("Starting a new(empty) InMemoryStorage.");
    }
  }

  /**
   * @throws IllegalArgumentException
   */
  private static void checkDirArgument(File backupDir) {
    Preconditions.checkNotNull(backupDir);
    if (!backupDir.canRead()) {
      String s = "Don't have read permission over the backup directory(" + backupDir.getAbsolutePath() + ").";
      logger.error(s);
      throw new IllegalArgumentException(s);
    }
    if (!backupDir.canWrite()) {
      String s = "Don't have write permission over the backup directory(" + backupDir.getAbsolutePath() + ").";
      logger.error(s);
      throw new IllegalArgumentException(s);
    }
  }

  public void dump() throws IOException {
    syncToDisk();
  }

  /**
   * Serializes this instance content to disk.
   * Blocking method.
   */
  private synchronized void syncToDisk() throws IOException {
    logger.info("Starting dump to disk.");
    File f = new File(backupDir, MAIN_FILE_NAME);
    ObjectOutputStream os = null;
    try {
      os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
      os.writeObject(compressedMap);
      os.flush();
      logger.info("Dump to disk completed.");
    } finally {
      Execute.close(os);
    }
  }

    
	@Override
	protected byte[] getBinaryDoc(String docId) {
		return compressedMap.get(docId);
	}

	@Override
	public void saveBinaryDoc(String docId, byte[] bytes) {
		compressedMap.put(docId, bytes);
	}

	@Override
	public void deleteBinaryDoc(String docId) {
		compressedMap.remove(docId);
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

    @Override
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.put("in_memory_storage_count", String.valueOf(compressedMap.size()));
        return stats;
    }
    
}
