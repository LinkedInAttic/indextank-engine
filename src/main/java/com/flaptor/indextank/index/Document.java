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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

public class Document implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String,String> fields;

    public Document() {
        this.fields = new HashMap<String,String>();
    }

    public Document(Map<String,String> fields) {
        Preconditions.checkNotNull(fields);
        this.fields = fields;
    }

    public void setField(String key, String value) {
        fields.put(key,value);
    }

    public String getField(String key) {
        return fields.get(key);
    }

    public Set<String> getFieldNames() {
        return fields.keySet();
    }

    @Override
    public String toString() {
    	return fields.toString();
    }
    
    public Map<String, String> asMap() {
    	return Collections.unmodifiableMap(fields);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Document other = (Document) obj;
        if (this.fields != other.fields && (this.fields == null || !this.fields.equals(other.fields))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.fields != null ? this.fields.hashCode() : 0);
        return hash;
    }
}
