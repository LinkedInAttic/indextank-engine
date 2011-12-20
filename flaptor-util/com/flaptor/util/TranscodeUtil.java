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

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * Utility class to manipulate strings encoding.
 * It provides methods to convert between hex and url-encoded representations, and
 * between binary and hex representations.
 * All methods return null incase an error ocurrs.
 * A NullPointerException is thrown if the argument is null.
 **/
public class TranscodeUtil {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    /** Returns the hex representation of the argument, or null if it is invalid. */
    public static String urlencToHex(String urlenc) {
        try {
            return binToHex(java.net.URLDecoder.decode(urlenc, "ISO-8859-1").getBytes());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /** Returns the url-encoded (ISO-8859-1) representation of the arugment, or null if it is invalid. */
    public static String hexToUrlenc(String hex) {
        try {
            return (java.net.URLEncoder.encode(new String(hexToBin(hex)), "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    // Used by binToHex
    private static final String hexdigits = "0123456789ABCDEF";

    // Returns the hex representation of the argument
    public static String binToHex(byte[] bin) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<bin.length; i++) {
            int value = bin[i] < 0 ? bin[i] + 256 : bin[i]; // signed -> unsigned
            buf.append(hexdigits.charAt(value/16));
            buf.append(hexdigits.charAt(value%16));
        }
        return buf.toString();
    }

    // Returns the hex representation of the argument, or null if the argument is not hex or its length not even
    public static byte[] hexToBin(String hex) {
        if ((hex.length() % 2) != 0) { return null; }
        byte[] buf = new byte[hex.length()/2];
        int j=0;
        for (int i=0; i<hex.length(); i+=2) {
            try {
                buf[j++] = (byte)Integer.parseInt(hex.substring(i,i+2),16);
            } catch (NumberFormatException e) {
                logger.error("hexToBin: " + e, e);
                return null;
            }
        }
        return buf;
    }

    //to use from the command line
    public static void main (String args[]) {
        if (args.length == 2) {
            try {
                java.lang.reflect.Method m = TranscodeUtil.class.getMethod(args[0], new Class[] { String.class });
                System.out.println((String)m.invoke(null, new Object[] {args[1]}));
            } catch (Exception e) {
                System.err.println("Error executing " +args[0] + "[" +e);
                e.printStackTrace();
            }
        } else {
            System.err.println("Usage: com.flaptor.util.TranscodeUtil < binToHex | hexToBin | hexToUrlenc | urlencToHex > value");
        }
    }
}
