package com.flaptor.indextank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;

import com.flaptor.indextank.index.BoostedDocument;
import com.flaptor.indextank.index.IndexEngine;
import com.flaptor.indextank.rpc.Document;
import com.flaptor.indextank.rpc.Indexer;
import com.flaptor.indextank.rpc.IndexerStatus;
import com.flaptor.indextank.storage.alternatives.IndexStorage;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.google.common.collect.Maps;

@Deprecated
public class IndexRecoverer extends Thread {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    public static enum IndexStorageValue { SIMPLEDB, CASSANDRA };

    private String indexServerHost;
    private Integer indexServerPort;
	private String indexId;
    private long timestamp;
	private Indexer.Client client;
    private boolean onlyDynamicData;
	private final String environment;
    private IndexEngine ie;
    private IndexStorageValue indexStorageValue;
    private String cassandraClusterHosts;

	public IndexRecoverer(IndexEngine ie,
            String indexServerHost,
			Integer indexServerPort,
            File baseDir,
			String indexId,
			String environment,
			IndexStorageValue indexStorageValue,
			String cassandraClusterHosts) {
        this.ie = ie;
        this.indexServerHost = indexServerHost;
        this.indexServerPort = indexServerPort;
		this.indexId = indexId;
		this.environment = environment;
        this.onlyDynamicData = false;
		timestamp = readTimestamp(baseDir);
		this.indexStorageValue = indexStorageValue;
		this.cassandraClusterHosts = cassandraClusterHosts;
	}

	public IndexRecoverer(IndexEngine ie,
            String indexServerHost,
			Integer indexServerPort,
			String indexId,
			String environment,
            long timestamp,
            boolean onlyDynamicData,
            IndexStorageValue indexStorageValue, 
            String cassandraClusterHosts) {
        this.ie = ie;
        this.indexServerHost = indexServerHost;
        this.indexServerPort = indexServerPort;
		this.indexId = indexId;
		this.environment = environment;
		this.timestamp = timestamp;
        this.onlyDynamicData = onlyDynamicData;
        this.indexStorageValue = indexStorageValue;
        this.cassandraClusterHosts = cassandraClusterHosts;
	}

    public void resetTimestamp() {
        timestamp = 0;
    }

	@Override
	public void run() {
        TSocket transport = new TSocket(indexServerHost, indexServerPort + 1);
        TProtocol protocol = new TBinaryProtocol(transport);
        client = new Indexer.Client(protocol);
        try {
            transport.open();
            IndexStorage storage = getStorage();
            logger.debug("Using environment: " + environment);
            Iterable<Pair<String, BoostedDocument>> allDocuments = storage.getAllDocuments(true, timestamp);
            logger.info("Starting recovery.");
            if (ie != null) ie.setStatus(IndexerStatus.recovering);
            int recovered = 0;
            for (Pair<String, BoostedDocument> pair : allDocuments) {
                logger.debug("Document found!");
                String docId = pair.first();
                
                BoostedDocument boostedDocument = pair.last();
                Map<Integer, Double> doubleBoosts = Maps.newHashMap();
                for (Entry<Integer, Float> entry : boostedDocument.getBoosts().entrySet()) {
                    doubleBoosts.put(entry.getKey(), Double.valueOf(entry.getValue()));
                }
                Map<String,String> categories = boostedDocument.getCategories();
                int userTimestamp;
                try {
                    userTimestamp = Integer.parseInt(boostedDocument.getDocument().getField("timestamp"));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid timestamp " +  boostedDocument.getDocument().getField("timestamp") + " for document " + docId + " -- SKIPPING DOCUMENT");
                    continue;
                }
                recovered++;
                if (onlyDynamicData) {
                    // this is only usefull for reddit, and only because it has an old version that doesn't recover variables from simpledb.
                    //System.out.println("Updating  docid:" + docId);
                    client.updateBoost(docId, doubleBoosts);
                    client.updateTimestampBoost(docId, userTimestamp);
                    client.updateCategories(docId, categories);
                } else {
                    //System.out.println("Adding  docid:" + docId);
                    if (boostedDocument.getDocument().asMap().size() > 0) {
                        Document doc = new Document();
                        doc.set_fields(boostedDocument.getDocument().asMap());
                        client.addDoc(docId, doc, userTimestamp, doubleBoosts);
                    } else {
                        client.updateBoost(docId, doubleBoosts);
                    }
                    client.updateCategories(docId, categories);
                }
                if (recovered % 100 == 0) {
                    logger.info("Recovered " + recovered + " documents so far");
                }
            }
            logger.info("Finished recovery. Recovered " + recovered + " documents");
            if (ie != null) ie.setStatus(IndexerStatus.ready);
        } catch (Exception ex) {
            throw new RuntimeException("Something BAD happened: ",ex);
        } finally {
            Execute.close(transport);
        }
	}

    public void updateSingleDymamicDatum() {
        TSocket transport = new TSocket(indexServerHost, indexServerPort + 1);
        TProtocol protocol = new TBinaryProtocol(transport);
        client = new Indexer.Client(protocol);
        try {
            transport.open();
            IndexStorage storage = getStorage();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in), 200);
            String docId;
            System.out.println("Enter the document Ids to recover, separated by enters.\nHit ctr-d to end.");
            while ((docId = in.readLine()) != null && docId.length() != 0) {
                BoostedDocument doc;
                try {
                    doc = storage.getDocument(docId, false);
                } catch (NullPointerException e) {
                    System.out.println("document " + docId + " is not in the storage.\nDELETING DOCUMENT.");
                    client.delDoc(docId);
                    continue;
                }
                Map<Integer, Double> doubleBoosts = Maps.newHashMap();
                for (Entry<Integer, Float> entry : doc.getBoosts().entrySet()) {
                    doubleBoosts.put(entry.getKey(), Double.valueOf(entry.getValue()));
                }
                Map<String,String> categories = doc.getCategories();

                client.updateBoost(docId, doubleBoosts);
                client.updateTimestampBoost(docId, Integer.parseInt(doc.getDocument().getField("timestamp")));
                client.updateCategories(docId, categories);
                System.out.println("Updated boosts for document " + docId + ", boosts: " + doubleBoosts);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Something BAD happened: ",ex);
        } finally {
            Execute.close(transport);
        }
    }
    
    private IndexStorage getStorage() {
        switch (this.indexStorageValue) {
        default:
            throw new RuntimeException("Unknown Index Storage " + this.indexStorageValue);
        }
    }
    
    public static void writeTimestamp(File dir, long timestamp) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(new File(dir, "timestamp")));
            oos.writeLong(timestamp);
        } catch (IOException e) {
            logger.error("Writing timestamp to inde2ehWx directory",e);
        } finally {
            if (null != oos) {
                try {
                    oos.close();
                } catch (IOException e) { /* ignore */ }
            }
        }
    }

    private static long readTimestamp(File dir) {
        long ts = 0;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(new File(dir, "timestamp")));
            ts = ois.readLong();
        } catch (IOException e) {
            /* It means the file is not there or is corrupt, so this is an empty or corrupt index,
               in which case we'll ignore the error and recover from the start of times. */
        } finally {
            if (null != ois) {
                try {
                    ois.close();
                } catch (IOException e) { /* ignore */ }
            }
        }
        return ts;
    }

    private static void help() {
        System.out.println("\n\tParameters:  <host> <port> <index_id> <environment> <timestamp> <only_dynamic_data>");
        System.out.println("\n\tor");
        System.out.println("\n\tParameters:  <host> <port> <index_id> <environment>>\n");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) help();
        String host = "", indexid = "", environment = "";
        int port = 0;
        long timestamp = 0;
        boolean onlyDynamicData = false;
        IndexStorageValue documentStorageValue = IndexStorageValue.SIMPLEDB;
        String cassandraClusterHosts = null;
        
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            indexid = args[2];
            environment = args[3];
            if (args.length > 4) {
                timestamp = Long.parseLong(args[4]);
                if (args.length > 5) {
                    onlyDynamicData = Boolean.parseBoolean(args[5]);
                    if (args.length > 6) {
                        if (args[6] == "cassandra") {
                            if (args.length == 7 || args[7].trim().length() == 0) {
                                throw new IllegalArgumentException("Expecting cassandra cluster hosts");
                            }
                            documentStorageValue = IndexStorageValue.CASSANDRA;
                            cassandraClusterHosts = args[7]; 
                        }
                    }
                }
            }
        } catch (Exception e) { help(); }
		IndexRecoverer indexRecoverer = new IndexRecoverer(null, host, port, indexid, environment, timestamp, onlyDynamicData, documentStorageValue, cassandraClusterHosts);
        if (args.length > 4) {
            System.out.println("Starting recovery...");
            indexRecoverer.run(); //blocking call, this is not start()
            System.out.println("Recovery finished");
        } else {
            indexRecoverer.updateSingleDymamicDatum();
        }
	}

}
