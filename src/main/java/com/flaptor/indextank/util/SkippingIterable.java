package com.flaptor.indextank.util;

import java.util.Iterator;

public class SkippingIterable<E> extends AbstractIterable<E> {

    public static class SkippingIterator<E> implements Iterator<E> {
        private Iterator<E> delegate;

        public SkippingIterator(Iterator<E> delegate, int skip) {
            this.delegate = delegate;
            while (this.delegate.hasNext() && skip > 0) {
                skip--;
                this.delegate.next();
            }
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            return delegate.next();
        }

        @Override
        public void remove() {
            delegate.remove();
        }
        
        public static <E> Iterator<E> skipping(Iterator<E> it, int skip) {
            return new SkippingIterator<E>(it, skip);
        }
    }

    private int skip;
    private Iterable<E> delegate;
    
    public SkippingIterable(Iterable<E> delegate, int skip) {
        this.skip = skip;
        this.delegate = delegate;
    }

    @Override
    public Iterator<E> iterator() {
        return new SkippingIterator<E>(delegate.iterator(), skip);
    }

    public static <E> Iterable<E> skipping(Iterable<E> it, int skip) {
        return new SkippingIterable<E>(it, skip);
    }

}
