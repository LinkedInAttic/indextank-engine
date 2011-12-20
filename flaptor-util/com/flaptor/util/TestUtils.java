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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;


public class TestUtils {

    private static Random rnd = new Random(System.currentTimeMillis());

    /**
     * Write an object to a file.
     * @param dirname the directory of the file.
     * @param filename the name of the file.
     * @param obj the object to be stored.
     */
    public static void writeObjectToFile(String dirname, String filename, Object obj) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dirname,filename))));
            oos.writeObject(obj);
            oos.flush();
        } catch (IOException e) {
            System.err.println("write: " +e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
    }

    /**
     * Write a text file.
     * @param filename the name of the file, can include path.
     * @param the text to be written to the file (lines separated by '#').
     */
    public static void writeFile (String filename, String text) throws IOException {
        File file = new File(filename);
        if (null != file.getParentFile()) {
            file.getParentFile().mkdirs();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        String[] lines = text.split("#");
        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    /**
     * Creates a temporary file with content.
     * The string will be converted to a byte array using the platform's
     * default encoding. 
     * @param content a string to store in the file.
     */
    public static File createTempFileWithContent(String content) throws IOException {
        File file = null;
        FileOutputStream fo = null;
        try {
            file = File.createTempFile("tmpFile-", ".tmp");
            fo = new FileOutputStream(file);
            fo.write(content.getBytes());
        } finally {
            Execute.close(fo);
        }
        return file;
    }


    private static Config testConfig = null;

    /**
     * Convenience method for creating a test config.
     * @param dirName the name of the config file.
     * @param contents the contents of the config file.
     */
    public static void setConfig(String configName, String contents) throws IOException {
        writeFile(configName, contents);
        if (configName.charAt(0) == '/') {
            URLClassLoader loader = new URLClassLoader(new URL[] {new File(configName).toURI().toURL()});
            testConfig = Config.getConfig(configName,loader);
        } else {
            testConfig = Config.getConfig(configName);
        }
    }

    /**
     * Convenience method for creating a test config.
     * It assumes the config has been set using setConfig().
     * @return the Config instance previously set with setConfig().
     */
    public static Config getConfig() throws Exception {
        if (null == testConfig) { 
            throw new Exception("getConfig was called before calling setConfig");
        }
        return testConfig;
    }


    /**
     * Generate a random text.
     * @param minWords the minimum number of words in the text.
     * @param maxWords the maximum number of words in the text.
     * @return a text containing between [minWords] and [maxWords] random words.
     */
    public static String randomText (int minWords, int maxWords) {
        StringBuffer buf = new StringBuffer();
        int words = minWords + rnd.nextInt(maxWords-minWords+1);
        for (int word=0; word<words; word++) {
            if (word != 0) {
                buf.append(' ');
            }
            int len = 1+rnd.nextInt(8);
            for (int p=0; p<len; p++) {
                char c = (char)('a' + rnd.nextInt('z'-'a'));
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * @return a random url, in the form http(s)://somerandomword(.somerandomword)*((/somerandomword)+(.somerandomword))
     */
    public static String randomUrl() {
        StringBuffer b = new StringBuffer();
        b.append("http");
        if (rnd.nextBoolean()) b.append("s");
        b.append("//");
        for (int i = 0; i < rnd.nextInt(3) + 1; ++i) {
            b.append(randomText(1, 1));
        }
        if (rnd.nextBoolean()) {
            b.append(":");
            b.append(rnd.nextInt(65535) + 1);
        }
        int numSlashes = rnd.nextInt(5);
        for (int i = 0; i < numSlashes; ++i) {
            b.append('/');
            b.append(TestUtils.randomText(1, 1));
        }
        if (numSlashes > 0 && rnd.nextBoolean()) {
            b.append(".");
            b.append(TestUtils.randomText(1, 1));
        }
        return b.toString();
    }
}

