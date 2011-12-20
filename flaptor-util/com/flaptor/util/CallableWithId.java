package com.flaptor.util;

import java.util.concurrent.Callable;

/**
 * A callable with id, to be able to track it, useful in multiexecutor for example
 *  
 * @author Martin Massera
 */
abstract public class CallableWithId<T, IdType> implements Callable<T>{
    
    private IdType id;

    public CallableWithId(IdType id) {
        super();
        this.id = id;
    }
    public IdType getId() {
        return id;
    }
}
