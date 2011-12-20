package com.flaptor.indextank.util;


public abstract class AbstractSkippableIterable<E> extends AbstractIterable<E> implements SkippableIterable<E> {

	/* (non-Javadoc)
	 * @see com.flaptor.indextank.util.SkippableIterable2#iterator()
	 */
	public abstract SkippableIterator<E> iterator();
	
}
