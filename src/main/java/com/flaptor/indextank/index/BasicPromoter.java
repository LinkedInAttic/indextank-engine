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

package com.flaptor.indextank.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class BasicPromoter extends AbstractPromoter {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final String MAIN_FILE_NAME = "promoter";
    private final ConcurrentMap<String, String> storage;
    private final File backupDir;

    /**
     * Constructor.
     * Creates an empty Promoter.
     * @param backupDir the directory to wich the data stored in this Scorer shall be
     * @param load if true, the promoter is generated from it's serialized version.
     * persisted.
     */
    @SuppressWarnings("unchecked")
	public BasicPromoter(File backupDir, boolean load) throws IOException {
        checkDirArgument(backupDir);
        this.backupDir = backupDir;
        if (!load) {
            storage = new ConcurrentHashMap<String, String>();
        } else {
            File f = new File(this.backupDir, MAIN_FILE_NAME);
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
                try {
                    storage = (ConcurrentMap<String, String>) is.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            } finally {
                Execute.close(is);
            }
        }
    }

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

    @Override
    public void promoteResult(String docId, String queryStr) {
        Preconditions.checkNotNull(docId);
        Preconditions.checkNotNull(queryStr);
        storage.put(queryStr, docId);
    }

    @Override
    public String getPromotedDocId(String queryStr) {
        return storage.get(queryStr);
    }

    private synchronized void syncToDisk() throws IOException {
        logger.info("Starting info dump to disk.");
        File f = new File(backupDir, MAIN_FILE_NAME);
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            os.writeObject(storage);
            os.flush();
        } finally {
            Execute.close(os);
        }
        logger.info("Dump to disk finished");
    }

    @Override
    public void dump() throws IOException {
        syncToDisk();
    }

    @Override
    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.put("basic_promoter_count", String.valueOf(storage.size()));
        return stats;
    }
}
