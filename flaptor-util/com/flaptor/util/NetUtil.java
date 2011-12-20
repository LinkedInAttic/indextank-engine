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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Assorted utilities for network handling.
 */
public class NetUtil {

    /**
     * Returns the list of local IP numbers.
     * @return an array list of String, each one representing an IP address local to this machine.
     */
    public static ArrayList<String> getLocalIPs () throws SocketException {
        ArrayList<String> ips = new ArrayList<String>();
        Enumeration<NetworkInterface> netcards = NetworkInterface.getNetworkInterfaces();
        while (netcards.hasMoreElements()) {
            Enumeration<InetAddress> inets = netcards.nextElement().getInetAddresses();
            while (inets.hasMoreElements()) {
                InetAddress inet = inets.nextElement();
                String addr = inet.getHostAddress();
                ips.add(addr);
            }
        }
        return ips;
    }

}

