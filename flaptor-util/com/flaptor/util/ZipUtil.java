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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/** 
 * This class zips and unzips directories
 */
public class ZipUtil {
    static final int ZIP_BUFFER = 4*1024;
    static Logger logger = Logger.getLogger(Execute.whoAmI());

    /**
     * Zip a directory into a file with the same name as the directory with
     * the "zip" extension.
     * @param directory the path of the directory to be zipped.
     */
    public static String zipDirectory(String directory) {
        String zipName = directory + ".zip";
        zipDirectory(directory, zipName);
        return zipName;
    }

    /**
     * Zip a directory into a file with the given name.
     * @param directory the path of the directory to be zipped.
     * @param zipName the name of the zipped file.
     */
    public static void zipDirectory(String directory, String zipName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            int endpos = directory.lastIndexOf('/');

            byte data[] = new byte[ZIP_BUFFER];
            // get a list of files from current directory
            File f = new File(directory);
            String files[] = getRecursiveFileList(f);

            for (int i=0; i<files.length; i++) {
			    logger.debug("Adding: " + files[i]);
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, ZIP_BUFFER);
                String fileName = files[i];

                String relativeName = fileName.substring(endpos+1,fileName.length());
                // assume that host is on UNIX and replaces slashes
                relativeName = relativeName.replace('\\','/');

                ZipEntry entry = new ZipEntry(relativeName);
                out.putNextEntry(entry);
                int count;
                while((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch(Exception e) {
            logger.error("zipDirectory: " + e, e);
        }
    }

    /**
     * Unzip a file. The file may contain files and directories.
     * @param fileName the name of the zip file.
     * @return the name of the unziped file or directory.
     */
	public static String unzipFile(String fileName) throws IOException {
		BufferedOutputStream dest = null;
		BufferedInputStream is = null;
		ZipEntry entry;
		ZipFile zipfile = new ZipFile(fileName);
		Enumeration<? extends ZipEntry> e = zipfile.entries();
	    File f = new File(fileName);
		String zipFilePath = f.getParent();
        String rootFileName = null;
        String lastRoot = null;
        boolean rootIsOne = true;
		while(e.hasMoreElements()) {
			entry = e.nextElement();

			logger.debug("Extracting: " + entry);
			is = new BufferedInputStream(zipfile.getInputStream(entry));

            String entryName = entry.getName();
            int p = entryName.indexOf(File.separator);
            if (p < 0) {
                rootFileName = entryName;
            } else {
                rootFileName = entryName.substring(0,p);
            }
            if (null != lastRoot) {
                if (!rootFileName.equals(lastRoot)) {
                    rootIsOne = false;
                }
                lastRoot = rootFileName;
            }

            String entryFileName = zipFilePath + File.separator + entry.getName();
			File zipFileEntry = new File(entryFileName);
			File zipFileDir = zipFileEntry.getParentFile();
			zipFileDir.mkdirs();

			int count;
			byte data[] = new byte[ZIP_BUFFER];
			FileOutputStream fos = new FileOutputStream(entryFileName);
			dest = new BufferedOutputStream(fos, ZIP_BUFFER);
			while ((count = is.read(data, 0, ZIP_BUFFER)) != -1) {
				dest.write(data, 0, count);
			}

			dest.flush();
			dest.close();
			is.close();
			fos.close();
		}

		zipfile.close();
        String rootPath;
        if (rootIsOne) {
            rootPath = zipFilePath + File.separator + rootFileName;
        } else {
            rootPath = zipFilePath;
        }
        return rootPath;
	}

    /**
     * Returns a list of files within the given directory
     * @param f the directory to list.
     * @return the list fo files.
     */
    public static String[] getRecursiveFileList(File f) {
        Vector<String> v = new Vector<String>();

        String[] files = f.list();
        if (files==null) {
            return new String[0];
        }

        for (int i=0; i<files.length; i++) {
            File subFile = new File(f.getAbsolutePath() + File.separator + files[i]);
            if (subFile.isDirectory()) {
                String[] childFiles = getRecursiveFileList(subFile);
                for (int j=0; j<childFiles.length; j++) {
                    v.add(childFiles[j]);
                }
            } else {
                v.add(subFile.getPath());
            }
        }

        return (String[]) v.toArray(new String[v.size()]);
    }


    public static void main(String argv[]) throws Exception {
        String usage = "usage: ZipUtil [c|x] name";
        if (argv.length != 2) {
            System.out.println(usage);
            return;
        }
        String op = argv[0].toLowerCase();
        String name = argv[1];
        // int num = Integer.parseInt(argv[1]);

        if (op.equals("c")) {
            zipDirectory(name);
        } else if (op.equals("x")) {
            unzipFile(name);
        } else {
            System.out.println(usage);
        }

    }


}

