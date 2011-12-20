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
