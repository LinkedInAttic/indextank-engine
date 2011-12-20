/*
 *  Copyright 2007-2010 Brian S O'Neill
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

/*
 * Copyright 2006 Amazon Technologies, Inc. or its affiliates.
 * Amazon, Amazon.com and Carbonado are trademarks or registered trademarks
 * of Amazon Technologies, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cojen.util;

import java.util.Stack;

import org.cojen.classfile.TypeDesc;
import org.cojen.classfile.attribute.Annotation;

/**
 * Builds all Annotation properties to a Cojen Annotation definition.
 *
 * @author Brian S O'Neill
 * @since 2.1
 */
public class AnnotationBuilder extends AnnotationVisitor<Object, Annotation> {

    // Stack for building arrays of annotations.
    private Stack<Annotation.MemberValue[]> mStack;

    public AnnotationBuilder() {
        super(false);
        mStack = new Stack<Annotation.MemberValue[]>();
    }

    public Object visit(String name, int pos, java.lang.annotation.Annotation value,
                        Annotation ann)
    {
        if (name == null && mStack.size() == 0) {
            // Root annotation.
            super.visit(name, pos, value, ann);
        } else {
            // Nested annotation.
            Annotation nested = ann.makeAnnotation();
            nested.setType(TypeDesc.forClass(value.annotationType()));
            super.visit(name, pos, value, nested);
            put(ann, name, pos, ann.makeMemberValue(nested));
        }
        return null;
    }

    public Object visit(String name, int pos, int value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, long value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, float value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, double value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, boolean value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, byte value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, short value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, char value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, String value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(value));
        return null;
    }

    public Object visit(String name, int pos, Class value, Annotation ann) {
        put(ann, name, pos, ann.makeMemberValue(TypeDesc.forClass(value)));
        return null;
    }

    public Object visit(String name, int pos, Enum value, Annotation ann) {
        put(ann, name, pos,
            ann.makeMemberValue(TypeDesc.forClass(value.getDeclaringClass()), value.name()));
        return null;
    }

    public Object visit(String name, int pos, java.lang.annotation.Annotation[] value,
                        Annotation ann)
    {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, int[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, long[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, float[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, double[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, boolean[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, byte[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, short[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, char[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, String[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, Class[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    public Object visit(String name, int pos, Enum[] value, Annotation ann) {
        mStack.push(new Annotation.MemberValue[value.length]);
        super.visit(name, pos, value, ann);
        put(ann, name, pos, ann.makeMemberValue(mStack.pop()));
        return null;
    }

    private void put(Annotation ann, String name, int pos, Annotation.MemberValue mv) {
        if (name == null) {
            mStack.peek()[pos] = mv;
        } else {
            ann.putMemberValue(name, mv);
        }
    }
}
