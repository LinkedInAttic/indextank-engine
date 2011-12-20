package com.flaptor.indextank.query;


public abstract class AToken {
    public abstract String getText();
    public abstract int getPosition();
    public abstract int getStartOffset();
    public abstract int getEndOffset();
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer("token(text: ");
        buff.append(getText())
            .append(", position: ")
            .append(getPosition())
            .append(", startOffset: ")
            .append(getStartOffset())
            .append(", endOffset: ")
            .append(getEndOffset());
        return buff.toString();
    }
}
