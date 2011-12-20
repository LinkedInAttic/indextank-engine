
package com.flaptor.util;

import com.google.common.collect.Iterables;

public abstract class AbstractIterable<E> implements Iterable<E> {

  @Override public String toString() {
    return Iterables.toString(this);
  }

}
