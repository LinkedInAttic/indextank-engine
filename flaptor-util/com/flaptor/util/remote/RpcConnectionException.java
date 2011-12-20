package com.flaptor.util.remote;

/**
 * Exception for signaling that the connection failed while doing RPC
 * @author Martin Massera
 */
public class RpcConnectionException extends RpcException {

    private static final long serialVersionUID = 1L;

    public RpcConnectionException() {
        super("Connection Exception");
    }

    public RpcConnectionException(Throwable cause) {
        super("Connection Exception", cause);
    }

    public RpcConnectionException(String message) {
        super(message);
    }
}
