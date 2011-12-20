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

package org.cojen.classfile;

import java.util.ArrayList;

/**
 * Utility class that supports parsing of Java method declarations. For
 * example, <code>public static int myMethod(java.lang.String, int value,
 * java.lang.String... extra)</code>.
 *
 * <p>The parser is fairly lenient. It doesn't care if the set of modifiers is
 * illegal, and it doesn't require that arguments have variable names assigned.
 * A semi-colon may appear at the end of the signature.
 *
 * <p>At most one variable argument is supported (at the end), and all class
 * names must be fully qualified.
 *
 * @author Brian S O'Neill
 */
public class MethodDeclarationParser {
    /**
     * @param src descriptor source string
     * @param pos position in source, updated at exit
     */
    private static void skipWhitespace(String src, int[] pos) {
        int length = src.length();
        int i = pos[0];
        while (i < length) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else {
                break;
            }
        }
        pos[0] = i;
    }

    /**
     * @param src descriptor source string
     * @param pos pos[0] is position in source, updated at exit
     * @return null if nothing left
     */
    private static String parseIdentifier(String src, int[] pos) {
        // Skip leading whitespace.
        skipWhitespace(src, pos);
        int i = pos[0];
        int length = src.length();
        if (i >= length) {
            return null;
        }

        int startPos = i;
        char c = src.charAt(i);
        if (!Character.isJavaIdentifierStart(c)) {
            return null;
        }
        i++;

        while (i < length) {
            c = src.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                i++;
            } else {
                break;
            }
        }

        pos[0] = i;

        // Skip trailing whitespace too.
        skipWhitespace(src, pos);

        return src.substring(startPos, i);
    }

    private static TypeDesc parseTypeDesc(String src, int[] pos) {
        // Skip leading whitespace.
        skipWhitespace(src, pos);
        int i = pos[0];
        int length = src.length();
        if (i >= length) {
            return null;
        }

        int startPos = i;
        char c = src.charAt(i);
        if (!Character.isJavaIdentifierStart(c)) {
            return null;
        }
        i++;

        while (i < length) {
            c = src.charAt(i);
            if (c == '.') {
                // Don't allow double dots.
                if (i + 1 < length && src.charAt(i + 1) == '.') {
                    break;
                } else {
                    i++;
                }
            } else if (Character.isJavaIdentifierPart(c) || c == '[' || c == ']') {
                i++;
            } else {
                break;
            }
        }

        pos[0] = i;

        // Skip trailing whitespace too.
        skipWhitespace(src, pos);

        return TypeDesc.forClass(src.substring(startPos, i));
    }

    /**
     * @param src descriptor source string
     * @param pos pos[0] is position in source, updated at exit
     */
    private static Modifiers parseModifiers(String src, int[] pos) {
        Modifiers modifiers = Modifiers.NONE;

        int length = src.length();
        loop: while (pos[0] < length) {
            int savedPos = pos[0];
            String ident = parseIdentifier(src, pos);
            int newPos = pos[0];
            pos[0] = savedPos;

            if (ident == null) {
                break;
            }

            switch (ident.charAt(0)) {
            case 'a':
                if ("abstract".equals(ident)) {
                    modifiers = modifiers.toAbstract(true);
                } else {
                    break loop;
                }
                break;

            case 'f':
                if ("final".equals(ident)) {
                    modifiers = modifiers.toFinal(true);
                } else {
                    break loop;
                }
                break;

            case 'n':
                if ("native".equals(ident)) {
                    modifiers = modifiers.toNative(true);
                } else {
                    break loop;
                }
                break;

            case 'p':
                if ("public".equals(ident)) {
                    modifiers = modifiers.toPublic(true);
                } else if ("private".equals(ident)) {
                    modifiers = modifiers.toPrivate(true);
                } else if ("protected".equals(ident)) {
                    modifiers = modifiers.toProtected(true);
                } else {
                    break loop;
                }
                break;

            case 's':
                if ("static".equals(ident)) {
                    modifiers = modifiers.toStatic(true);
                } else if ("synchronized".equals(ident)) {
                    modifiers = modifiers.toSynchronized(true);
                } else if ("strict".equals(ident)) {
                    modifiers = modifiers.toStrict(true);
                } else {
                    break loop;
                }
                break;

            case 't':
                if ("transient".equals(ident)) {
                    modifiers = modifiers.toTransient(true);
                } else {
                    break loop;
                }
                break;

            case 'v':
                if ("volatile".equals(ident)) {
                    modifiers = modifiers.toVolatile(true);
                } else {
                    break loop;
                }
                break;

            default:
                break loop;
            } // end switch

            // If this point is reached, valid modifier was parsed. Advance position.
            pos[0] = newPos;
        }

        return modifiers;
    }

    private static TypeDesc[] parseParameters(String src, int[] pos, boolean[] isVarArgs) {
        // Skip trailing whitespace too.
        skipWhitespace(src, pos);
        int length = src.length();
        if (pos[0] < length && src.charAt(pos[0]) != '(') {
            throw new IllegalArgumentException("Left paren expected");
        }
        pos[0]++;

        ArrayList<TypeDesc> list = new ArrayList<TypeDesc>();

        boolean expectParam = false;
        while (pos[0] < length) {
            TypeDesc type = parseTypeDesc(src, pos);
            if (type == null) {
                if (expectParam) {
                    throw new IllegalArgumentException("Parameter type expected");
                }
                break;
            }
            list.add(type);

            // Parse optional parameter name
            parseIdentifier(src, pos);

            if (pos[0] < length) {
                char c = src.charAt(pos[0]);
                if (c == ',') {
                    // More params to follow...
                    pos[0]++;
                    expectParam = true;
                    continue;
                } else if (c == ')') {
                    pos[0]++;
                    break;
                }

                // Handle varargs.
                if (c == '.' && pos[0] + 2 < length) {
                    if (src.charAt(pos[0] + 1) == '.' && src.charAt(pos[0] + 2) == '.') {
                        type = type.toArrayType();
                        isVarArgs[0] = true;
                        list.set(list.size() - 1, type);
                        pos[0] += 3;
                        expectParam = false;
                        continue;
                    }
                }

                throw new IllegalArgumentException("Expected comma or right paren");
            }
        }

        return list.toArray(new TypeDesc[list.size()]);
    }

    private final Modifiers mModifiers;
    private final TypeDesc mReturnType;
    private final String mMethodName;
    private final TypeDesc[] mParameters;

    /**
     * Parse the given method declaration, throwing a exception if the syntax
     * is wrong.
     *
     * @param declaration declaration to parse, which matches Java syntax
     * @throws IllegalArgumentException if declaration syntax is wrong
     */
    public MethodDeclarationParser(String declaration) throws IllegalArgumentException {
        int[] pos = new int[1];
        Modifiers modifiers = parseModifiers(declaration, pos);
        mReturnType = parseTypeDesc(declaration, pos);
        if (mReturnType == null) {
            throw new IllegalArgumentException("No return type");
        }
        mMethodName = parseIdentifier(declaration, pos);
        if (mMethodName == null) {
            throw new IllegalArgumentException("No method name");
        }
        boolean[] isVarArgs = new boolean[1];
        mParameters = parseParameters(declaration, pos, isVarArgs);
        if (isVarArgs[0]) {
            modifiers = modifiers.toVarArgs(true);
        }
        mModifiers = modifiers;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    public TypeDesc getReturnType() {
        return mReturnType;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public TypeDesc[] getParameters() {
        return (TypeDesc[])mParameters.clone();
    }
}
