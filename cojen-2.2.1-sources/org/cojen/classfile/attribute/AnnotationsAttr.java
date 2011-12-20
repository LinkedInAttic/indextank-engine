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

import java.util.ArrayList;
import java.util.List;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;

/**
 * Base class for annotations attributes defined for Java 5.
 *
 * @author Brian S O'Neill
 * @see ParameterAnnotationsAttr
 */
public abstract class AnnotationsAttr extends Attribute {

    private List<Annotation> mAnnotations;
    
    public AnnotationsAttr(ConstantPool cp, String name) {
        super(cp, name);
        mAnnotations = new ArrayList<Annotation>(2);
    }
    
    public AnnotationsAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);
        
        int size = din.readUnsignedShort();
        mAnnotations = new ArrayList<Annotation>(size);
        
        for (int i=0; i<size; i++) {
            addAnnotation(new Annotation(cp, din));
        }
    }

    public Annotation[] getAnnotations() {
        return mAnnotations.toArray(new Annotation[mAnnotations.size()]);
    }

    public void addAnnotation(Annotation annotation) {
        mAnnotations.add(annotation);
    }
    
    public int getLength() {
        int length = 2;
        for (int i=mAnnotations.size(); --i>=0; ) {
            length += mAnnotations.get(i).getLength();
        }
        return length;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mAnnotations.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            mAnnotations.get(i).writeTo(dout);
        }
    }
}

