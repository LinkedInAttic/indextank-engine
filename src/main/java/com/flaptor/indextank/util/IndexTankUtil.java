package com.flaptor.indextank.util;

import java.io.File;

public class IndexTankUtil {

    public static boolean isMaster() {
        return new File("/data/master").exists();
    }
    
}
