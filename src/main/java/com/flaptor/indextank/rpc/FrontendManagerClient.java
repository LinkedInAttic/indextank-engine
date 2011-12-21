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

import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

public class FrontendManagerClient {
	private final String host;
	private final int port;

	public FrontendManagerClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void saveInsight(String indexCode, String insightCode, String jsonValue) {
		TSocket transport = new TSocket(host, port);
		TProtocol protocol = new TBinaryProtocol(transport);
		FrontendManager.Client client = new FrontendManager.Client(protocol);
		
		try {
			transport.open();
			client.save_insight(indexCode, insightCode, jsonValue);
			transport.close();
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		} catch (TException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<IndexInfo> listIndexes() {
	    TSocket transport = new TSocket(host, port);
	    TProtocol protocol = new TBinaryProtocol(transport);
	    FrontendManager.Client client = new FrontendManager.Client(protocol);
	    
	    try {
	        transport.open();
	        List<IndexInfo> indexes = client.list_indexes();
	        transport.close();
	        return indexes;
	    } catch (TTransportException e) {
	        throw new RuntimeException(e);
	    } catch (TException e) {
	        throw new RuntimeException(e);
	    }
	}
	
}

