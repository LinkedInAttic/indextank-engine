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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.apache.log4j.Logger;


/**
 * This is an InputStream that accepts lines with conditional expression using '#ifdef &lt;token&gt;' 
 * and '#endif &lt;token&gt;' and parses out the lines in between depending on the provided set of
 * defined tokens.
 */
public class ConditionalInputStream extends InputStream {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private BufferedInputStream stream = null;
    private HashSet<String> defines = null;
    private int nested = 0;


    /**
     * Class initializer.
     * @param stream the input stream.
     * @param defines a set of defined tokens.
     */
    public ConditionalInputStream (InputStream stream, HashSet<String> defines) {
        this.stream = new BufferedInputStream(stream);
        if (null == defines) {
            this.defines = new HashSet<String>();
        } else {
            this.defines = defines;
        }
    }


    /**
     * Returns the number of bytes available for reading, or zero if the
     * end of stream was reached.
     * @return the number of available bytes.
     */
    public int available () throws IOException {
        return stream.available();
    }


    // Reads the rest of the line, until the line or stream end.
    private String readLine () throws IOException {
        StringBuffer line = new StringBuffer();
        while (stream.available() > 0) {
            int ch = stream.read();
            if (ch == '\n') break;
            line.append((char)ch);
        }
        return line.toString();
    }


    /**
     * Reads one byte from the input stream. If it encounters a '#ifdef', 
     * it checks to see if the condition is met, and if not it consumes 
     * all lines until the corresponding #endif line.
     */
    public int read() throws IOException {
        int ch = stream.read();
        if (ch == '#') {
            stream.mark(100);
            String line = readLine();
            if (line.toLowerCase().startsWith("ifdef ")) {
                String defineName = line.substring(6).trim();
                nested++;
                if ( ! defines.contains(defineName)) {
                    int level = 1;
                    do {
                        line = readLine();
                        if (line.trim().toLowerCase().startsWith("#ifdef ")) {
                            level++;
                        }
                        if (line.trim().toLowerCase().startsWith("#endif")) {
                            level--;
                        }
                    } while (level > 0);
                }
                ch = (available() > 0) ? read() : -1;
            } else if (line.toLowerCase().startsWith("endif")) {
                nested--;
                if (nested < 0) {
                    throw new RuntimeException("found #endif without #ifdef");
                }
                ch = (available() > 0) ? read() : -1;
            } else {
                stream.reset();
            }
        }
        return ch;
    }


    /**
     * Reads to a byte buffer.
     * @param buf byte buffer where the data will be read into.
     * @return the number of bytes read or -1 if no more chars available.
     */
    public int read (byte[] buf) throws IOException {
        if (available() == 0) {
            return -1;
        }
        int i = 0;
        while (i<buf.length && available()>0) {
            int ch = read();
            if (ch != -1) {
                buf[i++] = (byte)ch;
            }
        }
        return i;
    }


    // Code for testing
    public static void main (String args[]) throws Exception {
        if (args.length < 2) {
            System.out.println("Please specify a file to process and a list of defined terms");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.out.println("File not found: "+f.getAbsolutePath());
            System.exit(1);
        }
        HashSet<String> set = new HashSet<String>();
        for (int i=1; i<args.length; i++) {
            set.add(args[1]);
        }
        InputStream is = new ConditionalInputStream(new FileInputStream(f), set);
        System.out.print(IOUtil.readAll(is));
        is.close();
    }

}
