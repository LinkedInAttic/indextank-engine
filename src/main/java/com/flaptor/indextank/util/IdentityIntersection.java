/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
