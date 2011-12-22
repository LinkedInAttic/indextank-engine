package com.flaptor.indextank.api.util;

public class QueryHelper {

    public static int parseIntParam(String param, int defaultValue) {
        try {
            if(param != null && !param.isEmpty())
                return Integer.parseInt(param);
        } catch(Exception e) {
        }
        return defaultValue;
    }

}
