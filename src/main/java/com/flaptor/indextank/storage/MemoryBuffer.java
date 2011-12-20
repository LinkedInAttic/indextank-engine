/**
 * 
 */
package com.flaptor.indextank.storage;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;

class MemoryBuffer {
    TMemoryBuffer transport = new TMemoryBuffer(4096);
    TProtocol protocol = new TBinaryProtocol.Factory().getProtocol(transport);
}