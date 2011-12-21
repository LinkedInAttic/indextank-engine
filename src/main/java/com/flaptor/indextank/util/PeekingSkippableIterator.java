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
