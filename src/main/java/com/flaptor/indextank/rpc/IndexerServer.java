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

package com.flaptor.indextank.rpc;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.BoostingIndexer;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.util.Execute;

public class IndexerServer { 
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private IndexEngine ie;
    private BoostingIndexer indexer;
    private int port;

    public IndexerServer(IndexEngine ie, com.flaptor.indextank.BoostingIndexer indexer, int port) {
        this.ie = ie;
        this.indexer = indexer;  
        this.port = port;
    }

    public void start(){
        Thread t = new Thread() { 
            public void run() { 
                try {
                    TServerSocket serverTransport = new TServerSocket(IndexerServer.this.port);
                    Indexer.Processor processor = new Indexer.Processor(new IndexerImpl(ie, IndexerServer.this.indexer));
                    Factory protFactory = new TBinaryProtocol.Factory(true, true);
                    TServer server = new TThreadPoolServer(processor, serverTransport, protFactory);
                    System.out.println("Starting indexer server on port " +  IndexerServer.this.port + " ...");
                    server.serve();
                } catch( TTransportException tte ){
                    tte.printStackTrace();
                }
            }
        } ;

        t.start();
    }

    /**
     * Converts a Thrift Document into a Indextank Document.
     */ 
    private static com.flaptor.indextank.index.Document toIndexTankDocument(Document doc) {
        return new com.flaptor.indextank.index.Document(doc.get_fields());
    }


    private class IndexerImpl implements Indexer.Iface {
        private IndexEngine ie;
        private BoostingIndexer indexer;

        // constructor
        private IndexerImpl(IndexEngine ie, BoostingIndexer indexer){
            this.ie = ie;
            this.indexer = indexer;
        }

        @Override
        public void promoteResult(String docId, String query) throws IndextankException {
            try {
                logger.info("Promoting " + docId + " for query: " + query);
                this.indexer.promoteResult(docId, query);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing promoteResult.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public void updateCategories(String docId, Map<String, String> categories) throws IndextankException {
            try {
                logger.info("Updating " + categories.size() + " categories for " + docId);
                this.indexer.updateCategories(docId, categories);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing updateCategories.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public void dump() throws IndextankException {
            try {
                logger.info("Executing dump");
                this.indexer.dump();
            } catch (IOException e) {
                logger.error("IOException while processing dump.", e);
                throw new IndextankException(e.getMessage());
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing dump.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public void delDoc(String docId) throws IndextankException {
            try {
                logger.info("Deleting document " + docId);
                this.indexer.del(docId);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing delDoc.", e);
                throw new IndextankException(e.getMessage());
            }
        }
        @Override
        public IndexerStats stats() throws IndextankException {
            try {
                return new IndexerStats();
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing stats.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public IndexerStatus getStatus() {
            IndexerStatus status = ie.getStatus();
            logger.info("Returning index status (" + status.ordinal() + ") : " + status);
            return status;
        }

        @Override
        public void ping() throws TException {
            logger.info("Pinged");
        }

        @Override
        public void startFullRecovery() {
            logger.info("Starting full recovery");
            ie.startFullRecovery();
        }

		@Override
		public void addDoc(String docId, Document doc, int timestampBoost, Map<Integer, Double> boosts) throws IndextankException, TException {
            try {
                logger.info("Adding document " + docId + " for timestamp " + timestampBoost + " with " + doc.get_fields_size() + " fields and " + boosts.size() + " variables.");
                if (null == docId || docId.isEmpty()) {
                    String msg = "Received invalid documentId: \"" + docId + "\". Skipping Add.";
                    logger.error(msg);
                    throw new IndextankException(msg);
                }
            
                this.indexer.add(docId, IndexerServer.toIndexTankDocument(doc), timestampBoost, boosts);			
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing addDoc.", e);
                throw new IndextankException(e.getMessage());
            }
		}

		@Override
		public void updateBoost(String docId, Map<Integer, Double> boosts) throws IndextankException, TException {
            try {
                logger.info("Updating " + boosts.size() + " variables for document " + docId);
                this.indexer.updateBoosts(docId, boosts);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing updateBoost.", e);
                throw new IndextankException(e.getMessage());
            }
		}

		@Override
		public void updateTimestampBoost(String docId, int timestampBoost) throws IndextankException, TException {
            try {
                logger.info("Updating timestamp to " + timestampBoost + " variables for document " + docId);
                this.indexer.updateTimestamp(docId, timestampBoost);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing updateTimestampBoost.", e);
                throw new IndextankException(e.getMessage());
            }
		}

        @Override
        public void addScoreFunction(int functionIndex, String definition) throws IndextankException, TException {
            try {
                logger.info("Updating function " + functionIndex + " to definition " + definition);
                this.indexer.addScoreFunction(functionIndex, definition);
            } catch (Exception e) {
                logger.error("Exception while processing addScoreFunction.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public void removeScoreFunction(int functionIndex) throws IndextankException, TException {
            try {
                logger.info("Removing function " + functionIndex);
                this.indexer.removeScoreFunction(functionIndex);
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing removeScoreFunction.", e);
                throw new IndextankException(e.getMessage());
            }
        }

        @Override
        public Map<Integer,String> listScoreFunctions() throws IndextankException, TException {
            try {
                Map<Integer, String> functions = this.indexer.listScoreFunctions();
                logger.info("Listed " + functions.size() + " functions");
                return functions;
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing listScoreFunctions.", e);
                throw new IndextankException();
            }
        }

        @Override
        public Map<String, String> get_stats() throws IndextankException, TException {
            try {
                Map<String, String> stats = this.indexer.getStats();
                logger.info("Returned stats");
                return stats;
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing listScoreFunctions.", e);
                throw new IndextankException();
            }
        }
        
        @Override
        public void force_gc() throws IndextankException, TException {
            try {
                Runtime.getRuntime().gc();
                logger.info("Forced GC");
            } catch (RuntimeException e) {
                logger.error("RuntimeException while running Runtime.gc().", e);
                throw new IndextankException();
            }
        }
        
    }
}
