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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is implemented as a convenient way to store the application properties
 * and make them accessible. It periodically reads the configuration file and
 * updates the values of the variables. Warning: there can be only one config file
 * in an instance of the virtual machine.
 */
public class Config implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    // The configCache keeps an instance of the Config object for each config file.
    private static final Map<String,Config> configCache = new HashMap<String,Config>();
    private static final Map<String,Object> mutexCache = new HashMap<String,Object>();

    // Variables local to each instance
    private final String filename;
    private String filePath;
    private Properties prop;

    /**
     * Retrieves a Config representation of System.getProperties().
     *
     * It is like System.getProperties(), but it has the methods 
     * to extract properties casted as bools, ints, longs, arrays, etc.
     *
     */
    public static Config getSystemConfig() {
        Properties props = System.getProperties();
        return new Config("SYSTEM_PROPERTIES", new Properties(props));
    }

    /**
     * Get an empty config. Usefull for testing purposes, for example to create a 
     * config and add a list of fixed properties to it.
     */
    public static Config getEmptyConfig() {
        return new Config("PROPERTIES", new Properties());
    }

    /**
     * Create a named empty config. 
     */
    public static Config getEmptyConfig(String filename) {
        return new Config(filename, new Properties());
    }

    /**
     * Create a Config with given name and properties.
     */
    private Config(String name, Properties props) {
        this.prop = props;
        this.filename = name;
    }

    protected Config() {
        this.prop = null;
        this.filename = null;
    }


    /**
     * Initializes the class with the name of the properties file.
     * @param filename the properties file name
     * @throws IllegalStateException in case of an error accessing the filename for the first time (further calls are satisfied by the cache).
     *
     * @fixme non cached calls for non-default loaders
     */
    public static Config getConfig(String filename) {
        return getConfig(filename, Execute.myCallersClass().getClassLoader());
    }
    
    public static Config getConfig(String filename, ClassLoader loader) {
        if (filename == null) {
            System.err.println("Invalid filename = " +filename);
            System.exit(-1);
        }

        // FIXME hack to avoid conflicts when 2 different loadres
        // ask for the same config. Just parse and return 
        if (!loader.equals(Config.class.getClassLoader())) {
            return new Config(filename,loader);
        }


        // First try in the cache.
        Config config = null;
        Object configMutex = null;
        // Guarded by Config.class because the method is static.
        synchronized(Config.class) {
            configMutex = mutexCache.get(filename);
            if (configMutex == null) {
                configMutex = new Object();
                mutexCache.put(filename, configMutex);
            }
        }
        // Guarded by the mutext corresponding to the filename passed as argument.
        // Two simultaneous calls for different filenames won't contend, as each one is using its own mutex.
        // In the case the filename is the same, it doesn't matter which request will gain access to the mutex.
        // If the file already existed, both will have a mutex corresponding to the file.
        // If it is the first time, only one of the threads will be able to create the mutex, making it available for the oter thread.
        // After getting the mutext, any of them can update the cache indistinctly.  The other one will simply read it from the cache at its own time.
        synchronized(configMutex) {
            config = configCache.get(filename);
            if (config == null) {
                config = new Config(filename,loader);
                configCache.put(filename, config);
            }
        }
        return config;
    }


    /**
     * It constructs a new config object with the data found on the specified file.
     * @throws IllegalStateException in case of an error accessing the filename
     */
    private Config(String filename, ClassLoader loader) {
        this.filename = filename;
        this.filePath = getPathFromClasspath(filename, loader);
        prop = readFromClasspath(filename,loader);
        if (prop == null) {
            throw new IllegalStateException("Couldn't read resource " +filename);
        }
    }


    // Internal generic property getter
    protected synchronized String get(String key) {
        String p = prop.getProperty(key);
        if (null == p) {
            throw new IllegalStateException("Property " +key+ " not found");
        }
        return p.trim();
    }

    /**
     * Sets a key-value pair. This overrides the value found in the
     * configuration file, if there was one. It's probably only useful to write
     * tests cases, so it's unnecessary to write and ad-hoc configuration file.
     * To avoid having forgotten lines in your code and hard to find bugs, it
     * writes a "warn" to the logger. 
     *
     */
    public synchronized Config set(String key, String value) {
        logger.warn("Forcing new key-value pair in the configuration. Key: "
                + key + ", value: " + value);
        prop.setProperty(key, value);
        return this;
    }

    /**
     * writes the config back to disk, ONLY modifying 
     * the file that has been used to create the config (not the imported ones),
     * but only adding the things that has changed in the overall config (with imports)
     * 
     * the idea is to mantain the structure of the original file
     *  
     * @throws IOException
     */
    public void saveToDisk() throws IOException {
    	
    	//get the old config
    	Properties oldProps = readFromClasspath(filename, this.getClass().getClassLoader()); //TODO use the original classLoader?
    	save(oldProps, new File(filePath));
    }

    /**
     * writes the properties to disk overwriting a file, 
     * as in saveToDisk, maintaining the structure of the dest file
     * and adding/modifying only changes 
     * 
     * @param propertiesDestFile the properties file to be modified
     * @throws IOException
     */
    public void modifyOnDisk(File propertiesDestFile) throws IOException {
    	Properties oldProps = readFromStream(new FileInputStream(propertiesDestFile), this.getClass().getClassLoader());
    		readFromClasspath(filename, this.getClass().getClassLoader()); //TODO use the original classLoader?
    	save(oldProps, propertiesDestFile);
    }

    private void save(Properties oldProps, File destFile) throws IOException {
    	Map<Object, Object> change = new HashMap<Object, Object>();   	

    	for (Map.Entry<Object, Object> e : prop.entrySet()) {
    		//if a new property is not the same as an old property
    		if (!e.getValue().equals(oldProps.get(e.getKey()))) {
    			change.put(e.getKey(), e.getValue());
    		}
    	}
    	
    	BufferedReader reader = new BufferedReader(new FileReader(destFile));
    	List<String> lines = new ArrayList<String>();
    	String accumulatedLine = "";
    	while (true) {
    		String line = reader.readLine();
    		if (line == null) {
    			if (accumulatedLine.length() > 0) lines.add(accumulatedLine);
    			break;
    		}
    		if (line.trim().endsWith("\\")) {
    			if (accumulatedLine.length() > 0) accumulatedLine += "\n";
    			accumulatedLine += line;
    		} else {
    			if (accumulatedLine.length() > 0) {
    				line = accumulatedLine + "\n" + line;
        			accumulatedLine = "";
    			}
        		lines.add(line);
    		}
    	}
    	reader.close();
    	for (Map.Entry<Object, Object> e : change.entrySet()) {
    		if (!replace(lines, (String)e.getKey(), e.getValue())) {
    			lines.add("");
    			lines.add(getLine((String)e.getKey(), e.getValue()));
    		}
    	}
    	
    	Writer writer = new BufferedWriter(new FileWriter(destFile));
    	for (String line: lines) {
    		writer.write(line + "\n");
    	}
    	writer.flush(); writer.close();
    }
    
    /**
     * replaces a property in the list of lines 
     */
    private boolean replace(List<String> lines, String key, Object value) {
    	for (int i = 0; i < lines.size(); ++i) {
    		String line = lines.get(i).trim();
			int at = line.indexOf("=");   		
			if (at < 0) continue;
			String propertyName = line.substring(0, at).trim();
			if (propertyName.equals(key)) {
				lines.set(i, getLine(key, value));
				return true;
			}
		}
		return false;
    }
    
    /**
     * @return the property line corresponding to the key,value pair
     */
    private String getLine(String key, Object value) {
    	String ret = key+"=";
    	if (value instanceof String[]) {
    		String[] val = (String[]) value;
    		for (int i = 0; i < val.length; ++i) {
    			ret+=val[i];
    			if (i < val.length - 1) ret+=",";
    		}
    	} else {
    		ret+=value.toString();
    	}
    	return ret;
    }
    
    /**
     * Returns all the keys of the config's parameters. Using this method is possible to navigate the config.
     * 
     * @return a {@link Set} with the keys of the config's parameters.
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(prop.stringPropertyNames());
    }
    
    /**
     * Returns (as a string) the value corresponding to the variable name passed as argument.
     * @param key the name of the requested variable 
     * @return the string variable's value
     * @throws IllegalStateException if the requested property is not set
     */
    public String getString(String key) {
        return get(key);
    }

	/**
	 * Returns an array of string, splitted by comma (,).
	 * If the configuration contained an empty string, an array of 0 length is returned.
	 */
	public String[] getStringArray(String key) {
		String raw = getString(key);
		if (raw.equals("")) {
			return new String[0];
		} else {
			return raw.split(",");
		}
	}

    /**
     * Returns (as an int) the value corresponding to the variable name passed as argument.
     * @param key the name of the requested variable 
     * @return the int variable's value
     * @throws IllegalStateException if the requested property is not set
     * @throws IllegalArgumentException if the property is not a valid int number
     */
    public int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("getInt: Invalid value for property " +key+ "= " + value);
        }
    }

    /**
     * Returns an array of int, splitted by comma (,).
     * If the configuration contained an empty string, an array of 0 length is returned.
     */
    public int[] getIntArray(String key) {
        String[] strs= getStringArray(key);
        int[] res= new int[strs.length];
        for (int i=0; i< res.length; i++){
            try{
                res[i]= Integer.parseInt(strs[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("getIntArray: Invalid value for property " +key+ "= " + strs[i] + "(i="+ i +")");
            }
        }
        return res;
    }

    /**
     * Returns (as a long) the value corresponding to the variable name passed as argument.
     * @param key the name of the requested variable 
     * @return the long variable's value
     * @throws IllegalStateException if the requested property is not set
     * @throws IllegalArgumentException if the property is not a valid long number
     */
    public long getLong(String key) {
        String value = get(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("getLong: Invalid value for property " +key+ "= " + value);
        }
    }

    /**
     * Returns (as a float) the value corresponding to the variable name passed as argument.
     * @param key the name of the requested variable 
     * @return the float variable's value
     * @throws IllegalStateException if the requested property is not set
     * @throws IllegalArgumentException if the property is not a valid float number
     */
    public float getFloat(String key) {
        String value = get(key);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("getFloat: Invalid value for property " +key+ "= " + value);
        }
    }

    /**
     * Returns (as a boolean) the value corresponding to the variable name passed as argument.
     * @param key the name of the requested variable 
     * @return the boolean variable's value
     * @throws IllegalStateException if the requested property is not set
     * @throws IllegalArgumentException if the property is not one of [ true, yes, 1, false, no, 0]
     */
    public boolean getBoolean(String key) {
        String value = get(key);
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        } else {
            throw new IllegalArgumentException("getBoolean: Invalid value for property " +key+ "= " + value);
        }
    }



    /**
     * Returns a List&lt;Pair&lt;String,String&gt;&gt;. The property value has this format
     * first1:last1,first2:last2,..,firstN:lastN
     *
     * so every pair in the list will have firstI as first, and lastI as last.
     */
    public List<Pair<String,String>> getPairList(String key) {
        // Get strings, separated by ",", using other Config method.
        String[] raw = getStringArray(key);
        List<Pair<String,String>> list = new ArrayList<Pair<String,String>>(raw.length);

        // In case there were no pairs, this will be skipped.
        for (String strPair: raw) {
            // Check that there is no empty key - value pair
            if ("".equals(strPair)) {
                String error = "getPairList: found empty key - value pair. Check for ', ,' in your '" + key + "' definition.";
                logger.error(error);
                throw new IllegalArgumentException(error);
            }

            String[] parts = strPair.split(":");
            if (parts.length == 1 ) {
               logger.warn("getPairList: found token '" +parts[0] + "' without associated value.");
               list.add(new Pair<String,String>(parts[0],""));
            } else if (parts.length == 2 ) {
               list.add(new Pair<String,String>(parts[0],parts[1]));
            } else {
                String error = "getPairList: found token '" + strPair + "'  that is not a pair. ignoring it.";
                logger.error(error);
                throw new IllegalArgumentException(error);
            }
        }
        return list;
    }


    private char normalizedTimeUnit(String unit) {
        if ("".equals(unit) || "ms".equals(unit)) return 'l';
        if ("smhd".indexOf(unit.charAt(0)) >= 0) return unit.charAt(0);
    	throw new IllegalArgumentException();
    }
    
    private long timeUnitMillis(String unit) {
    	long v = 1;
    	switch (normalizedTimeUnit(unit)) {
    	case 'd': v *= 24;
    	case 'h': v *= 60;
    	case 'm': v *= 60;
    	case 's': v *= 1000;
    	}
    	return v;
    }
    
    /**
     * Returns the value corresponding to the specified variable name, converted to a specified time unit.
     * The time unit may be "ms", "s", "m", "h" or "d", or words starting with the letters s, m, h or d 
     * (corresponding to seconds, minutes, hours and days).
     * The variable may also specify a time unit. If it doesn't, it is assumed to be milliseconds.
     * @param key the name of the requested variable 
     * @return the variable's value converted to milliseconds
     * @throws IllegalStateException if the requested property is not set
     * @throws IllegalArgumentException if the value unit is not one of [ ms, s, m, h, d]
     */
    public long getTimePeriod(String key, String targetUnit) throws IllegalArgumentException {
    	String value = get(key);
    	value = value.replaceFirst("(\\D)"," $1");
        String[] parts = value.split("\\s+");
        String sourceUnit = "ms";
        if (parts.length > 1) {
        	sourceUnit = parts[1];
        }
        long time = 0;
        try {
            time = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("getTime: Invalid number in property " + key + " = " + value);
        }
        try {
        	time = time * timeUnitMillis(sourceUnit) / timeUnitMillis(targetUnit);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("getTime: Invalid time unit in property " + key + " = " + value);
        }
        return time;
    }




    /**
     * Verifies if a property is set.
     * @param key the name of the property
     * @return true if the property is defined, false otherwise
     * <br>
     * Note: It doesn't matter if the property is defined in the defaults file or in the customized file.
     */
    public synchronized boolean isDefined(String key) {
        return (null != prop.getProperty(key));
    }
    
    
    private String getPathFromClasspath(String resourceName, ClassLoader loader) {
    	URL url = loader.getResource(resourceName);
    	if (url != null) return url.getPath();
    	else return null;
    }

    
    private synchronized Properties readFromClasspath(String resourceName, ClassLoader loader) {

        InputStream is = loader.getResourceAsStream(resourceName);
        Properties p = null;
        if (null != is) {
            try {
            	p = readFromStream(is, loader);
            } catch (IOException e){
                logger.error("readFromClasspath: Couldn't read resource " + resourceName, e);
           }
        } else {
            logger.error("readFromClasspath: Couldn't read resource " + resourceName );
        }

        Execute.close(is);
        return p;
    }
    
    /**
     * Creates a config from a string. The string format is the same as a 
     * config/properties file.
     * @param cfg
     * @return
     */
    public static Config getConfigFromString(String cfg){        
        Config res= getEmptyConfig();
        ClassLoader loader= res.getClass().getClassLoader();
        InputStream is= new ByteArrayInputStream(cfg.getBytes());
        Properties p = null;
        try {
            p = res.readFromStream(is, loader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        res.prop=p;
        Execute.close(is);
        return res;     
    }
    
    private synchronized Properties readFromStream(InputStream is, ClassLoader loader) throws IOException{
        Properties p = new Properties();
		ExpandableInputStream eis = new ExpandableInputStream(is,loader); 
        p.load(eis);
        return p;
    }
    

    // override toString so it is easier to parse.
    // Properties.toString is a comma-separated on liner, and it is not
    // so easy to parse if keys or values have commas.
    public String toString() {
        String properties = null;
        try {
            OutputStream os = new ByteArrayOutputStream();
            this.prop.store(os,this.filename);
            properties = os.toString(); 
            os.close();
        } catch (IOException e) {
           properties = prop.toString();
        }

        return properties;
    }


    public static void main(String[] args){
    
        String help = "Lists a config file.\nUsage:\tConfig <file> [<property>]";

        if (args.length !=1 && args.length != 2) {
            System.out.println(help);
            System.exit(-1);
        }

        // so parameter count is right
        try {
            Config config = Config.getConfig(args[0]);
            if (args.length == 1) {
                System.out.println(config);
            } else { // args.length == 2
                System.out.println(config.get(args[1]));
            }
        } catch (IllegalStateException e) {
            System.out.println(args[0]);
            System.out.println(e);
            System.exit(-1);
        }

    }

    public String getFilename() {
        return filename;
    }
    
    public Config prefix(String prefix) {
        return new PrefixedConfig(this, prefix);
    }
    
    public Map<String, String> asMap() {
        return Maps.fromProperties(prop);
    }

}

