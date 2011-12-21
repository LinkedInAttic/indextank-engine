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

package com.flaptor.indextank;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.Document;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.storage.LogStorageIndexReader;
import com.flaptor.util.Execute;

public class LogIndexRecoverer implements Runnable {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private IndexEngine index;

    private final String indexCode;
    private final String logServerHost;
    private final int logServerPort;
    
    public LogIndexRecoverer(IndexEngine index, String indexCode, String logServerHost, int logServerPort) {
        this.index = index;
        this.indexCode = indexCode;
        this.logServerHost = logServerHost;
        this.logServerPort = logServerPort;
    }

    @Override
    public void run() {
        BoostingIndexer indexer = index.getIndexer();
        
        logger.info("Recovering index from log based storage.");
        
        int count = 0;
        LogStorageIndexReader reader = new LogStorageIndexReader(logServerHost, logServerPort, 5, indexCode);
        
        try {
            logger.info("Starting recovery from Log Based Storage");
            
            for (LogRecord record : reader) {
                if (record.is_deleted()) {
                    indexer.del(record.get_docid());
                }

                if (record.get_fields_size() > 0) {
                    if (!record.get_fields().containsKey("timestamp")) {
                        logger.warn("Document " + record.get_docid() + " had no timestamp, skipping it.");
                        continue;
                    }
                    Integer timestamp;
                    try {
                        timestamp = Integer.valueOf(record.get_fields().get("timestamp"));
                    } catch (NumberFormatException e) {
                        logger.warn("Document " + record.get_docid() + " had an invalid timestamp '" + record.get_fields().get("timestamp") + "', skipping it.");
                        continue;
                    }
                    Document document = new Document(record.get_fields());
                    indexer.add(record.get_docid(), document, timestamp, record.get_variables());
                } else {
                    indexer.updateBoosts(record.get_docid(), record.get_variables());
                }
                
                if (record.is_set_categories()) {
                    indexer.updateCategories(record.get_docid(), record.get_categories());
                }
                
                count++;
                
                if (count % 1000 == 0) {
                    logger.info("Already recovered " + count + " documents");
                }
            }
            
        } catch (Exception ex) {
            throw new RuntimeException("Something BAD happened: ",ex);
        }
    }
}
