package com.flaptor.indextank.api.util;

public class Timestamp {

    public static int inSeconds() {
        return (int)(System.currentTimeMillis() / 1000);
    }

}
