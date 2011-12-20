/**
 * 
 */
package com.flaptor.util.collect;

import java.util.Iterator;

public class SlicingIterable<E> implements Iterable<E> {
    final int end;
    final Iterable<? extends E> delegate;
    final int start;

    public SlicingIterable(Iterable<? extends E> delegate, int start, int end) {
        this.delegate = delegate;
        this.start = start;
        this.end = end;
    }

    @Override
    public Iterator<E> iterator() {
        return new SlicingIterator<E>(delegate, start, end);
    }
}