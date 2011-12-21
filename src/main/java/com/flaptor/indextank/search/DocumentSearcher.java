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

package com.flaptor.indextank.search;

import java.util.Map;

import com.flaptor.indextank.query.Query;


public interface DocumentSearcher {

    /**
     * Search interface.
     * @param query a AQuery implementation.
     * @param start the offset of the first document to retrieve.
     * @param limit the number of documents to retrieve.
     * @param scoringFunctionIndex TODO
     * @param snippetFields TODO
     * @param fetchFields TODO
     * @param snippetFields the field names for which snippets should be created 
     * @param fetchFields the field names that should be fetched completely
     */
    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex, Map<String, String> extraParameters) throws InterruptedException;

    public SearchResults search(Query query, int start, int limit, int scoringFunctionIndex) throws InterruptedException;

    public int countMatches(Query query) throws InterruptedException;

}
