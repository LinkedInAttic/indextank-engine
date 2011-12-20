package com.flaptor.indextank.util;

import com.flaptor.util.CollectionsUtil.PeekingIterator;

public class PeekingSkippableIterator<E> extends PeekingIterator<E> implements SkippableIterator<E> {

	private final SkippableIterator<? extends E> delegate;

	public PeekingSkippableIterator(SkippableIterator<? extends E> delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@Override
	public void skipTo(int i) {
		this.delegate.skipTo(i);		
	}


}
