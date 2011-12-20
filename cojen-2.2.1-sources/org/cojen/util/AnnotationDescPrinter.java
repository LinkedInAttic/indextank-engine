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

import java.lang.annotation.Annotation;

import org.cojen.classfile.TypeDesc;

/**
 * Prints machine readable, self-describing, annotation descriptors.
 *
 * @author Brian S O'Neill
 * @see AnnotationDescParser
 * @since 2.1
 */
public class AnnotationDescPrinter extends AnnotationVisitor<Object, Object> {
    /**
     * Returns an annotation descriptor that has no parameters.
     */
    public static String makePlainDescriptor(Class<? extends Annotation> annotationType) {
        return "" + TAG_ANNOTATION + TypeDesc.forClass(annotationType).getDescriptor();
    }

    /**
     * Returns an annotation descriptor that has no parameters.
     */
    public static String makePlainDescriptor(String annotationType) {
        return "" + TAG_ANNOTATION + TAG_OBJECT + annotationType.replace('.', '/') + ';';
    }

    static final char
        TAG_BOOLEAN    = 'Z',
        TAG_BYTE       = 'B',
        TAG_SHORT      = 'S',
        TAG_CHAR       = 'C',
        TAG_INT        = 'I',
        TAG_LONG       = 'J',
        TAG_FLOAT      = 'F',
        TAG_DOUBLE     = 'D',
        TAG_VOID       = 'V',
        TAG_OBJECT     = 'L',
        TAG_ARRAY      = '[',
        TAG_STRING     = 's',
        TAG_CLASS      = 'c',
        TAG_ENUM       = 'e',
        TAG_ANNOTATION = '@';

    private final StringBuilder mBuilder;

    /**
     * @param sort when true, sort annotation members by name (case sensitive)
     * @param b StringBuilder to get printed results
     */
    public AnnotationDescPrinter(boolean sort, StringBuilder b) {
        super(sort);
        mBuilder = b;
    }

    /**
     * Prints the annotation to the builder passed to the constructor.
     *
     * @param value Annotation to visit
     * @return null
     */
    public Object visit(Annotation value) {
        return visit(value, null);
    }

    public Object visit(String name, int pos, Annotation value, Object param) {
        if (appendName(name, pos, TAG_ANNOTATION)) {
            mBuilder.append(TypeDesc.forClass(value.annotationType()).getDescriptor());
        }
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, int value, Object param) {
        appendName(name, pos, TAG_INT);
        mBuilder.append(value);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, long value, Object param) {
        appendName(name, pos, TAG_LONG);
        mBuilder.append(value);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, float value, Object param) {
        appendName(name, pos, TAG_FLOAT);
        mBuilder.append(Float.toString(value));
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, double value, Object param) {
        appendName(name, pos, TAG_DOUBLE);
        mBuilder.append(Double.toString(value));
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, boolean value, Object param) {
        appendName(name, pos, TAG_BOOLEAN);
        mBuilder.append(value ? '1' : '0');
        return null;
    }

    public Object visit(String name, int pos, byte value, Object param) {
        appendName(name, pos, TAG_BYTE);
        mBuilder.append(value);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, short value, Object param) {
        appendName(name, pos, TAG_SHORT);
        mBuilder.append(value);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, char value, Object param) {
        appendName(name, pos, TAG_CHAR);
        mBuilder.append(value);
        return null;
    }

    public Object visit(String name, int pos, String value, Object param) {
        appendName(name, pos, TAG_STRING);
        int length = value.length();
        for (int i=0; i<length; i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == ';') {
                mBuilder.append('\\');
            }
            mBuilder.append(c);
        }
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, Class value, Object param) {
        appendName(name, pos, TAG_CLASS);
        if (value == String.class) {
            mBuilder.append(TAG_STRING);
        } else if (value == Class.class) {
            mBuilder.append(TAG_CLASS);
        } else {
            mBuilder.append(TypeDesc.forClass(value).getDescriptor());
        }
        return null;
    }

    public Object visit(String name, int pos, Enum value, Object param) {
        if (appendName(name, pos, TAG_ENUM)) {
            mBuilder.append(value.getDeclaringClass().getName());
            mBuilder.append('.');
        }
        mBuilder.append(value.name());
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, Annotation[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, int[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, long[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, float[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, double[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, boolean[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, byte[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, short[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, char[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, String[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, Class[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    public Object visit(String name, int pos, Enum[] value, Object param) {
        appendName(name, pos, TAG_ARRAY);
        super.visit(name, pos, value, param);
        mBuilder.append(';');
        return null;
    }

    /**
     * @return true if tag was appended too
     */
    private boolean appendName(String name, int pos, char tag) {
        if (name != null) {
            mBuilder.append(name);
            mBuilder.append('=');
        }
        if (name != null || pos == 0) {
            mBuilder.append(tag);
            return true;
        }
        return false;
    }
}
