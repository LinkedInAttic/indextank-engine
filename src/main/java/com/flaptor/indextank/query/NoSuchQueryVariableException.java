package com.flaptor.indextank.query;

public class NoSuchQueryVariableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer missingVariableIndex;
    
    public NoSuchQueryVariableException(String msg, Integer missingVariableIndex) {
        super(msg);
        this.missingVariableIndex = missingVariableIndex;
    }

    public Integer getMissingVariableIndex() {
        return this.missingVariableIndex;
    }
}
