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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;


public class DocId implements Serializable, Comparable<DocId> {
    
    private static final long serialVersionUID = 1L;
    
    private byte[] buffer;
    private int start;
    private int count;
    private int hash;
    
    public DocId(String string) {
        try {
            this.buffer = string.getBytes("UTF-8");
            this.start = 0;
            this.count = this.buffer.length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public DocId(byte[] buffer, int start, int count) {
        this.buffer = buffer;
        this.start = start;
        this.count = count;
    }
    
    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            int off = start; 
            byte[] buf = buffer;
            int len = count;
            for (int i = 0; i < len; i++) {
                h = 31*h + buf[off++];
            }
            hash = h;
        }
        return h;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DocId) {
            DocId oid = (DocId)obj;
            int n = count;
            if (n == oid.count) {
                byte v1[] = buffer;
                byte v2[] = oid.buffer;
                int i = start;
                int j = oid.start;
                while (n-- != 0) {
                    if (v1[i++] != v2[j++])
                        return false;
                }
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return new String(buffer, start, count);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeInt(count);
        oos.write(buffer, start, count);
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        this.start = 0;
        this.count = ois.readInt();
        this.buffer = new byte[count];
        ois.read(this.buffer);
    }
    public void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(count);
        dos.write(buffer, start, count);
    }
    public static void writeNull(DataOutputStream dos) throws IOException {
        dos.writeInt(-1);
    }
    
    public static DocId readData(DataInputStream dis) throws IOException {
        int count = dis.readInt();
        if (count < 0) return null;
        byte[] buffer = new byte[count];
        dis.read(buffer);
        return new DocId(buffer, 0, count);
    }

    @Override
    public int compareTo(DocId o) {
        int n = Math.min(count, o.count);
        int i = start, j = o.start;
        int end = i + n;
        if (i == j) {
          while (i < end) {
              int c = buffer[i] - o.buffer[i];
              if (c != 0) return c;
              ++i;
          }
        } else {
            while (i < end) {
                int c = buffer[i++] - o.buffer[j++];
                if (c != 0) return c;
            }
        }
        return count - o.count;
    }

    public void update(byte[] buffer, int start, int count) {
        this.buffer = buffer;
        this.start = start;
        this.count = count;
        this.hash = 0;
    }
    
    public void updateFrom(DocId docid) {
        byte[] buf = this.buffer;
        byte[] obuf = docid.buffer;
        int off = docid.start;
        int len = docid.count;
        if (buf.length < docid.count) {
            buf = new byte[docid.count];
            this.buffer = buf;
        }
        System.arraycopy(obuf, off, buf, 0, len);
        this.start = 0;
        this.count = docid.count;
        this.hash = 0;
    }
    
    public DocId copy() {
        return this.copy(this.count);
    }
    public DocId copy(int targetBufferSize) {
        byte[] buf = this.buffer;
        int off = this.start;
        int len = this.count;
        
        targetBufferSize = Math.max(targetBufferSize, len);
        byte[] clonedBuffer = new byte[targetBufferSize];
        System.arraycopy(buf, off, clonedBuffer, 0, len);
        return new DocId(clonedBuffer, 0, len);
    }
    
}
