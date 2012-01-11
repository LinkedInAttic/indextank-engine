/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package com.flaptor.indextank.suggest;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import com.flaptor.util.FileUtil;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class PopularityIndexBenchmark {

    static String[] words;
    static Multiset<String> input;
    
    static void load(String file) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        input = HashMultiset.create();
        while (dis.available() > 0) {
            String str = dis.readUTF();
            int c = dis.readInt();
            input.add(str, c);
        }
        words = input.elementSet().toArray(new String[0]);
    }
    
    public static void main(String[] args) throws IOException {
        load(args[0]);
        System.out.println(input.size());
        System.out.println(input.elementSet().size());
        //testLinear1by1(300000);
        //btestLinear1by1(300000);
        //NewPopularityIndex npi = testRandom(1000000);
        //testQueries(npi, 1000000);
        PopularityIndex pi = btestRandom(1000000);
        btestQueries(pi, 1000000);
    }

    private static void testLinear1by1(int max) throws IOException {
        File f = FileUtil.createTempDir("PopularityIndexTest", ".tmp");
        NewPopularityIndex npi = new NewPopularityIndex(f);
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        for (String string : input) {
            npi.addTerm(string);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
            if (c == max) break;
        }
    }
    private static NewPopularityIndex testRandom(int max) throws IOException {
        File f = FileUtil.createTempDir("PopularityIndexTest", ".tmp");
        NewPopularityIndex npi = new NewPopularityIndex(f);
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        Random r = new Random(5);
        for (int i = 0; i < input.size(); i++) {
            int k = r.nextInt(words.length);
            npi.addTerm(words[k]);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
            if (c == max) break;
        }
        return npi;
    }
    private static void testQueries(NewPopularityIndex npi, int max) throws IOException {
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        Random r = new Random(5);
        for (int i = 0; i < max; i++) {
            int k = r.nextInt(words.length);
            //int l = r.nextInt(words[k].length());
            npi.getMostPopular(words[k]);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
        }
    }
    private static void btestLinear1by1(int max) throws IOException {
        File f = FileUtil.createTempDir("PopularityIndexTest", ".tmp");
        PopularityIndex npi = new PopularityIndex(f, false);
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        for (String string : input) {
            npi.add(string);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
            if (c == max) break;
        }
    }
    private static PopularityIndex btestRandom(int max) throws IOException {
        File f = FileUtil.createTempDir("PopularityIndexTest", ".tmp");
        PopularityIndex npi = new PopularityIndex(f, false);
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        Random r = new Random(5);
        for (int i = 0; i < input.size(); i++) {
            int k = r.nextInt(words.length);
            npi.add(words[k]);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
            if (c == max) break;
        }
        return npi;
    }
    private static void btestQueries(PopularityIndex npi, int max) throws IOException {
        long t = System.currentTimeMillis();
        long t1 = t;
        int c = 0;
        Random r = new Random(5);
        for (int i = 0; i < max; i++) {
            int k = r.nextInt(words.length);
            //int l = r.nextInt(words[k].length());
            npi.getMostPopular(words[k]);
            if (++c % 100000 == 0) { 
                long t2 = System.currentTimeMillis();
                long d = t2 - t;
                long d1 = t2 - t1;
                System.out.format("%d so far: %.3f ms avg (%.3f avg last 100k)\n", c, 1.0*d/c, 1.0*d1/c);
            }
        }
    }
    
}
