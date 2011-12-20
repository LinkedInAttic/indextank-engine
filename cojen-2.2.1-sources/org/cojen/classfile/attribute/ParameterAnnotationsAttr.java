/*
 *  Copyright 2005-2010 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.classfile.attribute;

import java.util.Vector;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;

/**
 * Base class for parameter annotations attributes defined for Java 5.
 *
 * @author Brian S O'Neill
 * @see AnnotationsAttr
 */
public abstract class ParameterAnnotationsAttr extends Attribute {

    /** Contains Vectors of annotations */
    private Vector<Vector<Annotation>> mParameterAnnotations;
    
    public ParameterAnnotationsAttr(ConstantPool cp, String name) {
        super(cp, name);
        mParameterAnnotations = new Vector<Vector<Annotation>>(2);
    }
    
    public ParameterAnnotationsAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedByte();
        mParameterAnnotations = new Vector<Vector<Annotation>>(size);

        for (int i=0; i<size; i++) {
            int subSize = din.readUnsignedShort();
            Vector<Annotation> annotations = new Vector<Annotation>(subSize);

            for (int j=0; j<subSize; j++) {
                annotations.add(new Annotation(cp, din));
            }

            mParameterAnnotations.add(annotations);
        }
    }

    /**
     * First array index is zero-based parameter number.
     */
    public Annotation[][] getAnnotations() {
        Annotation[][] copy = new Annotation[mParameterAnnotations.size()][];
        for (int i=copy.length; --i>=0; ) {
            Vector<Annotation> annotations = mParameterAnnotations.get(i);
            if (annotations == null) {
                copy[i] = new Annotation[0];
            } else {
                copy[i] = annotations.toArray(new Annotation[annotations.size()]);
            }
        }
        return copy;
    }

    public int getParameterCount() {
        return mParameterAnnotations.size();
    }

    /**
     * @param parameter zero-based parameter number
     */
    public Annotation[] getAnnotations(int parameter) {
        Vector<Annotation> annotations = mParameterAnnotations.get(parameter);
        if (annotations == null) {
            return new Annotation[0];
        } else {
            return annotations.toArray(new Annotation[annotations.size()]);
        }
    }

    /**
     * @param parameter zero-based parameter number
     */
    public void addAnnotation(int parameter, Annotation annotation) {
        if (parameter >= mParameterAnnotations.size()) {
            mParameterAnnotations.setSize(parameter);
        }
        Vector<Annotation> annotations = mParameterAnnotations.get(parameter);
        if (annotations == null) {
            annotations = new Vector<Annotation>(2);
            mParameterAnnotations.set(parameter, annotations);
        }
        annotations.add(annotation);
    }
    
    public int getLength() {
        int length = 1;
        for (int i=mParameterAnnotations.size(); --i>=0; ) {
            Vector<Annotation> annotations = mParameterAnnotations.get(i);
            for (int j=annotations.size(); --j>=0; ) {
                length += 2 + annotations.get(j).getLength();
            }
        }
        return length;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mParameterAnnotations.size();
        dout.writeByte(size);
        for (int i=0; i<size; i++) {
            Vector<Annotation> annotations = mParameterAnnotations.get(i);
            int subSize = annotations.size();
            dout.writeShort(subSize);
            for (int j=0; j<subSize; j++) {
                annotations.get(j).writeTo(dout);
            }
        }
    }
}

