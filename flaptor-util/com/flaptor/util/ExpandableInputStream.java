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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.apache.log4j.Logger;


/**
 * This is an InputStream that accepts lines with the form '#import &lt;resourcename&gt;' 
 * and expands the stream with the contents of the specified resource. 
 */
public class ExpandableInputStream extends InputStream {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private BufferedInputStream localStream = null;
    private ExpandableInputStream externalStream = null;
    private String externalName;
    private ClassLoader loader;
    private static HashSet<String> resourceset = new HashSet<String>();
    private boolean local = true;


    /**
     * Class initializer.
     */
    public ExpandableInputStream (InputStream stream, ClassLoader loader) {
        localStream = new BufferedInputStream(stream);
        this.loader = loader;
    }


    // Starts importing a resourcei. This is recursive, since it opens 
    // a substream using the same class, so if the imported file
    // contains a #import command, it will be expanded also. 
    private void startImport (InputStream is, String resourceName) throws IOException {
        externalName = resourceName;
        if (resourceset.contains(externalName)) {
            throw new IOException("There is a reference loop involving the resource " + externalName);
        }
        resourceset.add(externalName);
        externalStream = new ExpandableInputStream(is,loader);
        local = false;
    }


    // Stops importing a file, will continue reading from the local stream.
    private void stopImport () throws IOException {
        externalStream.close();
        resourceset.remove(externalName);
        local = true;
    }


    // Reads the rest of the line, until the line or stream end.
    private String readLine () throws IOException {
        StringBuffer line = new StringBuffer();
        while (localStream.available() > 0) {
            int ch = localStream.read();
            if (ch == '\n') break;
            line.append((char)ch);
        }
        return line.toString();
    }


    /**
     * Returns the number of bytes available for reading, or zero if the 
     * end of stream was reached.
     * If it was expanding an import command and there are no more bytes 
     * available from the external stream, it has to check with the local
     * stream.
     * @return the number of available bytes.
     */
    public int available () throws IOException {
        int av = 0;
        if (local) {
            av = localStream.available();
        } else {
            av = externalStream.available();
            if (av == 0) {
                stopImport();
                av = available();
            }
        }
        return av;
    }


    /**
     * Reads one byte from the input stream. If it encounters a '#import', 
     * reads to the end of the line to get the filename and expands the 
     * stream to include that file.
	 * @todo throws FileNotFoundException, but maybe that's not the most
	 *	appropiate exception to throw...
     */
    public int read() throws IOException {
        int ch = 0;
        if (local) {
            ch = localStream.read();
            if (ch == '#') {
                localStream.mark(100);
                String line = readLine();
                if (line.toLowerCase().startsWith("import ")) {
                    String resourceName = line.substring(7);
                    InputStream is = loader.getResourceAsStream(resourceName);
                    if (null != is) {
                        startImport(is,resourceName);
                        ch = read();
                    } else {
                        // TODO ResourceNotFound or so 
                        logger.error("Resource not found: " + resourceName);
                        throw new FileNotFoundException("Resource not found: " + resourceName);
                    }
                } else {
                    localStream.reset();
                }
            }
        } else {
            if (externalStream.available() > 0) {
                ch = externalStream.read();
            } else {
                stopImport();
                ch = read();
            }
        }
        return ch;
    }


    // Code for testing
    public static void main (String args[]) throws Exception {
        if (args.length == 0) {
            System.out.println("Please specify a file to import");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.out.println("File not found: "+f.getAbsolutePath());
            System.exit(1);
        }
        InputStream is = new ExpandableInputStream(new FileInputStream(f),ClassLoader.getSystemClassLoader());
        while (is.available() > 0) {
            System.out.print((char)is.read());
        }
        is.close();
    }

}
