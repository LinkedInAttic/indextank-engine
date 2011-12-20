package com.flaptor.util;

import java.io.File;

import org.apache.log4j.Logger;


/**
 * Use this class to avoid a serializing to be truncated by system exiting.
 * Given a file and an object, does this:
 * 
 *  object -> tempFile
 *  file -> oldFile
 *  tempFile -> file
 *  delete (oldFile)
 *  
 * @author Martin Massera
 *
 */
public class FileSerializer {
    
	private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private File file;
    private File oldFile;
    private File tempFile;
    
    public FileSerializer(File file) {
        this.file = file;
        oldFile = new File(file.getAbsolutePath()+".old");
        tempFile = new File(file.getAbsolutePath()+".temp");
    }
    
    public void serialize(Object obj, boolean compressed) {
        IOUtil.serialize(obj, tempFile.getAbsolutePath(), compressed);
        file.renameTo(oldFile);
        tempFile.renameTo(file);
        oldFile.delete();
    }
    
    public Object deserialize(boolean compressed) {
        Object obj = null;

        boolean ok = false;
        
        if (tempFile.exists()) {
            try {
                obj = IOUtil.deserialize(tempFile.getAbsolutePath(), compressed);
                tempFile.renameTo(file);
                ok = true;
            } catch (Throwable t) {
            	logger.error(t,t);
            }
            tempFile.delete();
        }
        if (!ok && file.exists()) {
            try {
                obj = IOUtil.deserialize(file.getAbsolutePath(), compressed);
                ok = true;
            } catch (Throwable t) {
            	logger.error(t,t);
            }
        }
        if (!ok && oldFile.exists()) {
            try {
                obj = IOUtil.deserialize(oldFile.getAbsolutePath(), compressed);
                oldFile.renameTo(file);
            } catch (Throwable t) {
            	logger.error(t,t);
            }
            oldFile.delete();
        }
        return obj;
    }
}
