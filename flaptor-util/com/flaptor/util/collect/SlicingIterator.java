/**
 * 
 */
package com.flaptor.util.collect;

import java.util.Iterator;

class SlicingIterator<E> implements Iterator<E> {
    private Iterator<? extends E> delegate;
    private int current;
    private int end;
    public SlicingIterator(Iterable<? extends E> it, int start, int end) {
        this.delegate = it.iterator();
        this.current = 0;
        this.end = end;
        while (current < start) {
            next();
        }
    }

    public boolean hasNext() {
        return current < end && delegate.hasNext();
    }

    public E next() {
        current++;
        return delegate.next();
    }

    public void remove() {
        delegate.remove();
    }
}