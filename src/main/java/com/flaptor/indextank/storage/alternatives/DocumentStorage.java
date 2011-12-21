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

package com.flaptor.indextank.storage.alternatives;

import java.io.IOException;
import java.util.Map;

import com.flaptor.indextank.index.Document;

public interface DocumentStorage {
	public Document getDocument(String docId);
	public void saveDocument(String docId, Document document);
	public void deleteDocument(String docId);
    public void dump() throws IOException;
    public Map<String, String> getStats();
}
