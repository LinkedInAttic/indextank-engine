package com.flaptor.indextank.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class Skippables {

	private static final SkippableIterator<Object> EMPTY_ITERATOR = new SkippableIterator<Object>() {
		@Override
		public void skipTo(int i) {
		}
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public Object next() {
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			throw new IllegalStateException();
		}
	};
	private static final SkippableIterable<Object> EMPTY_ITERABLE = new AbstractSkippableIterable<Object>() {
		@Override
		public SkippableIterator<Object> iterator() {
			return EMPTY_ITERATOR;
		}
	};

	@SuppressWarnings("unchecked")
	public static <E> SkippableIterator<E> emptyIterator() {
		return (SkippableIterator<E>) EMPTY_ITERATOR;
	}
	@SuppressWarnings("unchecked")
	public static <E> SkippableIterable<E> emptyIterable() {
		return (SkippableIterable<E>) EMPTY_ITERABLE;
	}

	public static <E> SkippableIterable<E> fromIterable(final Iterable<E> iterable) {
		return new AbstractSkippableIterable<E>() {
			@Override
			public SkippableIterator<E> iterator() {
				return fromIterator(iterable.iterator());
			}
		};
	}
	public static <E> SkippableIterator<E> fromIterator(final Iterator<E> iterator) {
		return new SkippableIterator<E>() {
			@Override
			public void skipTo(int i) {
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}
			@Override
			public E next() {
				return iterator.next();
			}
			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}
	public static <F,T> SkippableIterable<T> transform(final SkippableIterable<F> fromIterable, final Function<F, T> function) {
	    checkNotNull(fromIterable);
	    checkNotNull(function);
	    return new AbstractSkippableIterable<T>() {
	    	public SkippableIterator<T> iterator() {
	    		return transform(fromIterable.iterator(), function);
	    	}
	    };
	}
	public static <F,T> SkippableIterator<T> transform(final SkippableIterator<F> fromIterator, final Function<F, T> function) {
		checkNotNull(fromIterator);
		checkNotNull(function);
		return new SkippableIterator<T>() {
			public boolean hasNext() {
				return fromIterator.hasNext();
			}
			public T next() {
				F from = fromIterator.next();
				return function.apply(from);
			}
			public void remove() {
				fromIterator.remove();
			}
			@Override
			public void skipTo(int i) {
				fromIterator.skipTo(i);
			}
	    };
	}
	public static <T> SkippableIterable<T> filter(final SkippableIterable<T> unfiltered, final Predicate<? super T> predicate) {
		checkNotNull(unfiltered);
		checkNotNull(predicate);
		return new AbstractSkippableIterable<T>() {
			public SkippableIterator<T> iterator() {
				return filter(unfiltered.iterator(), predicate);
			}
		};
	}
	public static <T> SkippableIterator<T> filter(final SkippableIterator<T> unfiltered, final Predicate<? super T> predicate) {
		checkNotNull(unfiltered);
		checkNotNull(predicate);
		return new AbstractSkippableIterator<T>() {
			@Override protected T computeNext() {
				while (unfiltered.hasNext()) {
					T element = unfiltered.next();
					if (predicate.apply(element)) {
						return element;
					}
				}
				return endOfData();
			}
			@Override
			public void skipTo(int i) {
				unfiltered.skipTo(i);
			}
		};
	}

	
}
