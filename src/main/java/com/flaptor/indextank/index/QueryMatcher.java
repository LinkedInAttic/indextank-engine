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

package com.flaptor.indextank.index;

import com.flaptor.indextank.query.Query;
import com.google.common.base.Predicate;


public interface QueryMatcher {

    /**
     * Search interface.
     * @param query a AQuery implementation.
     * @param limit the number of documents to retrieve.
     * @param scoringFunctionIndex TODO
     */
    public TopMatches findMatches(Query query, int limit, int scoringFunctionIndex) throws InterruptedException;
    public TopMatches findMatches(Query query, Predicate<DocId> idFilter, int limit, int scoringFunctionIndex) throws InterruptedException;
    
    public int countMatches(Query query) throws InterruptedException;
    public int countMatches(Query query, Predicate<DocId> idFilter) throws InterruptedException;
    
    public boolean hasChanges(DocId docid) throws InterruptedException;

}
