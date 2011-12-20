package com.flaptor.util.remote;

/**
 * connection succesful but couldn't find the remote method
 * @author Martin Massera 
 *
 */
public class NoSuchRpcMethodException extends RpcException {
    
    private static final long serialVersionUID = 1L;

    public NoSuchRpcMethodException(String method) {
        super("Could not find remote method " + method);
    }
    
    public NoSuchRpcMethodException(String method, Throwable cause) {
        super("Could not find remote method " + method, cause);
    }
}
