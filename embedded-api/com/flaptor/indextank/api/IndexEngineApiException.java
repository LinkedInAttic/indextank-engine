package com.flaptor.indextank.api;

public class IndexEngineApiException extends Exception {

    public IndexEngineApiException(String message) {
        super(message);
    }

    public IndexEngineApiException(Throwable cause) {
        super(cause);
    }

    public IndexEngineApiException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}
