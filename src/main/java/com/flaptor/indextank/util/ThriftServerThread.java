package com.flaptor.indextank.util;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

public class ThriftServerThread {

    public static Thread forProcessor(final TProcessor processor, final String name, final int port) {
        return new Thread() { 
            public void run() { 
                try {
                    TServerSocket serverTransport = new TServerSocket(port);
                    Factory protFactory = new TBinaryProtocol.Factory(true, true);
                    TServer server = new TThreadPoolServer(processor, serverTransport, protFactory);
                    System.out.println("Starting " + name + " server on port " + port + " ...");
                    server.serve();
                } catch( TTransportException tte ){
                    tte.printStackTrace();
                }
            } 
        };
    }
    
}
