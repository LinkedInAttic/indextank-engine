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

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;


public class ScoreMathTest {

    private static boolean PRINT = false;

    // old km distance formula
    private double km(double x1, double y1, double x2, double y2) {
        return 111.133 * Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    // Great Circle formula: http://en.wikipedia.org/wiki/Great-circle_distance
    private double kmgreatc(double x1, double y1, double x2, double y2) {
        x1 = Math.toRadians(x1);
        y1 = Math.toRadians(y1);
        x2 = Math.toRadians(x2);
        y2 = Math.toRadians(y2);

        return 6372.8 * Math.acos(Math.sin(x1)*Math.sin(x2)+Math.cos(x1)*Math.cos(x2)*Math.cos(Math.abs(y1 - y2)));
    }
    // Great Circle formula: http://en.wikipedia.org/wiki/Great-circle_distance
    private double milesgreatc(double x1, double y1, double x2, double y2) {
        x1 = Math.toRadians(x1);
        y1 = Math.toRadians(y1);
        x2 = Math.toRadians(x2);
        y2 = Math.toRadians(y2);

        return 3959.87433 * Math.acos(Math.sin(x1)*Math.sin(x2)+Math.cos(x1)*Math.cos(x2)*Math.cos(Math.abs(y1 - y2)));
    }


    // old miles distance formula
    private double miles(double x1, double y1, double x2, double y2) {
        return 69.055 * Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    //    @Test
    public void testTimeKm() {
        Random r = new Random();
        double x1 = r.nextInt(89) + r.nextDouble();
        double y1 = r.nextInt(180) + r.nextDouble();
        double x2 = r.nextInt(89) + r.nextDouble();
        double y2 = r.nextInt(180) + r.nextDouble();

        long oldTime = 0;
        long newTime = 0;

        //(just in case of compiler and caches optimizations)
        for(long i = 0; i < 100; i++) {
            km(x1, y1, x2, y2);
            ScoreMath.km(x1, y1, x2, y2);
        }

        for(int j = 0; j < 12; j++) {
            long times = 100000000L;

            // take care whether you measure new function and then old function or vice, because times changes 

            /* TIME for new KM function*/
            long t2 = System.currentTimeMillis();
            for (long i = 0; i < times; i++) {
                ScoreMath.km(x1, y1, x2, y2);
            }
            long t3 = System.currentTimeMillis();
            /* TIME for new KM function*/

            /* TIME for old KM function*/
            long t0 = System.currentTimeMillis();
            for (long i = 0; i < times; i++) {
                km(x1, y1, x2, y2);
            }
            long t1 = System.currentTimeMillis();
            /* TIME for old KM function*/

            double expected = t1 - t0;
            oldTime += expected;

            double actual = t3 - t2;
            newTime += actual;

            System.out.printf("KM: Old formula %s, New formula %s\n", expected, actual);
            Assert.assertTrue("New km formula is too slow", (double)actual / (double)expected < 10d);
        }
        if (PRINT)
            System.out.printf("KM TIME: OldTime: %6d --- NewTime: %6d --- %3.2f times slower\n", oldTime, newTime, (double)newTime/(double)oldTime);
    }

    //    @Test
    public void testTimeMiles() {
        Random r = new Random();
        double x1 = r.nextInt(89) + r.nextDouble();
        double y1 = r.nextInt(180) + r.nextDouble();
        double x2 = r.nextInt(89) + r.nextDouble();
        double y2 = r.nextInt(180) + r.nextDouble();

        //(just in case of compiler and caches optimizations)
        for(long i = 0; i < 100; i++) {
            miles(x1, y1, x2, y2);
            ScoreMath.miles(x1, y1, x2, y2);
        }

        long oldTime = 0;
        long newTime = 0;

        for(int j = 0; j < 10; j++) {
            long times = 100000000L;

            // take care whether you measure new function and then old function or vice, because times changes 

            /* TIME for new MILES function*/
            long t2 = System.currentTimeMillis();
            for (long i = 0; i < times; i++) {
                ScoreMath.miles(x1, y1, x2, y2);
            }
            long t3 = System.currentTimeMillis();
            /* TIME for new MILES function*/

            /* TIME for old MILES function*/
            long t0 = System.currentTimeMillis();
            for (long i = 0; i < times; i++) {
                miles(x1, y1, x2, y2);
            }
            long t1 = System.currentTimeMillis();
            /* TIME for old MILES function*/

            double expected = t1 - t0;
            oldTime += expected;

            double actual = t3 - t2;
            newTime += actual;

            System.out.printf("MILES: Old formula %s, New formula %s\n", expected, actual);
            Assert.assertTrue("New miles formula is too slow", (double)actual / (double)expected < 1000d);
        }

        if (PRINT)
            System.out.printf("MILES TIME: OldTime: %6d --- NewTime: %6d --- %3.2f times slower\n", oldTime, newTime, (double)newTime/(double)oldTime);
    }

    @Test
    public void testPrecisionKm() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            double x1 = (r.nextBoolean()? r.nextInt(89) : -r.nextInt(90)) + r.nextDouble();
            double y1 = r.nextInt(180) + r.nextDouble();
            double x2 = (r.nextBoolean()? r.nextInt(89) : -r.nextInt(90)) + r.nextDouble();
            double y2 = r.nextInt(180) + r.nextDouble();

            double kmold = km(x1, y1, x2, y2);
            double kmnew = ScoreMath.km(x1, y1, x2, y2);
            double kmgreatc = kmgreatc(x1, y1, x2, y2);

            //    		System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(kmnew));

            if (Math.abs(kmgreatc - kmnew) <= Math.abs(kmgreatc - kmold)) {
                betterThanOld++;
            }

            if (Math.abs((kmgreatc - kmnew)/kmgreatc) < 0.15) {
                goodEnough++;
            }
        }
        if (PRINT)
            System.out.printf("KM RANDOM: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                    (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.9);
        Assert.assertTrue((double)goodEnough/(double)times >= 0.85);
    }

    @Test
    public void testPrecisionMiles() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            double x1 = (r.nextBoolean()? r.nextInt(89) : -r.nextInt(90)) + r.nextDouble();
            double y1 = r.nextInt(180) + r.nextDouble();
            double x2 = (r.nextBoolean()? r.nextInt(89) : -r.nextInt(90)) + r.nextDouble();
            double y2 = r.nextInt(180) + r.nextDouble();

            double milesold = miles(x1, y1, x2, y2);
            double milesnew = ScoreMath.miles(x1, y1, x2, y2);
            double milesgreatc = milesgreatc(x1, y1, x2, y2);

            //    		System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(milesnew));

            if (Math.abs(milesgreatc - milesnew) <= Math.abs(milesgreatc - milesold)) {
                betterThanOld++;
            }

            if (Math.abs((milesgreatc - milesnew)/milesgreatc) < 0.15) {
                goodEnough++;
            }
        }

        if (PRINT)
            System.out.printf("MILES RANDOM: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                    (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.9);
        Assert.assertTrue("New formula is not good enough", (double)goodEnough/(double)times >= 0.85);
    }

    @Test
    public void testPrecisionKmAroundNewYork() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            double x1 = 40.6d + r.nextDouble();
            double y1 = -73.80 - r.nextDouble();
            double x2 = 40.78d - r.nextDouble();
            double y2 = -74.05 + r.nextDouble();

            double kmold = km(x1, y1, x2, y2);
            double kmnew = ScoreMath.km(x1, y1, x2, y2);
            double kmgreatc = kmgreatc(x1, y1, x2, y2);

            //    		System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(kmnew));

            if (Math.abs(kmgreatc - kmnew) <= Math.abs(kmgreatc - kmold)) {
                betterThanOld++;
            }

            if (Math.abs((kmgreatc - kmnew)/kmgreatc) < 0.05) {
                goodEnough++;
            }
        }

        if (PRINT)
            System.out.printf("KM NEW_YORK: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                    (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.95);
        Assert.assertTrue((double)goodEnough/(double)times >= 0.95);
    }

    @Test
    public void testPrecisionMilesAroundNewYork() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            double x1 = 40.6d + r.nextDouble();
            double y1 = -73.80 - r.nextDouble();
            double x2 = 40.78d - r.nextDouble();
            double y2 = -74.05 + r.nextDouble();

            double milesold = miles(x1, y1, x2, y2);
            double milesnew = ScoreMath.miles(x1, y1, x2, y2);
            double milesgreatc = milesgreatc(x1, y1, x2, y2);

            //    		System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(milesnew));

            if (Math.abs(milesgreatc - milesnew) <= Math.abs(milesgreatc - milesold)) {
                betterThanOld++;
            }

            if (Math.abs((milesgreatc - milesnew)/milesgreatc) < 0.05) {
                goodEnough++;
            }
        }if (PRINT)

            if (PRINT)
                System.out.printf("MILES NEW_YORK: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                        (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.95);
        Assert.assertTrue((double)goodEnough/(double)times >= 0.95);
    }

    @Test
    public void testPrecisionKmShortDistance() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            int latitude = r.nextInt(90);
            int longitude = r.nextInt(180);
            double x1 = latitude + r.nextDouble();
            double y1 = longitude  - r.nextDouble();
            double x2 = latitude - r.nextDouble();
            double y2 = longitude  + r.nextDouble();

            double kmold = km(x1, y1, x2, y2);
            double kmnew = ScoreMath.km(x1, y1, x2, y2);
            double kmgreatc = kmgreatc(x1, y1, x2, y2);

            //    		System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(kmnew));

            if (Math.abs(kmgreatc - kmnew) <= Math.abs(kmgreatc - kmold)) {
                betterThanOld++;
            }

            if (Math.abs((kmgreatc - kmnew)/kmgreatc) < 0.05) {
                goodEnough++;
            }
        }

        if (PRINT)
            System.out.printf("KM NEAR: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                    (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.90);
        Assert.assertTrue((double)goodEnough/(double)times >= 0.95);
    }

    @Test
    public void testPrecisionMilesShortDistance() {

        long times = 100000L;
        int goodEnough= 0;
        int betterThanOld= 0;

        for(int j = 0; j < times; j++) {
            Random r = new Random();
            int latitude = r.nextInt(90);
            int longitude = r.nextInt(180);
            double x1 = latitude + r.nextDouble();
            double y1 = longitude  - r.nextDouble();
            double x2 = latitude - r.nextDouble();
            double y2 = longitude  + r.nextDouble();

            double milesold = miles(x1, y1, x2, y2);
            double milesnew = ScoreMath.miles(x1, y1, x2, y2);
            double milesgreatc = milesgreatc(x1, y1, x2, y2);

            // System.out.printf("%10.2f %10.2f %10.2f\n", kmgreatc, kmnew, kmold);
            Assert.assertTrue("Distance cant be NaN", !Double.isNaN(milesnew));

            if (Math.abs(milesgreatc - milesnew) <= Math.abs(milesgreatc - milesold)) {
                betterThanOld++;
            }

            if (Math.abs((milesgreatc - milesnew)/milesgreatc) < 0.05) {
                goodEnough++;
            }
        }

        if (PRINT)
            System.out.printf("MILES NEAR: BetterThanOld: %6.4f --- GoodEnough: %6.4f\n", 
                    (double)betterThanOld / (double)times, (double)goodEnough / (double)times);

        Assert.assertTrue((times - betterThanOld) + " times old formula was better", 
                (double)betterThanOld/(double)times >= 0.90);
        Assert.assertTrue((double)goodEnough/(double)times >= 0.95);
    }

    @Test
    public void testBorderCases() {
        double x = ScoreMath.km(90, 180, -90, -180);
        Assert.assertTrue(x > 0 && x < Double.POSITIVE_INFINITY);

        double y = ScoreMath.km(90d, 0, 90d, 0);
        Assert.assertEquals(0d, y);
    }
}
