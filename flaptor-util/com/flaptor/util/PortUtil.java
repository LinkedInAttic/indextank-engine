/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/

package com.flaptor.util;

/**
 * utilities for getting ports 
 * 
 * @author Martin Massera
 */
public class PortUtil {
	
	/**
	 * gets the base port from common.properties
	 */
	public static int getBasePort() {
        Config config = Config.getConfig("common.properties");
        return config.getInt("port.base");
	}

	/**
	 * gets an offset for a port name
	 * @param portName what follows after port.offset. (search for property port.offset.[portName])
	 * @return the offset
	 */
	public static int getOffset(String portName) {
        Config config = Config.getConfig("common.properties");
		return config.getInt("port.offset." + portName);
    }

	
	/**
	 * gets a port from common.properties, using base port configured
     * in common.properties.
     *
	 * @param portName what follows after port.offset. (search for property port.offset.[portName])
	 */
	public static int getPort(String portName) {
        Config config = Config.getConfig("common.properties");
        int basePort = config.getInt("port.base");
        return getPort(basePort,portName);
	}

    /**
     * Gets a port from common.properties, using basePort as the base,
     * so resulting port is basePort + offset. This is a Useful method 
     * for Stubs, to find out the port of a remote service, knowing
     * its base port.
     *
     * @param basePort
     *          The base port of the jvm exporting the service
     * @param portName
	 *          what follows after port.offset. 
     *          (search for property port.offset.[portName])
     * 
     * @return the port where the service should be available.
     *
     */
    public static int getPort(int basePort, String portName) {
        Config config = Config.getConfig("common.properties");
        int offset = config.getInt("port.offset." + portName);
		return basePort + offset;
    }

    /**
     * Parses a host[:basePort] string. If only host is supplied, assumes the currentBasePort
     * @param hostString can be "host" or "host:baseport"
     * @return a pair of the host and the baseport
     */
    public static Pair<String, Integer> parseHost(String hostString) {
        String[] hostSplit = hostString.split(":");
        String hostName = hostSplit[0];
        int port;
        if (hostSplit.length > 1) port = Integer.parseInt(hostSplit[1]);
        else port = getBasePort();
        return new Pair<String, Integer>(hostName, port);
    }
    
    /**
     * Parses a host[:basePort] string. If only host is supplied, assumes the currentBasePort
     * Then seeks the offset for the given port name.
     * 
     * @param hostString can be "host" or "host:baseport"
     * @param portName the name of the port to look the offset for
     * @return a pair of the host and the final port
     */
    public static Pair<String, Integer> parseHost(String hostString, String portName) {
    	Pair<String, Integer> base = parseHost(hostString);
    	return new Pair<String, Integer>(base.first(),  PortUtil.getPort(base.last(), portName));
    }
    
    /**
     * main method for getting ports from command line
     * options are
     * 
     * getPort portName
  	 * getPort portName basePort
     * getOffset portName
     */
    public static void main(String[] args) {
		if (2 > args.length  || args.length > 3 ) printUsage();
		if ("getPort".equals(args[0])) {
			if (args.length == 2) System.out.println(getPort(args[1]));
			else System.out.println(getPort(Integer.parseInt(args[2]), args[1]));
		} else if ("getOffset".equals(args[0])) {
			System.out.println(getOffset(args[1]));
		} else printUsage();
	}
    
    private static void printUsage() {
    	String usage = 
    		"usage:\n"+
    		" getPort portName"+
    		" getPort portName basePort"+
    		" getOffset portName";
		System.out.println(usage);
    	System.exit(1);
    }
}
