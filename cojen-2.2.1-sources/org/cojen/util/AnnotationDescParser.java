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

import java.util.ArrayList;
import java.util.List;

import org.cojen.classfile.TypeDesc;
import org.cojen.classfile.attribute.Annotation;

// Import the tags.
import static org.cojen.util.AnnotationDescPrinter.*;

/**
 * Parses an annotation descriptor String to a Cojen Annotation definition.
 *
 * @author Brian S O'Neill
 * @see AnnotationDescPrinter
 * @since 2.1
 */
public class AnnotationDescParser {
    private final String mStr;

    private int mPos;

    /**
     * @param annotationString annotation to parse
     */
    public AnnotationDescParser(String annotationString) {
        mStr = annotationString;
    }

    /**
     * Parses the given annotation, returning the root annotation that received
     * the results.
     *
     * @param rootAnnotation root annotation
     * @return root annotation
     * @throws IllegalArgumentExcecption if annotation is malformed
     */
    public Annotation parse(Annotation rootAnnotation) {
        mPos = 0;

        if (parseTag() != TAG_ANNOTATION) {
            throw error("Malformed");
        }

        TypeDesc rootAnnotationType = parseTypeDesc();

        if (rootAnnotation == null) {
            rootAnnotation = buildRootAnnotation(rootAnnotationType);
        } else if (!rootAnnotationType.equals(rootAnnotation.getType())) {
            throw new IllegalArgumentException
                ("Annotation type of \"" + rootAnnotationType +
                 "\" does not match expected type of \"" + rootAnnotation.getType());
        }

        parseAnnotation(rootAnnotation, rootAnnotationType);

        return rootAnnotation;
    }

    /**
     * Override this method if a root annotation is not provided, as it must be
     * built after parsing the root annotation type. By default, this method
     * throws UnsupportedOperationException.
     */
    protected Annotation buildRootAnnotation(TypeDesc rootAnnotationType) {
        throw new UnsupportedOperationException();
    }

    private void parseAnnotation(Annotation dest, TypeDesc annType) {
        while (true) {
            try {
                if (mStr.charAt(mPos) == ';') {
                    mPos++;
                    break;
                }
            } catch (IndexOutOfBoundsException e) {
                error("Missing terminator");
            }

            if (mPos >= mStr.length()) {
                break;
            }

            String propName = parseName();
            char propTag = peekTag();

            Annotation.MemberValue mv;
            if (propTag == TAG_ARRAY) {
                mPos++;
                mv = parseArray(dest, peekTag(), parseTypeDesc());
            } else {
                mv = parseProperty(dest, propTag, parseTypeDesc());
            }

            dest.putMemberValue(propName, mv);
        }
    }

    private Annotation.MemberValue parseArray(Annotation dest, char compTag, TypeDesc compType) {
        List<Annotation.MemberValue> mvList = new ArrayList<Annotation.MemberValue>();
        while (true) {
            try {
                if (mStr.charAt(mPos) == ';') {
                    mPos++;
                    break;
                }
            } catch (IndexOutOfBoundsException e) {
                error("Missing terminator");
            }

            mvList.add(parseProperty(dest, compTag, compType));
        }
        Annotation.MemberValue[] mvArray = new Annotation.MemberValue[mvList.size()];
        return dest.makeMemberValue(mvList.toArray(mvArray));
    }

    private Annotation.MemberValue parseProperty(Annotation dest, char propTag, TypeDesc propType)
    {
        Annotation.MemberValue mv;

        try {
            if (propTag == TAG_STRING) {
                StringBuilder b = new StringBuilder();
                while (true) {
                    char c = mStr.charAt(mPos++);
                    if (c == ';') {
                        break;
                    }
                    if (c == '\\') {
                        c = mStr.charAt(mPos++);
                    }
                    b.append(c);
                }
                mv = dest.makeMemberValue(b.toString());
            } else if (propTag == TAG_BOOLEAN) {
                mv = dest.makeMemberValue(mStr.charAt(mPos++) != '0');
            } else if (propTag == TAG_CHAR) {
                mv = dest.makeMemberValue(mStr.charAt(mPos++));
            } else if (propTag == TAG_ANNOTATION) {
                Annotation propAnn = dest.makeAnnotation();
                propAnn.setType(propType);
                parseAnnotation(propAnn, propType);
                mv = dest.makeMemberValue(propAnn);
            } else if (propTag == TAG_CLASS) {
                mv = dest.makeMemberValue(parseTypeDesc());
            } else {
                int endPos = nextTerminator();
                String valueStr = mStr.substring(mPos, endPos);

                switch (propTag) {
                default:
                    throw error("Invalid tag");

                case TAG_BYTE: {
                    mv = dest.makeMemberValue(Byte.parseByte(valueStr));
                    break;
                }

                case TAG_SHORT: {
                    mv = dest.makeMemberValue(Short.parseShort(valueStr));
                    break;
                }

                case TAG_INT: {
                    mv = dest.makeMemberValue(Integer.parseInt(valueStr));
                    break;
                }

                case TAG_LONG: {
                    mv = dest.makeMemberValue(Long.parseLong(valueStr));
                    break;
                }

                case TAG_FLOAT: {
                    mv = dest.makeMemberValue(Float.parseFloat(valueStr));
                    break;
                }

                case TAG_DOUBLE: {
                    mv = dest.makeMemberValue(Double.parseDouble(valueStr));
                    break;
                }

                case TAG_ENUM: {
                    mv = dest.makeMemberValue(propType, valueStr);
                    break;
                }
                }

                mPos = endPos + 1;
            }
        } catch (IndexOutOfBoundsException e) {
            throw error("Invalid property value");
        }

        return mv;
    }

    private TypeDesc parseTypeDesc() {
        try {
            int endPos;

            switch (mStr.charAt(mPos)) {
            default:
                throw error("Invalid tag");

            case TAG_ARRAY:
                mPos++;
                return parseTypeDesc().toArrayType();

            case TAG_STRING:
                mPos++;
                return TypeDesc.STRING;

            case TAG_CLASS:
                mPos++;
                return TypeDesc.forClass(Class.class);

            case TAG_ANNOTATION:
                mPos++;
                return parseTypeDesc();

            case TAG_ENUM:
                mPos++;
                endPos = nextTerminator();
                int dot = mStr.lastIndexOf('.', endPos);
                if (dot < mPos) {
                    throw error("Invalid enumeration");
                }
                TypeDesc type = TypeDesc.forClass(mStr.substring(mPos, dot));
                mPos = dot + 1;
                return type;

            case TAG_BOOLEAN:
            case TAG_BYTE:
            case TAG_SHORT:
            case TAG_CHAR:
            case TAG_INT:
            case TAG_LONG:
            case TAG_FLOAT:
            case TAG_DOUBLE:
            case TAG_VOID:
                endPos = mPos + 1;
                break;

            case TAG_OBJECT:
                endPos = nextTerminator() + 1;
                break;
            }

            TypeDesc type = TypeDesc.forDescriptor(mStr.substring(mPos, endPos));
            mPos = endPos;

            return type;
        } catch (IndexOutOfBoundsException e) {
            throw error("Invalid type descriptor");
        }
    }

    private char peekTag() {
        char tag = parseTag();
        mPos--;
        return tag;
    }

    private char parseTag() {
        try {
            return mStr.charAt(mPos++);
        } catch (IndexOutOfBoundsException e) {
            throw error("Too short");
        }
    }

    private String parseName() {
        int index = mStr.indexOf('=', mPos);
        if (index < 0) {
            throw error("Incomplete assignment");
        }
        String name = mStr.substring(mPos, index);
        mPos = index + 1;
        return name;
    }

    private int nextTerminator() {
        int index = mStr.indexOf(';', mPos);
        if (index < 0) {
            throw error("Missing terminator");
        }
        return index;
    }

    private IllegalArgumentException error(String message) {
        message = "Illegal annotation descriptor: " + message +
            " at position " + mPos + " of " + mStr;
        return new IllegalArgumentException(message);
    }
}
