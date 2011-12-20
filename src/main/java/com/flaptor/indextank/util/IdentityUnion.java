package com.flaptor.indextank.util;

import java.util.Arrays;

public class IdentityUnion<T extends Comparable<T>> extends Union<T, T> {
	
	public IdentityUnion(SkippableIterable<T>... cursors) {
		this(Arrays.asList(cursors));
	}

	public IdentityUnion(Iterable<SkippableIterable<T>> cursors) {
		super(cursors);
	}

	protected T transform(T t) {
		return t;
	}

}
