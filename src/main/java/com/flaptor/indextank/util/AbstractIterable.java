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

/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flaptor.indextank.util;

import com.google.common.collect.Iterables;

/**
 * Provides an implementation of {@code Object#toString} for {@code Iterable}
 * instances.
 *
 * @author Mike Bostock
 */
public abstract class AbstractIterable<E> implements Iterable<E> {
  /**
   * Returns a string representation of this iterable. The string representation
   * consists of a list of the iterable's elements in the order they are
   * returned by its iterator, enclosed in square brackets ("[]"). Adjacent
   * elements are separated by the characters ", " (comma and space). Elements
   * are converted to strings as by {@link String#valueOf(Object)}.
   */
  @Override public String toString() {
    return Iterables.toString(this);
  }
}
