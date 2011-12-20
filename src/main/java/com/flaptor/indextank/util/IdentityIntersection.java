/**
 * 
 */
package com.flaptor.indextank.util;

import java.util.Arrays;

public class IdentityIntersection<T extends Comparable<T>> extends Intersection<T, T> {
	public IdentityIntersection(SkippableIterable<T>... cursors) {
		super(Arrays.asList(cursors));
	}
	public IdentityIntersection(Iterable<SkippableIterable<T>> cursors) {
		super(cursors);
	}

	@Override
	protected T transform(T t) {
		return t;
	}
}