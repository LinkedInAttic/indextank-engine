package com.flaptor.indextank.rpc;

import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.query.ParseException;
import com.google.common.base.Preconditions;

public class SuggestorServer { 

    private com.flaptor.indextank.suggest.Suggestor suggestor;
    private int port;

    public SuggestorServer(com.flaptor.indextank.suggest.Suggestor suggestor, int port){
        Preconditions.checkNotNull(suggestor);
        Preconditions.checkArgument(port > 0 && port < (1<<16));
        this.suggestor = suggestor;  
        this.port = port;
    }

    public void start(){
        Thread t = new Thread() { 
        
            public void run() { 
                try {
                    TServerSocket serverTransport = new TServerSocket(SuggestorServer.this.port);
                    Suggestor.Processor processor = new Suggestor.Processor(new SuggestorImpl(SuggestorServer.this.suggestor));
                    Factory protFactory = new TBinaryProtocol.Factory(true, true);
                    TServer server = new TThreadPoolServer(processor, serverTransport, protFactory);
                    System.out.println("Starting suggestor server on port " + SuggestorServer.this.port + " ...");
                    server.serve();
                } catch( TTransportException tte ){
                    tte.printStackTrace();
                }
            } 
        };
        t.start();
    }

    private class SuggestorImpl implements Suggestor.Iface {
        private com.flaptor.indextank.suggest.Suggestor suggestor;

        // constructor
        private SuggestorImpl(com.flaptor.indextank.suggest.Suggestor suggestor){
            this.suggestor = suggestor;
        }

        public List<String> complete(String partialQuery, String field) {
            return suggestor.complete(partialQuery, field);
        }
    } 

    public static void main(String[] args) throws ParseException {
    }

}
