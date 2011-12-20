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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
/**
 * utility class for common IO operations
 * 
 * @author Martin Massera
 */
public class IOUtil {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    /**
     * fully reads a reader
     * @return a string with all the contents of the reader 
     * @throws IOException 
     */
    public static String readAll(Reader reader) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(reader);
            StringBuffer buf = new StringBuffer();
            char[] buffer = new char[256];
            while(true) {
                int charsRead = br.read(buffer);
                if (charsRead == -1) break;
                buf.append(buffer, 0, charsRead);
            }
            return buf.toString();
        } finally {
            Execute.close(br, logger);
        }
    }


    /**
     * fully reads a stream
     * Warning, it does not close the passed InputStream.
     * @return a string with all the contents of the stream 
     * @throws IOException 
     */
    public static String readAll(InputStream stream) throws IOException {
        StringBuffer buf = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = stream.read(buffer);
            if (bytesRead == -1) break;
            buf.append(new String(buffer, 0, bytesRead));
        }
        return buf.toString();
    }
    /**
     * fully reads a stream
     * @return a string with all the contents of the stream 
     * @throws IOException 
     */
    public static byte[] readAllBinary(InputStream stream) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = stream.read(buffer);
            if (bytesRead == -1) break;
            o.write(buffer, 0, bytesRead);
        }
        return o.toByteArray();
    }

    /**
     * reads the last n bytes of a file
     * @param stream
     * @param numBytes
     * @return
     * @throws IOException
     */
    public static byte[] tail(File file, long numBytes) throws IOException {
        RandomAccessFile raFile = null;
        try {
            raFile = new RandomAccessFile(file, "r");
            long position = raFile.length() - numBytes;
            if (position < 0) position = 0;
            raFile.seek(position);
            byte[] ret = new byte[(int)(raFile.length() - position)];
            raFile.read(ret);
            return ret;
        } finally {
            if (raFile != null) raFile.close();
        }
        
    }

    public static void serialize(Object o, OutputStream os, boolean compress) throws IOException {
        GZIPOutputStream gos = null;
    	ObjectOutputStream oos = null;
        try {        
            if (compress) {
                gos = new GZIPOutputStream(os);
                os = gos;
            }
            oos = new ObjectOutputStream(os);
            oos.writeObject(o);
            oos.flush();
            if (gos != null) {
                gos.flush();
            }
        } finally {
            Execute.close(oos, logger);
            Execute.close(gos, logger);
        }
    }

    /**
     * method for quick serialization of an object
     * @param o
     * @param file
     */
    public static void serialize(Object o, String file, boolean compress) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            serialize(o, fos, compress);
            fos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Execute.close(fos, logger);
        }
    }
    
    public static byte[] serialize(Object o, boolean compress) {
        ByteArrayOutputStream out = null;
    	try { 
	    	out = new ByteArrayOutputStream();
	    	serialize(o, out, compress);
	    	out.flush();
	    	return out.toByteArray();
    	} catch (IOException e) { //this exception should not happen
    		throw new RuntimeException(e);
    	} finally {
            Execute.close(out, logger);
        }
    }


    /**
     * quick deserializing from a file, to use with serialize(object, file)
     * @param file
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(String file, boolean compressed){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            Object obj = deserialize(fileInputStream, compressed);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Execute.close(fileInputStream, logger);
        }
    }

    public static Object deserialize(InputStream is, boolean compressed) {
        GZIPInputStream gis = null;
        ObjectInputStream ois = null;
    	try {
    	    if (compressed) {
    	        gis = new GZIPInputStream(is);
    	        is = gis;
    	    }
            ois = new ObjectInputStream(is);
    	    Object obj = ois.readObject();
    	    return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Execute.close(ois, logger);
            Execute.close(gis, logger);
        }
    }

   	public static Object deserialize(byte[] bytes, boolean compressed) throws IOException, ClassNotFoundException {
    	ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    	Object obj = deserialize(bis, compressed);
    	bis.close();
    	return obj;
    }
   	
   	public static String getStackTrace(Throwable t) {
        java.io.CharArrayWriter caw = new java.io.CharArrayWriter();
        t.printStackTrace(new java.io.PrintWriter(caw)); 
        return caw.toString();
   	}
   	
   	/**
     * reads all non empty lines 
     * @param toLowerCase if true makes every line lowercase
   	 * @return a list of strings with a line per element
   	 * @throws IOException 
   	 */
   	public static List<String> readLines(boolean toLowerCase, boolean trim, boolean emptyLines, Reader reader) throws IOException {
   	    BufferedReader bufferedReader = null;
        try {
            List<String> ret = new ArrayList<String>();
            bufferedReader = new BufferedReader(reader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    return ret;
                }
                if (toLowerCase) line = line.toLowerCase();
                if (trim) line = line.trim();
                if (!emptyLines && line.length() == 0) continue;
                ret.add(line);
            }
        } finally {
            Execute.close(bufferedReader, logger);
        }
   	}
   	
   	/**
   	 * returns an iterable (for use in fors) for iterating over the lines of a file.
   	 * The difference with readLines is that readLines reads the entire file and this reads the stream
   	 * when requested. Use this for very big files. 
   	 * 
   	 * @param toLowerCase
   	 * @param trim
   	 * @param emptyLines
   	 * @param reader
   	 * @return
   	 * @throws IOException
   	 */
   	public static Iterable<String> lineIterable(final boolean toLowerCase, final boolean trim, final boolean emptyLines, final Reader reader) throws IOException {
   	    final BufferedReader br = new BufferedReader(reader);
   	    return new Iterable<String>() {
            String nextLine = getNextLine();
            String getNextLine() throws IOException{
                String line = null;
                while (true){
                    line = br.readLine();
                    if (line == null) break;
                    if (trim) line = line.trim();
                    if (emptyLines || line.length() > 0) break;
                }
                return line;
            }
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    public boolean hasNext() {
                        return nextLine != null;
                    }
                    public String next() {
                        String line = nextLine;
                        try {
                            nextLine = getNextLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            if (nextLine == null) {
                                Execute.close(br, logger);
                            }
                        }                            
                        return line;
                    }
                    public void remove() {throw new UnsupportedOperationException("remove not supported");}
                };
            }
   	    };
   	}
   	
   	public static void main(String[] args) throws IOException, ClassNotFoundException {
        
   	    byte[] bytes = IOUtil.serialize(Lists.newArrayList(1,2,3,4,5), true);
   	    System.out.println(IOUtil.deserialize(bytes, true));
    }
}
