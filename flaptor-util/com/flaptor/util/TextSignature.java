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

import com.flaptor.util.compression.CompressedIntSequence;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class TextSignature implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HASH_SIZE = 256;
    protected CompressedIntSequence components = null;

    // Adds a word to the hash.
    private int addWord (int hash, String word) {
        int code = word.hashCode();
        code = (code < 0) ? -code : code;
        hash ^= code;
        return hash;
    }

    // Removes a word from the hash. This is simply a call to addWord,
    // because it uses xor, which is reversed by reapplying.
    private int removeWord (int hash, String word) {
        return addWord(hash, word);
    }

    /**
     * Creates a signature of the provided text.
     * @param text the text from which to compute the signature.
     */
    public TextSignature (String text) {
        try { 
            Reader reader = new StringReader(text);
            build(reader);
        } catch (IOException ioe) {
            // this should NEVER happen, as the 
            // String is already loaded into the heap.
        }
    } 

    
    /**
     * Creates a signature of the provided text.
     * @param reader the reader from which to read the text to compute the signature.
     */
    public TextSignature (Reader reader) throws IOException {
        build(reader);
    }

    /**
     * Creates a signature of the provided text.
     * @param is the InputStream from which to read the text to compute the signature.
     */
    public TextSignature (InputStream is) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(is));
        build(reader);
    }


    private void build(Reader reader) throws IOException {
        int[] data = new int[HASH_SIZE];
        
        Scanner scanner = new Scanner(reader).useDelimiter("\\W+");
        List<String> tokens = new LinkedList<String>();

        int windowSize = 2;
        int tail = 1-windowSize;
        int hash = 0;
        boolean hashing = false;


        while (scanner.hasNext()) {
            String token = scanner.next();
            tokens.add(token);
            hash = addWord(hash, token);
            if (tail == 0) {
                hashing = true;
            }
            if (hashing) {
                data[hash%HASH_SIZE]++;
                hash = removeWord(hash, tokens.remove(0));
            }
            tail++;
        }

        // text shorter than window? hash it anyway.
        if (!hashing){
            data[hash%HASH_SIZE]++;
        }

        //if (!hashing) System.err.println("TextSignature: still not hashing. Components will be empty.");
        components = new CompressedIntSequence(data);

    }



    /**
     * Compares this signature with the specified signature for similarity. 
     * Returns a number in the 0..1 range that measures the similarity of this signature to another signature.
     * @param otherSig the other signature.
     * @return A float in the 0..1 range indicating the similarity to the provided signature. 0 means different, 1 means equal.
     */
    public float compareTo (TextSignature other) {
        int union_size = 0;
        int intersection_size = 0;
        int[] data1 = components.getUncompressed();
        int[] data2 = other.components.getUncompressed();
        for (int i=0; i<HASH_SIZE; i++) {
            union_size += Math.max(data1[i], data2[i]);
            intersection_size += Math.min(data1[i], data2[i]);
        }

        // check if both were empty. union will be 0 in that case.
        if (0 == union_size) return 0f;

        // so, someone was not empty .. comparison means something.
        return (float)intersection_size / union_size;
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TextSignature)) { return false; }
        return components.equals(((TextSignature)other).components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public String toString() {
        return components.toString();
    }

    // For testing purposes
    public static void main (String[] args) throws Exception {
/*
        try {
            File file1 = new File(args[0]);
            File file2 = new File(args[1]);
            String string1 = FileUtil.readFile(file1);
            String string2 = FileUtil.readFile(file2);
            TextSignature t1 = new TextSignature(string1);
            TextSignature t2 = new TextSignature(string2);
            System.out.println("Similarity: " + t1.compareTo(t2));
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
*/

        File dir = FileUtil.createTempDir("","");
        File file = new File(dir,"signature.tst");
        long max = 100000;
        for (int i=0; i<max; i++) {
            String text = TestUtils.randomText(10,500);
//            System.out.println("=================================================================");
//            System.out.println(text);
//            System.out.println("-----------------------------------------------------------------");
   
            TextSignature sig1 = new TextSignature(text);
            int[] data = sig1.components.getUncompressed();

            ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file, false), 16000));
            outputStream.writeObject(sig1);
            outputStream.close();

            ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), 16000));
            TextSignature sig2 = (TextSignature)inputStream.readObject();
  
            if (!sig1.equals(sig2)) {
                System.out.println("ERROR! Sig serialization self-mismatch");
                System.exit(-1);
            }
            
            if (sig1.compareTo(sig2) != 1.0f) {
                System.out.println("ERROR! Sig serialization self-mismatch");
                System.exit(-1);
            }

//            System.out.println("SIG ("+data.length+" bytes): "+sig1.toString());
        }
        System.out.println("Ok!");
        FileUtil.deleteDir(dir);
    }

}

