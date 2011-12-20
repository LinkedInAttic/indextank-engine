package com.flaptor.indextank.rpc;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.google.common.collect.Maps;

public class IndexerClient implements com.flaptor.indextank.BoostingIndexer {
	private final String host;
	private final int port;

	public IndexerClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public void promoteResult(String docid, String query) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.promoteResult(docid, query);
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
	public void add(String docId, com.flaptor.indextank.index.Document document, int timestampBoost, Map<Integer, Double> dynamicBoosts) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.addDoc(docId, new Document(document.asMap()), timestampBoost, dynamicBoosts);
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
	public void dump() {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.dump();
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
	public void updateBoosts(String docId, Map<Integer, Double> updatedBoosts) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.updateBoost(docId, updatedBoosts);
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
	public void updateCategories(String docId, Map<String, String> categories) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.updateCategories(docId, categories);
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
	public void updateTimestamp(String docId, int timestampBoost) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.updateTimestampBoost(docId, timestampBoost);
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
	public void del(String docid) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.delDoc(docid);
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
	public void addScoreFunction(int functionIndex, String definition) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.addScoreFunction(functionIndex, definition);
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
	public void removeScoreFunction(int functionIndex) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			client.removeScoreFunction(functionIndex);
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
	public Map<Integer,String> listScoreFunctions() {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		Indexer.Client client = new Indexer.Client(protocol);
		
		try {
			transport.open();
			Map<Integer,String> retValue = client.listScoreFunctions();
			transport.close();
            return retValue;
		} catch (IndextankException e) {
			throw new RuntimeException(e);
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}
	}



    @Override
    public Map<String, String> getStats() {
        TSocket transport = new TSocket(host, port);
        TProtocol protocol = new TBinaryProtocol(transport);
        Indexer.Client client = new Indexer.Client(protocol);
        
        try {
            transport.open();
            Map<String,String> retValue = client.get_stats();
            transport.close();
            return retValue;
        } catch (IndextankException e) {
            throw new RuntimeException(e);
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }



    public static void main(String[] args) {
    	IndexerClient client = new IndexerClient("localhost", 7911);

        Map<String,String> fields = Maps.newHashMap();
        fields.put("mono","lete");
        fields.put("ignacio","perez");
        fields.put("santi","miente");
        fields.put("jorge","grumpy");
        fields.put("spike","sweet");

        String extra = "";
        if (args.length > 1 ) {
            fields.put(args[0],args[1]);
            extra = args[0] + " " + args[1];
        } 

        fields.put("todo","mono lete ignacio perez santi miente jorge grumpy spike y sweet " + extra);
        com.flaptor.indextank.index.Document doc = new com.flaptor.indextank.index.Document(fields);
        client.add("prueba", doc, (int)(System.currentTimeMillis() / 1000), Maps.<Integer, Double>newHashMap());
    }


}

