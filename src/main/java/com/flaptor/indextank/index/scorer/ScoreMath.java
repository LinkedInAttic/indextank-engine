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

/**
 * Static math function for use in the user-defined scoring functions.
 */
public class ScoreMath {

    public static double neg(double val) {
        return -val;
    }

    public static double min(double val1, double val2) {
        return (val1 < val2) ? val1 : val2;
    }

    public static double max(double val1, double val2) {
        return (val1 > val2) ? val1 : val2;
    }

    public static double abs(double val) {
        return val<0 ? -val : val;
    }

    public static double log(double val) {
        // return Math.getExponent(val);
        if (val > 0) {
            return (double)Math.log(val);
        } else if (val == 0) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return Double.NaN;
        }
    }

    public static double sqrt(double val) {
        if (val >= 0) {
            return Math.sqrt(val);
        } else {
            return Double.NaN;
        }
    }

    public static double pow(double val, double exp) {
        int expi = (int)exp;
        if (expi < 0) { 
            val = 1f / val; 
            expi = -expi; 
        } 
        double res = 1f;
        while (expi > 0) { 
            if ((expi & 1) == 1) {
                res *= val;
            }
            val *= val; 
            expi >>= 1; 
        } 
        return res; 
    }

    private static final int C_SIZE = 181;
    // C contains a correction factor between 0 and 1 to 
    private static final double C[] = new double [C_SIZE];

    static {
        for(int i = 0; i < C_SIZE; i++) {
            C[i] = Math.cos(Math.toRadians(i-90));
            C[i] *= C[i];
        }
    }

    public static double km(double x1, double y1, double x2, double y2) {
        if (Math.abs(x1) > 90 || Math.abs(x2) > 90 || Math.abs(y1) > 180 || Math.abs(y2) > 180) {
            return Double.POSITIVE_INFINITY;
        } else {
            return 111.133 * Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)*C[(90+((int)(x1+x2)/2))]); 
        }
    }

    public static double miles(double x1, double y1, double x2, double y2) {
        if (Math.abs(x1) > 90 || Math.abs(x2) > 90 || Math.abs(y1) > 180 || Math.abs(y2) > 180){
            return Double.POSITIVE_INFINITY;
        } else {
            return 69.055 * Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)*C[(90+((int)(x1+x2)/2))]);
        }
    }

    public static double bit(double val1, double val2) {
        return (((int)val1 & (1 << (int)val2)) > 0) ? 1 : 0;
    }

}
