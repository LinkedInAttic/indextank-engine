package com.flaptor.indextank.rpc;

import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class SuggestorClient {

    public static void main(String[] args) {
        TTransport transport;
        try {
            transport = new TSocket("localhost", 2000);
            TProtocol protocol = new TBinaryProtocol(transport);
            Suggestor.Client client = new Suggestor.Client(protocol);

            transport.open();
            List<String> suggestions = client.complete(args[0], "text");
            transport.close();
            System.out.println(suggestions);
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        }
    }

}

