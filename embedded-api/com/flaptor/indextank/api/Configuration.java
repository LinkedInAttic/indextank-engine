package com.flaptor.indextank.api;

public class Configuration {
    
    public static String host = "localhost";
    public static Integer port = 20220;
    public static Integer indexerPort = port + 1;
    public static Integer searcherPort = port + 2;
    public static Integer api = 8080;
    public static String indexCode = "dbajo";
    public static EmbeddedIndexEngine engine;

}
