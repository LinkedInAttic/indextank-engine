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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.log4j.Logger;

/**
 * This class provides static methods for file manipulation.
 * 
 * @author Martin Massera
 */
public final class FileUtil {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    //so that it cannot be instantiated.
    private FileUtil() {}

    static final String CHECKSUM_FILENAME = "checksum";


    /**
     * Deletes a file or directory.
     * @param spec the directory to delete.
     * @return true if successful, false otherwise.
     */
    public static boolean deleteDir (File spec) {
        if (spec.isDirectory()) {
            String[] children = spec.list();
            for (int i=0; i<children.length; i++) {
                if (!deleteDir(new File(spec, children[i]))) {
                    return false;
                }
            }
        }
        return spec.delete();
    }

    /**
     * Deletes a file or directory.
     * @param path the name of the file or dir to delete.
     * @return true if successful, false otherwise.
     */
    public static boolean deleteDir (String path) {
        return deleteDir(new File(path));
    }

    /**
     * Deletes a file or directory.
     * @param spec the file or dir to delete.
     * @return true if successful, false otherwise.
     */
    public static boolean deleteFile (File spec) {
        return deleteDir(spec);
    }

    /**
     * Deletes a file or directory.
     * @param path the name of the file or dir to delete.
     * @return true if successful, false otherwise.
     */
    public static boolean deleteFile (String path) {
        return deleteDir(new File(path));
    }



    /**
     * Renames a file or directory.
     * @param oldName the original name of the file.
     * @param newName the name the file should be renamed to.
     * @return true if successful, false otherwise.
     */
    public static boolean rename (String oldName, String newName) {
        File fold = new File(oldName);
        File fnew = new File(newName);
        return fold.renameTo(fnew);
    }

    /**
     * Copies one file to another, optionally appending.
     * @param from the file to be copied.
     * @param to the destination file.
     * @return true if successful, false otherwise.
     */
    public static boolean copyFile (File from, File to, boolean append) {
        boolean ok = true;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(from), 64*1024);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to,append), 64*1024);
            byte[] buf = new byte[8*1024];
            int len = 0;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            logger.warn("Copying file "+from.getName()+" to "+to.getName()+": "+e);
            ok = false;
        }
        return ok;
    }

    /**
     * Copies one file to another, overwriting the destination file.
     * @param from the file to be copied.
     * @param to the destination file.
     * @return true if successful, false otherwise.
     */
    public static boolean copyFile (File from, File to) {
        return copyFile(from,to,false);
    }

    /**
     * Copies one file to another
     * @param from the name of the file to be copied.
     * @param to the name of the destination file.
     * @return true if successful, false otherwise.
     */
    public static boolean copyFile (String from, String to) {
        return copyFile (new File(from), new File(to));
    }


    /**
     * Gets the size of a file or directory, in bytes.
     * This method is needed because the File.length() 
     * method is not defined for directories.
     * @param spec the file or directory whose size is requested.
     * @return the size of the file or directory.
     */
    public static long sizeOf(File spec) {
        long size = 0;
        if (spec.isDirectory()) {
            String[] children = spec.list();
            for (int i=0; i<children.length; i++) {
                size += sizeOf(new File(spec, children[i]));
            }
        } else {
            size = spec.length();
        }
        return size;
    }


    /**
     * @see fileToSet(String,String,Set<String>,Logger) 
     *
     * It uses a default Logger
     */
    public static boolean fileToSet(final String dirname, final String filename, final Set<String> set) {
        return fileToSet(dirname,filename,set,logger);
    }

    /**
     * Loads the lines from a file into a set.
     * @param dirname the name of the directory. If null, it gets replaced by ".".
     * @param filename the name of the file (may include path information).
     * @param set the set that will hold the lines from the file.
     * @param logger
     *          the logger where to log errors.
     * @return true if ok, false in case of an error.
     * @throws no exception is thrown by this implementation.
     *         The returned value must be checked to verify that no error has ocurred.
     */
    public static boolean fileToSet(final String dirname, final String filename, final Set<String> set,Logger logger) {
        List<String> list = new ArrayList<String>();
        boolean ret = fileToList(dirname, filename, list,logger);
        if (ret) {
            set.addAll(list);
        }
        return ret;
    }


    /**
     * @see fileToList(String,String,List<String>,Logger) 
     *
     * It uses a default Logger
     */
    public static boolean fileToList(final String dirname, final String filename, final List<String> list) {
        return fileToList(dirname,filename,list,logger);
    }
    

    /**
     * Loads the lines from a file into a list.
     * @param dirname the name of the directory. If null, it gets replaced by ".".
     * @param filename the name of the file (may include path information).
     * @param list the list that will hold the lines from the file.
     * @param logger
     *          the Logger where to log errors.
     * @return true if ok, false in case of an error.
     * @throws no exception is thrown by this implementation.
     *         The returned value must be checked to verify that no error has ocurred.
     */
    // Rafa assumes in com.flaptor.hounder.classifier.util.ProbsUtils#loadMaps(String, String, int)}
    // that this method returns a list in the same order that the file it reads.
    // If you change that, please update the training app.     
    public static boolean fileToList(final String dirname, final String filename, final List<String> list, Logger logger) {
        boolean ret = false;
        String fullname = getFullName(dirname, filename);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fullname));
            while (reader.ready()) {
                list.add(reader.readLine());
            }
            ret = true;
        } catch (IOException e) {
            logger.error("fileToList: error reading the file " + fullname + ": " +e,e);
        } finally {
            Execute.close(reader);
        }
        return ret;
    }

    /**
     * Saves the elements of a set into a file.
     * @param dirname the name of the directory where the file will be saved.
     * @param filename the name of the file.
     * @param set the set that holds the lines for the file.
     * @return true if ok, false in case of an error.
     * @throws no exception is thrown by this implementation.
     *         The returned value must be checked to verify that no error has ocurred.
     */
    public static boolean setToFile(final String dirname, final String filename, final Set<String> set) {
        List<String> list = new ArrayList<String>(set);
        return listToFile(dirname, filename, list,logger);
    }



    /**
     * @see listToFile(String,String,List<String>,Logger) 
     *
     * It uses a default Logger
     */
    public static boolean listToFile(final String dirname, final String filename, final List<String> list) {
        return listToFile(dirname,filename,list,logger);
    }

    

    /**
     * Saves the elements of a list into a file.
     * @param dirname the name of the directory where the file will be saved.
     * @param filename the name of the file.
     * @param list the list that holds the lines for the file.
     * @return true if ok, false in case of an error.
     * @throws no exception is thrown by this implementation.
     *         The returned value must be checked to verify that no error has ocurred.
     */
    // Rafa assumes in com.flaptor.hounder.classifier.util.ProbsUtils#loadMaps(String, String, int)}
    // and com.flaptor.hounder.classifier.LearningBean#saveData() 
    // that this method will save the file with the same order it received the list
    // If you change that, please update the training app.     
    public static boolean listToFile(final String dirname, final String filename, final List<String> list, Logger logger) {
        boolean ret = false;
        String fullname = FileUtil.getFullName(dirname, filename);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fullname));
            for (String line : list) {
                writer.write(line);
                writer.newLine();
            }
            ret = true;
        } catch (IOException e) {
            logger.error("listToFile: error writing to the file " + fullname + ": " +e,e);
        } finally {
            Execute.close(writer);
        }
        return ret;
    }

    /**
     * Creates a fully qualified path from a dirname and a filename.
     * @param dirname the name of the directory. If null, it gets replaced by ".".
     * @param filename the name of the file (may include path information).
     * @return a String with the full name of the file.
     */
    private static String getFullName(final String dirname, final String filename) {
        File f = new File(dirname, filename);
        return f.getAbsolutePath();
    }




    /**
     * Computes the checksum of a given file and adds it to the provided
     * Checksum object.
     * 
     * @param chk the Checksum object.
     * @param fname the name of the file to check it's checksum.
     * @throws java.io.FileNotFoundException, java.io.IOException
     */
    private static void checksumFile(final Checksum chk, final String fname) throws IOException {
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fname);
            bis = new BufferedInputStream(fis);
            int len = 0;
            byte[] bytes = new byte[8192];
            while ((len = bis.read(bytes)) >= 0) {
                chk.update(bytes, 0, len);
            }
        } finally {
            Execute.close(bis);
            Execute.close(fis);
        }
    }

    /**
     * Recursively computes the checksum of a given directory tree and adds it
     * to the provided Checksum object.
     * @throws java.io.FileNotFoundException, java.io.IOException
     */
    private static void checksumDir(Checksum chk, String path) throws IOException {
        File dir = new File(path);
        if (!dir.exists()) {
            throw new FileNotFoundException(
                    "FileUtil.checksumDir: The directory " + path
                            + " does not exist");
        }
        if (dir.isDirectory()) {
            String[] names = dir.list();
            // Needed because list() method may sort different the files
            // depending on implementation, and CRC32 is non-commutative.
            Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
            for (int i = 0; i < names.length; i++) {
                if (!names[i].endsWith(CHECKSUM_FILENAME)) {
                    String filespec = path + File.separatorChar + names[i];
                    File file = new File(filespec);
                    if (file.isDirectory()) {
                        checksumDir(chk, filespec);
                    } else {
                        checksumFile(chk, filespec);
                    }
                }
            }
        }
    }

    /** 
     * Returns the absolute path of the given directory. 
     * @throws java.io.IOException
	 * @todo see the method's body
     */
    public static String getDir(String path) throws IOException {
        File f = null;
        if (path.startsWith(Character.toString(File.separatorChar))) {
            f = new File(path);
        } else {//XXX TODO: is this necessary?
            f = new File(".", path);
        }
        return f.getCanonicalPath();
    }

    /** 
     * Returns the checksum of a given directory tree. 
     * @throws java.io.IOException
     */
    public static long computeChecksum(String path) throws IOException {
        String dir = getDir(path);
        Checksum checksum = new CRC32();
        checksumDir(checksum, dir);
        return checksum.getValue();
    }

    /**
     * Stores the checksum of a given directory tree in a file at the root of
     * that directory.
     * @throws java.io.IOException
	 * @todo the serialization of the checksum is done using a hack. Serialize
	 * 	properly using ObjectOutputStream.writeLong
     */
    public static void createChecksum(String path) throws IOException {
        long checksum_long = computeChecksum(path);
        byte[] checksum = String.valueOf(checksum_long).getBytes();
        File f = new File(getDir(path), CHECKSUM_FILENAME);
        FileOutputStream os = new FileOutputStream(f);
        os.write(checksum); // checksum is long, write expects byte[]
        os.close();
    }

    /**
     * Computes the checksum of a given directory tree and verifies that it
     * matches the checksum stored in a file at the root the that directory
     * @throws java.io.IOException, java.io.FileNotFoundException
	 * @todo deserialize properly @link #createChecksum()
     */
    public static boolean verifyChecksum(String path) throws IOException {
//        boolean eq = false;
        long checksum_long = computeChecksum(path);
        byte[] checksum1 = String.valueOf(checksum_long).getBytes();
        byte[] checksum2 = new byte[checksum1.length];
        File f = new File(getDir(path), CHECKSUM_FILENAME);
        if (!f.exists()) {
            throw new FileNotFoundException("FileUtil.verifyChecksum: Missing checksum file in " + path);
        }
        FileInputStream is = null; 
        try {
            is = new FileInputStream(f);
            //XXX TODO: see comment about serialization in createChecksum
            is.read(checksum2);
            return java.util.Arrays.equals(checksum1, checksum2);
        }
        finally {
            Execute.close(is);
        }
    }
    
    /**
     * Creates temporary directory.
     * @param prefix a string to use at the beginning of the directory's name.
     * @param suffix  a string to use at the end of the directory's name.
     * @return a newly created (empty) directory on the system's tmp directory.
     */
    public static File createTempDir (final String prefix, final String suffix) throws IOException {
        File retVal = null;
        java.util.Random random = new java.util.Random(System.currentTimeMillis());
        int retry = 0;
        while ((null == retVal) && retry < 20) {
            retVal = new File(System.getProperty("java.io.tmpdir") + File.separator + prefix + random.nextInt() + suffix);
            if (!retVal.mkdir()) {
                retVal = null;
                retry++;
            }
        }
        if  (null == retVal) {
            throw new IOException("Can't create a temporary directory in "+System.getProperty("java.io.tmpdir"));
        }
        retVal.deleteOnExit();
        return retVal;
    }

    /**
     * Creates a directory or gets it if it exists. Can check for reading and writing permission.
     * Can take a directory of any depth even if none exists:
     * example /var/local/doesntExist1/doesntExist2/doesntExist3
     * 
     * @param path the directory to be created or gotten
     * @param mustBeReadable whether to check for reading permission
     * @param mustBeWritable whether to check for writing permission
     * @return a File representing that directory 
     * 
     * @throws IOException if the directory cannot be created or permissions dont match
     */
    public static File createOrGetDir(String path, boolean mustBeReadable, boolean mustBeWritable) throws IOException {
        File dir = new File(path);
        createIfDoesntExist(dir, mustBeReadable, mustBeWritable);
        return dir;
    }

    /**
     * creates a directory if it doesnt exist and verifies permissions
     * @param dir
     * @param mustBeReadable
     * @param mustBeWritable
     * @throws IOException
     */
    public static void createIfDoesntExist(File dir, boolean mustBeReadable, boolean mustBeWritable) throws IOException {
        if (dir.exists()) {
            if (!dir.isDirectory()) throw new IOException(dir.getAbsolutePath() + " exists but isn't a directory");
        } else {
            if (!dir.mkdirs()) throw new IOException("Couldn't create directory " + dir.getAbsolutePath() );
        }

        if (mustBeReadable && !dir.canRead()) throw new IOException("Cannot read from " + dir.getAbsolutePath() );   
        if (mustBeWritable && !dir.canWrite()) throw new IOException("Cannot write in " + dir.getAbsolutePath() );
    }

    
    /**
     * gets a file that exists and matches the requested permissions or throws an exception
     * checks if it s a file or a directory
     * 
     * @param path the file path
     * @param mustBeReadable whether to check for reading permission
     * @param mustBeWritable whether to check for writing permission
     * @param isDirectory true if it should be a directory, false if a file
     * @return a File representing that file 
     */
    public static File getExistingFile(String path, boolean mustBeReadable, boolean mustBeWritable, boolean isDirectory) throws IOException{
    	File file = getExistingFile(path, mustBeReadable, mustBeWritable);
        if (file.isDirectory() && !isDirectory) throw new IOException(file.getAbsolutePath() + " exists but is not a file" );
        if (!file.isDirectory() && isDirectory) throw new IOException(file.getAbsolutePath() + " exists but is not a directory" );
		return file;
	}

    /**
     * gets a file that exists and matches the requested permissions or throws an exception
     * (can be a file or a directory)
     * 
     * @param path the file path
     * @param mustBeReadable whether to check for reading permission
     * @param mustBeWritable whether to check for writing permission
     * @return a File representing that file 
     */
    public static File getExistingFile(String path, boolean mustBeReadable, boolean mustBeWritable) throws IOException{
    	File file = new File(path);
    	if (!file.exists()) throw new FileNotFoundException("file not found: " + file.getAbsolutePath());
        if (mustBeReadable && !file.canRead()) throw new IOException("Cannot read from " + file.getAbsolutePath() );   
        if (mustBeWritable && !file.canWrite()) throw new IOException("Cannot write in " + file.getAbsolutePath() );
		return file;
	}
    
    /** 
     * Gets the absolute path of a file with given filename, searching
     * classpath.
     *
     * Useful to use log4j's PropertyConfigurator even if log4j.properties is 
     * not placed in the working directory of the jvm.
     *
     * @param filename
     *              The name of the file that has to be searched in the
     *              classpath
     * @return the absolute path of the file if found, or null otherwise.
     */
    public static String getFilePathFromClasspath(String filename) {
        
        java.net.URL url = ClassLoader.getSystemClassLoader().getResource(filename);
        if (null != url && "file".equals(url.getProtocol())) { 
            return url.getPath();
        }

        // resource not found as "file://"
        return null;

    }

    /**
     * writes a file with the given content. If content == null or "" it writes an empty file
     * @param file
     * @param content
     * @throws IOException
     */
    public static void writeFile(File file, String content) throws IOException {
    	Writer w = new FileWriter(file);
    	if (content != null) w.write(content);
    	w.flush();
    	w.close();
    }
    
    /**
     * reads an entire char based file into a string 
     * @param file
     * @return the contents in a string
     * @throws IOException
     */
	public static String readFile(File file) throws IOException {
		return IOUtil.readAll(new FileReader(file));
	}

    /**
     * Tool to create and verify checksums, and to delete directories.
     */
    public static void main(String[] argv) {

        try {
            if (argv.length != 2) {
                System.err.println("Usage: FileUtil (delete|createchk|verifychk) <dir>");
                System.exit(1);
            }
            String cmd = argv[0];
            String dir = argv[1];
            if (cmd.equals("delete")) {
                deleteDir(dir);
                System.out.println("ok");
            }
            if (cmd.equals("createchk")) {
                createChecksum(dir);
                System.out.println("ok");
            }
            if (cmd.equals("verifychk")) {
                boolean ok = verifyChecksum(dir);
                System.out.println("Verification result: " + ok);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
