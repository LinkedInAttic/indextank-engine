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
package com.flaptor.util.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TranscodeUtil;
import java.lang.reflect.Method;


/**
 * This class implements a disk-based hash table. 
 * The cached data/object must implement Serializable
 * 
 * This class is threadsafe, each thread should use its own iterator to iterate over the cache keys.
 * 
 * @todo implement a entry-iterator so as to avoid duplicate file access when iterating over the keys and values
 * 
 * actual VERSION: 2 - generic FileCache
 * supports reading VERSION 1 (byte[] FileCache)
 * 
 * @author Flaptor Development Team
 */
public class FileCache<T> implements Iterable<String>, RmiCache<T>{

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    
    //private static byte VERSION = 1; //old byte[] fileCache 
    private static byte VERSION = 2;
    
    protected String cachedir = null;
    protected final ADigester digester; 

    
    private int subdirDepth = 2;

    private boolean writeToTempFiles = false;  

    /** 
     * Initializes the cache with a default subdirectory depth of 2 levels (subdir/subdir/file)
     * @param cachedir the directory of the cache.
     */ 
    public FileCache (String cachedir) {
        this.digester = new SHADigester();
    	construct(cachedir);
    }
    
    private boolean useZipStream= true;

    /**
     * By default, the data in the cacho is stored ziped.
     * This contructor can be used to store it unzip.
     * Note: that you are the only responsible for calling the addItem and the
     * all the retrieve item method (getItem/iteration,hasItem...) with the same
     * kind of FileCache: If you added the item with a FileCache constructed 
     * using zip=true, and then tries to read with a FileCache constructed 
     * using zip=false, strange things can happen.
     * @param zip true iff the data is (to be) stored compressed
     * @param @param cachedir the directory of the cache.
     * note: the other constructors set zip=true.
     */
    // This ctor is used in the BayesCalculator
    public FileCache (boolean zip, String cachedir) {
        this(cachedir);
        useZipStream= zip;
    }

    /**
     * This constructor behaves like {@link #FileCache(boolean, String)} and also 
     * takes an additional writeToTempFiles parameter which causes the write operations
     * to occur in a temporary file that will replace the original once the write operation
     * has ended. This allows for another thread or process to read the item that's being 
     * written without risking to get a partially written file. However this does not handle
     * the possibility of two threads or processes writting to the same file at the same time.
     *  
     * @param cachedir
     * @param zip
     * @param writeToTempFiles
     */
    public FileCache(String cachedir, boolean zip, boolean writeToTempFiles) {
        this(zip, cachedir);
        this.writeToTempFiles = writeToTempFiles;
    }

    /** 
     * Initializes the cache with a specified subdirectory depth
     * @param cachedir the directory of the cache.
     * @param subdirDepth the subdirectory depth (2 means subdir/subdir/file). Should be strictly larger than 0 and lower than 20 
     */ 
    public FileCache (String cachedir, int subdirDepth) {
    	this.subdirDepth = subdirDepth;
        this.digester = new SHADigester();
    	construct(cachedir);
    }
    

    /**
     * Initializes a cache, using a digester that will always generate
     * colissions. This constructor should be only called for testing,
     * and behaves like this:
     *
     * If boolean parameter is false, logs ERROR and throws an 
     * IllegalArgumentException.
     *
     * If boolean parameter is true, logs WARN and continues.
     *
     * THIS CONSTRUCTOR IS ONLY USEFUL FOR TESTING COLLISIONS
     *
     */
    public FileCache (String cachedir, boolean generateCollisions) {
        if (false == generateCollisions) {
            logger.error("Using testing constructor, for a \"legitimate\" use.");
            throw new IllegalArgumentException("Using testing constructor");
        }

        logger.warn("Using testing constructor");
        construct(cachedir);
        this.digester = new ConstantDigester();
    }




    //construction code
    private void construct(String cachedir) {
        this.cachedir = cachedir;
        File dir = new File(cachedir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Adds an item to the cache.
     * @param key an identification for the item.
     * @param data the contents of the item.
     */
    public void addItem (String key, T value) {
        File file = getAssociatedFile(key);
        
        if (writeToTempFiles) {
            file = getTempFileForWritting(file);
        }
        
        ObjectOutputStream stream = null;
        try {
            if (useZipStream){
                stream = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
            } else {
                stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            }
            writeVersion(stream,VERSION);
            writeKey(stream, key);
            writeValue(stream, value);
        } catch (IOException e) {
            logger.error("Adding an item to the cache: " + e, e);
        } finally {
            try { 
                if (null != stream) {
                    stream.close(); 
                }
                
                if (writeToTempFiles) {
                    moveTempFile(file);
                }
            } catch (IOException e) {
            }
        }
    }

    private File getTempFileForWritting(File file) {
        return new File(file.getAbsolutePath() + ".writting");
    }

    private void moveTempFile(File tempFile) {
        String path = tempFile.getAbsolutePath();
        String original = path.substring(0, path.length() - ".writting".length());
        if (!FileUtil.rename(path, original)) {
            logger.error("Unable to rename temporary cache file to actual cache file.");
        }
    }

    // Writes version number to a stream
    private void writeVersion (ObjectOutputStream stream, byte version) {
        try {
            stream.writeByte(version);
        } catch (IOException e) {
            logger.error("Writing a version to a cache item file", e);
        }
    }

    // Writes a block of data to a file.
    // Blocks contain a two-byte length field, followed by the block data.
    private void writeValue(ObjectOutputStream stream, T value) {
        try {
            stream.writeObject(value);
        } catch (IOException e) {
            logger.error("Writing the value to a cache item file", e);
        }
    }

    // Writes key
    private void writeKey(ObjectOutputStream stream, String key){
        try {
            stream.writeObject(key);
        } catch (IOException e) {
            logger.error("Writing key to a cache item file", e);
        }
    }

    private ObjectInputStream open(File file) throws IOException {
        ObjectInputStream stream = null;
        if (useZipStream){
            stream = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
        } else {
            stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
        }
        return stream;
    }

    
    /**
     * @return the version number or -1 if the version is wrong
     */
    private static int verifyVersion(ObjectInputStream stream) throws IOException {
        byte version = readVersion(stream);
        if (0 < version && version <= VERSION ) {
            return version;
        } else {
            logger.error("Wrong version: found " + version + " expected:" + VERSION);
            Execute.close(stream);
            return -1;
        }
    }
    
    protected ObjectInputStream openAndVerifyVersion(File file) throws IOException {
        ObjectInputStream stream = open(file);
        int version = verifyVersion(stream);
        return (version >= 0) ? stream : null;
    }


    // Reads a block of data from a file.
    // Blocks contain a two-byte length field, followed by the block data.
    @SuppressWarnings("unchecked")
    private T readValue (ObjectInputStream stream) {
        try {
            return (T)stream.readObject();
        } catch (Throwable t) {
            logger.error("Reading value from a cache item file.", t);
            return null;
        }
    }
    
    /** Reads a block of data from a file.
     *  Blocks contain a two-byte length field, followed by the block data.
     *  for compatibility with old VERSION 1
     */
    private byte[] readByteArrayValue(ObjectInputStream stream) {
        try {
            int blocklen = stream.readInt();
            byte[] block = new byte[blocklen];
            stream.readFully(block);
            return block;
        } catch (IOException e) {
            logger.error("Reading a block to a cache item file: " + e, e);
        }
        return null;
    }
    

    // Reads version number from a stream.
    private static byte readVersion (ObjectInputStream stream) {
        try {
            return stream.readByte();
        } catch (IOException e) {
            logger.error("Reading version number to a cache item file", e);
        }
        return 0;
    }

    // Reads key from a stream.
    protected static String readKey (ObjectInputStream stream) {
        try {
            return (String)stream.readObject();
        } catch (Exception e) {
            logger.error("Reading a key to a cache item file.", e);
        }
        return null;
    }

    /**
     * Returns true if the item is in the cache.
     * @param key an identification for the item.
     * @return true if the item is in the cache.
     */
    public boolean hasItem (String key) {
        if (null == key ) return false;
        return getAssociatedFile(key).exists();
    }

    /**
     * Retrieves an item from the cache.
     * @param key an identification for the item.
     * @return the contents of the cached item, or null if the item is not cached.
     */
    @SuppressWarnings("unchecked")
    public T getItem (String key) {
        T value = null;
        if (null == key) {
            logger.error("Can't retrieve cache item with null key.");
        } else {
            try {
                File file = getAssociatedFile(key);
                if (!file.exists()) {
                    logger.debug("There is no cache item for this key: " + key + " (" + digester.getDigest(key) + ")");
                } else {
                    boolean ok = false;

                    ObjectInputStream stream = null;
                    try {
                        stream = open(file);
                        int version = verifyVersion(stream); 
                        if (version < 0) return null;
                        String storedKey = readKey(stream);
                        if (null != storedKey) {
                            if (!key.equals(storedKey)) {
                                logger.error("Cache colission between the following keys: [" + key + "]; [" + storedKey + "]");
                            } else {
                                if (version == VERSION) {
                                    value = readValue(stream);
                                    if (null != value) ok = true;
                                } else if (version == 1) { 
                                    //to support old cache, T must be byte[]
                                    value = (T) readByteArrayValue(stream); 
                                    if (null != value) ok = true;
                                }
                            }
                        }
                    } catch (EOFException e) {
                        logger.error("data incomplete for " + key + " - deleting", e);
                        removeItem(key);
                    } catch (IOException e) {
                        logger.error("Getting " + key + " from the cache", e);
                    } finally {
                        try { if (null != stream) stream.close(); } catch (IOException e) {}
                    }
                    if (!ok) {
                        logger.error("Corrupted cache file: unexpected end of file.");
                    }
                }
            } catch (UnsupportedEncodingException e) {
                logger.error(e,e);
            }
        }
        return value;
    }

    /**
     * Deletes an item from the cache.
     * @param key an identification for the item.
     * @return true if the item was deleted (or didn't exist), false otherwise
     */
    public boolean removeItem (String key) {
        File file = getAssociatedFile(key);
        if (file.exists()) {
            boolean deleted = file.delete();
            File dir = file.getParentFile();
            if (dir.list().length == 0) dir.delete();
            return deleted;
        }
        // else
        return true;
    }
    
    /**
     * Returns true if the item is in the cache.
     * @param key an identification for the item.
     * @return true if the item is in the cache.
     */
    public Date getItemDate (String key) {
        if (null == key ) return new Date(0);
        File file = getAssociatedFile(key);
        return new Date(file.lastModified());
    }


    //returns the subdirectory where a digest should be placed,
    // according to the subdirectory depth level
    private File getSubdir(String digest) {
        StringBuffer subdir = new StringBuffer();
        for (int i = 0; i < subdirDepth; ++i) {
            subdir.append(digest.substring(i * 2, i * 2 + 2) + File.separator);
        }
        return new File(cachedir, subdir.toString());
    }

    // Finds a File associated with a key. It solves collisions, and returns
    // as follows:
    // A file that exists, if the key was stored on the cache
    // A file that does not exist, if the key was not found.
    //
    // The file, no matter if it exists or not, will have collisions solved,
    // so if you wanted to know if a key was stored, check if the file exists.
    // If you want to write a key, you will get a file to write the key and
    // its content, and you can choose to overwrite if it existed or not.
    private File getAssociatedFile (String key) {
        try {
            String digest = digester.getDigest(key);
            File dir = getSubdir(digest);
            File file = new File(dir,digest);
            // If the dir did not exist, its obvious that the
            // file does not exist either. create dir and return new file.
            if (!dir.exists()) {
                dir.mkdirs();
                return file;
            }

            File[] synonyms = dir.listFiles(new CollisionFilenameFilter(digest));
            int max = -1;

            for (File syn: synonyms ) {
                ObjectInputStream stream = null;
                try {
                    stream = openAndVerifyVersion(syn);
                } catch (IOException e) {
                    logger.warn("could not open and verify version on " + syn.getAbsolutePath());
                    Execute.close(stream);
                    continue;
                }

                if (null == stream) {
                    logger.warn("could not open and verify version on " + syn.getAbsolutePath());
                    continue;
                }

                String storedKey = readKey(stream);
                Execute.close(stream);
                if (key.equals(storedKey)) {
                    return syn;
                }

                // So, it was not what we expected. Find collision index.
                String[] parts = syn.getName().split("-");
                int num = 0;
                if (2 == parts.length ) {
                    num = Integer.parseInt(parts[1]);
                }
                max = Math.max(max,num);
            }

            // If we got out of the for loop, we did not find it. Just create
            // a new file with the specified name, and append max+1 if not 0
            if (max >= 0) {
                file = new File(dir,digest + "-" + (max+1));
            }
            return file;

        } catch (UnsupportedEncodingException e) {
            logger.error(e,e);
            // IT MAKES NO SENSE TO CONTINUE IF THE HARDCODED ENCODING IS 
            // NOT SUPPORTED BY PLATFORM. CACHE WILL BE USELESS AND CORRUPTED
            // WITH DUPLICATES, ETC.
            throw new RuntimeException(e);
        }
    }



    public Iterator<String> iterator() {
        return new CacheIterator();
    }

   
    /** Implements the iterator of keys in the cache
    */
    protected class CacheIterator implements Iterator<String>{
        private Stack<File[]> enumFiles = null;
        private Stack<Integer> enumFileIndex = null;
        private File currentFile = null;
        private File nextFile = null;

        protected CacheIterator() {
            enumFiles = new Stack<File[]>();
            enumFileIndex = new Stack<Integer>();
            File root = new File(cachedir);
            if (root.list().length > 0) {
                enumFiles.push(new File[] {root}); // root level
                enumFileIndex.push(-1);

                currentFile = null;
                advanceNextFile();
            } else {
                currentFile = null;
                nextFile = null;
            }
        }

        public boolean hasNext() {
            return null != nextFile;
        }

        public String next() {
            String key = null;
            if (hasNext()) {
                String storedKey = null;
// Comented out because there is no other way to communicate that a cache entry is wrong except returning null.
//                while (null == storedKey && hasNext()) {
                currentFile = nextFile;
                advanceNextFile();

                ObjectInputStream stream = null; 
                try {
                    stream = openAndVerifyVersion(currentFile);
                    if (stream == null) return null;
                    storedKey = readKey(stream);
                } catch (IOException e) {
                    logger.error("Getting an enumeration key from the cache: " + e, e);
                } finally {
                    try { if (null != stream) stream.close(); } catch (IOException e) {}                    
                }
//                }
                if (null != storedKey) key = storedKey;
            }
            return key;	        
        }

        public void remove() {
            currentFile.delete();
        }

        // Advance the iterator to the next cache file.
        private synchronized void advanceNextFile() {
            do {
                nextFile = null;
    
                File[] files = enumFiles.peek();
                int index = enumFileIndex.peek() + 1;
                while(index < files.length)
                {
                    index = enumFileIndex.pop() + 1;
                    enumFileIndex.push(index);
    
                    File file = enumFiles.peek()[index];
    
                    if (!file.exists()) continue; //if the file doesnt exist (it was deleted) keep going
                    if (file.isFile()) { //if the file exists, set it as currentFile and end
                        nextFile = file;
                        return;
                    }
    
                    if (file.isDirectory()) {//if it is a directory, get into the directory
                        enumFiles.push(file.listFiles());
                        enumFileIndex.push(-1);
                        advanceNextFile();
                        return;
                    }
                }
                //we finished all files in the directory, so get out of it
                enumFiles.pop();
                enumFileIndex.pop();
            } while (enumFiles.size() > 0);//if we have nothing in enumFiles, we just popped the root directory and we are done 
        }
    }


    protected interface ADigester {
        public String getDigest(String param) throws UnsupportedEncodingException;
    }

    private class SHADigester implements ADigester {
        private MessageDigest md = null;

        public SHADigester() {
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                logger.error("Initializing the cache: " + e, e);
                throw new RuntimeException(e);
            }    	
        }

        public String getDigest (String key) throws UnsupportedEncodingException {
            return TranscodeUtil.binToHex(md.digest(key.getBytes("UTF-8"))); 
        }
    }

    private static class ConstantDigester implements ADigester {
        public String getDigest (String param) throws UnsupportedEncodingException {
            return "constant";
        }

    }


    private class CollisionFilenameFilter implements java.io.FilenameFilter {
        private String prefix;

        public CollisionFilenameFilter(String digest) {
            this.prefix = digest;
        }

        public boolean accept(File dir,String filename) {
            return filename.startsWith(prefix);
        }
    }
    
    private static void helpAndExit() {
        String usage = "\nCache <command> <args> [-dates <dd-mm-yyyy> <dd-mm-yyyy>]\n"
        	+"\nCommands:\n"
        	+"  list <dir>\n"
            +"      Lists the keys for the stored objects.\n"
        	+"  get <dir> [<key>]\n"
            +"      Shows the result of calling toString() on the stored objects.\n"
            +"      If the <key> parameter is not given, the command is applied to all the stored objects.\n"
        	+"  getprop <dir> [<method> [<key>]]\n"
            +"      Shows the result of calling the given method on the stored objects.\n"
            +"      If no method is given, it shows callable methods.\n"
            +"      If the <key> is not given, it applies to all the stored items.\n"
        	+"  translate <src-dir> <src-file-depth> <dest-dir> <dst-file-depth>\n"
            +"      Copies the cache to a new location using a different file depth structure.\n";
        System.out.println(usage);
        System.exit(1);
    }
    
    private static String[] extractDateRange(String[] args, Date[] range) {
    	ArrayList<String> param = new ArrayList<String>();
    	boolean datesFound = false;
    	for (int i=0; i<args.length; i++) {
    		if (args[i].startsWith("-dates")) {
    			String from = args[++i];
    			String to = args[++i] + " 23:59:59";
    			range[0] = new SimpleDateFormat("dd-MM-yyyy").parse(from, new ParsePosition(0));
    			range[1] = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(to,new ParsePosition(0));
    			if (range[0].after(range[1])) {
    				System.out.println("Start date should not be after end date");
    				System.exit(1);
    			}
    			datesFound = true;
    		} else {
    			param.add(args[i]);
    		}
    	}
    	if (!datesFound) {
			range[0] = new Date(0);
			range[1] = new Date(Long.MAX_VALUE);
    	}
    	return param.toArray(new String[]{});
    }
    
    private static boolean betweenDates(Date date, Date[] range) {
    	boolean beforeStart = date.before(range[0]);
    	boolean afterEnd = date.after(range[1]);
		return (!beforeStart && !afterEnd); // between both dates, inclusive
    }

    private static void showEntry(FileCache cache, String key, Object data, Date[] range) {
    	if (null == data) {
    		System.out.println("Page not found in cache");
    	} else {
        	Date date = cache.getItemDate(key);
    		if (betweenDates(date, range)) {
    			String str = (data instanceof byte[]) ? new String((byte[])data) : data.toString();
    			System.out.println("-------");
    			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
    			System.out.println();
    			System.out.println(key);
				System.out.println();
				System.out.println(str);
				System.out.println();
			}
		}
    }
    
    private static Object getProperty(Object object, String property) {
        if (object == null) return null;
        try {
            return object.getClass().getMethod(property, new Class[0]).invoke(object, new Object[0]);
        } catch (SecurityException e) {
            System.out.println("Unable to obtain property " + property + " from class "+ object.getClass());
        } catch (NoSuchMethodException e) {
            System.out.println("Unable to obtain property " + property + " from class "+ object.getClass());
        } catch (IllegalArgumentException e) {
            System.out.println("Unable to obtain property " + property + " from class "+ object.getClass());
        } catch (IllegalAccessException e) {
            System.out.println("Unable to obtain property " + property + " from class "+ object.getClass());
        } catch (InvocationTargetException e) {
            System.out.println("Unable to obtain property " + property + " from class "+ object.getClass());
        }
        return null;
    }
    
    private static void showCallableMethods(Object obj) {
       System.out.println("Stored object is of class "+obj.getClass()+" which implements the following callable methods");
        for (Method mtd : obj.getClass().getMethods()) {
            if (mtd.getParameterTypes().length == 0 && ! "void".equals(mtd.getReturnType().getName())) {
                System.out.println("   "+mtd.getName()+" returns "+mtd.getReturnType().getName());
            }
        }
    }



    public static void main (String[] args) {
        if (args.length < 2) {
    		helpAndExit();
    	}
        Date[] range = new Date[2];
        String[] param = extractDateRange(args, range);
        int arglen = param.length;
/*    	if ("getbytes".equals(param[0])) {
    		FileCache<byte[]> cache = new FileCache<byte[]>(param[1]);
    		switch (arglen) {
    		case 2:
    			Iterator<String> it = cache.iterator();
    			while (it.hasNext()) {
    				String key = it.next();
    				byte[] data = cache.getItem(key);
    				showEntry(cache, key, data, range);
    			}
    			System.out.println("-------");
    			break;
    		case 3:
    			byte[] data = cache.getItem(param[2]);
    			showEntry(cache, param[2], data, range);
    			break;
    		default: helpAndExit();
    		}
    	} else*/
        if ("get".equals(param[0]) || "getobj".equals(param[0])) {
    		FileCache<Object> cache = new FileCache<Object>(param[1]);
    		switch (arglen) {
    		case 2:
    			Iterator<String> it = cache.iterator();
    			while (it.hasNext()) {
    				String key = it.next();
    				Object data = cache.getItem(key);
    				showEntry(cache, key, data, range);
    			}
    			System.out.println("-------");
    			break;
    		case 3:
    			Object data = cache.getItem(param[2]);
    			showEntry(cache, param[2], data, range);
    			break;
    		default: helpAndExit();
    		}
    	} else if ("getprop".equals(param[0])) {
    		FileCache<Object> cache = new FileCache<Object>(param[1]);
            Iterator<String> it = null;
    		switch (arglen) {
            case 2:
    			it = cache.iterator();
    			if (it.hasNext()) {
    				String key = it.next();
    				Object obj = cache.getItem(key);
                    showCallableMethods(obj);
                } 
                break;
    		case 3:
                String key = param[2];
                Object obj = cache.getItem(key);
                if (null != obj) {
                    showCallableMethods(obj);
                } else {
                    it = cache.iterator();
                    while (it.hasNext()) {
                        key = it.next();
                        obj = cache.getItem(key);
                        String prop = param[2];
                        Object data = getProperty(obj, prop);
                        if (null == data) {
                            System.out.println("Unable to obtain property "+prop+" from class "+obj.getClass());
                        }
                        showEntry(cache, key, data, range);
                    }
                    System.out.println("-------");
                }
    			break;
    		case 4:
                key = param[3];
                obj = cache.getItem(key);
                String prop = param[2];
                Object data = getProperty(obj, prop);
                if (null == data) {
                    System.out.println("Unable to obtain property "+prop+" from class "+obj.getClass());
                }
    			showEntry(cache, key, data, range);
    			break;
    		default: helpAndExit();
    		}
    	} else if ("list".equals(param[0])) {
    		if (arglen == 2) {
    			FileCache<Object> cache = new FileCache<Object>(param[1]);
    			Iterator<String> en = cache.iterator();
    			while (en.hasNext()) {
    				String key = en.next();
                	Date date = cache.getItemDate(key);
                	if (betweenDates(date, range)) {
                		System.out.println(key);
                	}
    			}
    		} else {
    			helpAndExit();
    		}
    	} else if ("purge".equals(param[0])) {
    		if (arglen == 2) {
    			FileCache<Object> cache = new FileCache<Object>(param[1]);
    			Iterator<String> en = cache.iterator();
    			while (en.hasNext()) {
    				Object value = en.next();
    				if (null == value) {
    					System.out.println("Removing faulty cache entry");
    					en.remove();
    				}
    			}
    			System.out.println("Done.");
    		} else {
    			helpAndExit();
    		}
    	} else if ("translate".equals(param[0])) {
    		if (arglen == 5) {
    			FileCache<byte[]> cacheSrc = new FileCache<byte[]>(param[1], Integer.parseInt(param[2]));
    			FileCache<byte[]> cacheDest = new FileCache<byte[]>(param[3], Integer.parseInt(param[4]));

    			for (String key: cacheSrc) {
                	Date date = cacheSrc.getItemDate(key);
                	if (betweenDates(date, range)) {
	    				System.out.println("translating " + key);
	    				cacheDest.addItem(key, cacheSrc.getItem(key));
                	}
    			}
    			System.out.println("Done.");
    		} else {
    			helpAndExit();
    		}
    	}
    }

}

