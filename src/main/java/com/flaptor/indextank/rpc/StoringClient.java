package com.flaptor.indextank.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.storage.alternatives.EnqueuingStorage;
import com.google.common.collect.Maps;

public class StoringClient implements EnqueuingStorage {
	private final String host;
	private final int port;

	public StoringClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	
	@Override
	public void enqueueAddDocument(String indexId, String sourceDocId, com.flaptor.indextank.index.Document document, int timestamp, Map<Integer, Float> boosts) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Storage.Client client = new Storage.Client(protocol);
		
		try {
		    Map<Integer, Double> doubleBoosts = Maps.newHashMapWithExpectedSize(boosts.size());
		    for (Entry<Integer, Float> entry : boosts.entrySet()) {
				doubleBoosts.put(entry.getKey(), entry.getValue().doubleValue());
			}
			
			transport.open();
			client.enqueueAddStore(indexId, sourceDocId, new Document(document.asMap()), timestamp, doubleBoosts);
			transport.close();
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) { 
			throw new RuntimeException(e);
		}
	}

	@Override
	public void enqueueRemoveDocument(String indexId, String sourceDocId) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Storage.Client client = new Storage.Client(protocol);
		
		try {
			transport.open();
			client.enqueueRemoveStore(indexId, sourceDocId);
			transport.close();
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public void enqueueUpdateBoosts(String indexId, String sourceDocId,	Map<Integer, Float> boosts) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Storage.Client client = new Storage.Client(protocol);
		
		try {
			transport.open();
			Map<Integer, Double> doubleBoosts = Maps.newHashMapWithExpectedSize(boosts.size());
		    for (Entry<Integer, Float> entry : boosts.entrySet()) {
				doubleBoosts.put(entry.getKey(), entry.getValue().doubleValue());
			}
			
			client.enqueueUpdateBoosts(indexId, sourceDocId, doubleBoosts);
			transport.close();
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
    public void enqueueUpdateCategories(String indexId, String sourceDocId, Map<String, String> categories) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Storage.Client client = new Storage.Client(protocol);

		try {
			transport.open();
			client.enqueueUpdateCategories(indexId, sourceDocId, categories);
			transport.close();
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}
    }

	@Override
	public void enqueueUpdateTimestamp(String indexId, String sourceDocId, int timestamp) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Storage.Client client = new Storage.Client(protocol);
		
		try {
			transport.open();
			client.enqueueUpdateTimestamp(indexId, sourceDocId, timestamp);
			transport.close();
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}

	}

    public static void main(String[] args) {
        System.out.println("HOLA");
	    StoringClient client = new StoringClient("localhost", 10000);
        int max = Integer.parseInt(args[0]);
        Random rnd = new Random();
        long t0 = System.currentTimeMillis();
        com.flaptor.indextank.index.Document doc = new com.flaptor.indextank.index.Document();
        doc.setField("text", "Hi, i was trying different implementations of this fix. The difference among those implementations are the way to choose the latitude to estimate a correction factor. Km and miles functions get 2 points: (lat1, long1, lat2, long2). So, i've tried three different aproaches. Choose the first latitude, an average and the max of both latitudes. Here they are some results i've got: Times are shown in millis. 'BetterThanOld' and 'GoodEnough' are the relations between BetterThanOld quantity over total quantity, and GoodEnough quantity over total quantity, respectively. GoodEnough criteria is relative error less than 0.15 (against great circle formula). any latitude KM: OldTime:   1799 --- NewTime:   1911 --- 1.06 times slower MILES: OldTime:   1781 --- NewTime:   1899 --- 1.07 times slower KM: BetterThanOld: 0.7853 --- GoodEnough: 0.8336 MILES: BetterThanOld: 0.7887 --- GoodEnough: 0.8347 average latitude KM: OldTime:   1808 --- NewTime:   1909 --- 1.06 times slower MILES: OldTime:   1815 --- NewTime:   1908 --- 1.05 times slower KM: BetterThanOld: 0.9523 --- GoodEnough: 0.8615 MILES: BetterThanOld: 0.9516 --- GoodEnough: 0.8622 max latitude KM: OldTime:   1823 --- NewTime:   1913 --- 1.05 times slower MILES: OldTime:   1788 --- NewTime:   1905 --- 1.07 times slower KM: BetterThanOld: 0.7868 --- GoodEnough: 0.8341 MILES: BetterThanOld: 0.7880 --- GoodEnough: 0.8337");
        Map<Integer,Float> boosts = new HashMap<Integer,Float>();
	    for (int i=1; i<max; i++) {
            if (i == 1500) t0 = System.currentTimeMillis();
            String docid = String.valueOf(rnd.nextInt(10000));
            System.out.print("Doc "+i+" - ");
            client.enqueueAddDocument("jorgetest", docid, doc, 0, boosts);
        }
        long s = (System.currentTimeMillis() - t0)/1000;
        System.out.println("Time: "+s+" seconds, "+((max-1500)/s)+" docs/s");
	}


}

