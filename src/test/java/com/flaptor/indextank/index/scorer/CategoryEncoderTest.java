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


package com.flaptor.indextank.index.scorer;

import static com.flaptor.util.TestInfo.TestType.UNIT;

import com.flaptor.indextank.IndexTankTestCase;
import com.flaptor.util.TestInfo;

public class CategoryEncoderTest extends IndexTankTestCase {

    @TestInfo(testType=UNIT)
    public void testConsistentCoding() {
        int[] data = new int[2];
        int[] bitmask = new int[2];
        bitmask[0] = 0x3010;
        bitmask[1] = 0x02; //just 4 bits
        int value = 0;
        while (value < 16) {
            //System.out.print("bitmask:\t\t"); print(bitmask);
            //System.out.print("data(before):\t"); print(data);
            data = CategoryEncoder.encode(data, 0, bitmask, value);
            //System.out.print("data(after):\t"); print(data);
            int decodedValue = CategoryEncoder.decode(data, 0, bitmask);
            //System.out.println("value: " + value);
            //System.out.println("decoded value: " + decodedValue);
            assertEquals(value, decodedValue);
            value++;
        }
        try {
            data = CategoryEncoder.encode(data, 0, bitmask, value);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
            //This is ok.
            }
    }

    @TestInfo(testType=UNIT)
    public void testCrossCoding() {
        int[] data = new int[2];
        int[] bitmask1 = new int[2];
        int[] bitmask2= new int[2];
        bitmask1[0] = 0x3010;
        bitmask1[1] = 0x0002; //just 4 bits
        bitmask2[0] = 0x4020;
        bitmask2[1] = 0x1001; //different 4 bits
        int value1 = 12;
        int value2 = 13;
        //System.out.print("data(before):\t"); print(data);
        //System.out.print("bitmask1:\t\t"); print(bitmask1);
        data = CategoryEncoder.encode(data, 0, bitmask1, value1);
        //System.out.print("data(after1):\t"); print(data);
        data = CategoryEncoder.encode(data, 0, bitmask2, value2);
        //System.out.print("bitmask2:\t\t"); print(bitmask2);
        //System.out.print("data(after2):\t"); print(data);
        int decodedValue1 = CategoryEncoder.decode(data, 0, bitmask1);
        int decodedValue2 = CategoryEncoder.decode(data, 0, bitmask2);
        assertEquals(value1, decodedValue1);
        assertEquals(value2, decodedValue2);
    }

    @TestInfo(testType=UNIT)
    public void testCrossCodingDontChangeOtherDataLong() {
        int[] data = new int[4];
        int payload = 0x73ea3333;
        data[0] = payload;
        int[] bitmask1 = new int[2];
        int[] bitmask2= new int[2];
        bitmask1[0] = 0x3010;
        bitmask1[1] = 0x0002; //just 4 bits
        bitmask2[0] = 0x4020;
        bitmask2[1] = 0x1001; //different 4 bits
        int value1 = 12;
        int value2 = 13;
        //System.out.print("data(before):\t"); print(data);
        //System.out.print("bitmask1:\t\t"); print(bitmask1);
        data = CategoryEncoder.encode(data, 1, bitmask1, value1);
        //System.out.print("data(after1):\t"); print(data);
        data = CategoryEncoder.encode(data, 1, bitmask2, value2);
        //System.out.print("bitmask2:\t\t"); print(bitmask2);
        //System.out.print("data(after2):\t"); print(data);
        int decodedValue1 = CategoryEncoder.decode(data, 1, bitmask1);
        int decodedValue2 = CategoryEncoder.decode(data, 1, bitmask2);
        assertEquals(value1, decodedValue1);
        assertEquals(value2, decodedValue2);
        assertEquals(payload, data[0]);
    }

    @TestInfo(testType=UNIT)
    public void testCrossCodingDontChangeOtherDataShort() {
        int[] data = new int[1];
        int payload = 0x73ea3333;
        data[0] = payload;
        int[] bitmask1 = new int[2];
        int[] bitmask2= new int[2];
        bitmask1[0] = 0x3010;
        bitmask1[1] = 0x0002; //just 4 bits
        bitmask2[0] = 0x4020;
        bitmask2[1] = 0x1001; //different 4 bits
        int value1 = 12;
        int value2 = 13;
        //System.out.print("data(before):\t"); print(data);
        //System.out.print("bitmask1:\t\t"); print(bitmask1);
        data = CategoryEncoder.encode(data, 1, bitmask1, value1);
        //System.out.print("data(after1):\t"); print(data);
        data = CategoryEncoder.encode(data, 1, bitmask2, value2);
        //System.out.print("bitmask2:\t\t"); print(bitmask2);
        //System.out.print("data(after2):\t"); print(data);
        int decodedValue1 = CategoryEncoder.decode(data, 1, bitmask1);
        int decodedValue2 = CategoryEncoder.decode(data, 1, bitmask2);
        assertEquals(value1, decodedValue1);
        assertEquals(value2, decodedValue2);
        assertEquals(payload, data[0]);
    }

    @TestInfo(testType=UNIT)
    public void testGrowInvariance() {
        int[] data = new int[1];
        int[] bitmask1s = new int[1];
        int[] bitmask1l= new int[2];
        int[] bitmask2l= new int[2];
        bitmask1s[0] = 0x0011;
        bitmask1l[0] = bitmask1s[0];
        bitmask2l[0] = 0x1100;
        bitmask2l[1] = 0x1001;
        int value1 = 2;
        int value2 = 5;
        //System.out.print("data(before):\t"); print(data);
        //System.out.print("bitmask1s:\t\t"); print(bitmask1s);
        data = CategoryEncoder.encode(data, 0, bitmask1s, value1);
        //System.out.print("data(after1):\t"); print(data);
        data = CategoryEncoder.encode(data, 0, bitmask2l, value2);
        //System.out.print("bitmask2l:\t\t"); print(bitmask2l);
        //System.out.print("data(after2):\t"); print(data);
        int decodedValue1s = CategoryEncoder.decode(data, 0, bitmask1s);
        int decodedValue1l = CategoryEncoder.decode(data, 0, bitmask1l);
        int decodedValue2l = CategoryEncoder.decode(data, 0, bitmask2l);
        assertEquals(value1, decodedValue1s);
        assertEquals(value1, decodedValue1l);
        assertEquals(value2, decodedValue2l);
    }

    /*private void print(int[] d) {
        for (int i = d.length-1; i >=0; i--) {
            for (int j = 31; j >= 0 ; j--) {
                if ((d[i] & (1<<j)) != 0) {
                    System.out.print('1');
                } else {
                    System.out.print('0');
                }
            }
            System.out.print('|');
        }
        System.out.print('\n');
    }*/


}
