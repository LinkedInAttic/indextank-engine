/*
 *  Copyright 2004-2010 Brian S O'Neill
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

import java.lang.reflect.Modifier;

/**
 * The Modifiers class is an immutable wrapper around a modifier bitmask. The
 * methods provided to manipulate the bitmask ensure that it is always
 * legal. i.e. setting it public automatically clears it from being private or
 * protected.
 * 
 * @author Brian S O'Neill
 */
public class Modifiers {
    public static final Modifiers NONE;
    public static final Modifiers PUBLIC;
    public static final Modifiers PUBLIC_ABSTRACT;
    public static final Modifiers PUBLIC_STATIC;
    public static final Modifiers PROTECTED;
    public static final Modifiers PRIVATE;

    static {
        NONE = new Modifiers(0);
        PUBLIC = new Modifiers(Modifier.PUBLIC);
        PUBLIC_ABSTRACT = new Modifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
        PUBLIC_STATIC = new Modifiers(Modifier.PUBLIC | Modifier.STATIC);
        PROTECTED = new Modifiers(Modifier.PROTECTED);
        PRIVATE = new Modifiers(Modifier.PRIVATE);
    }

    /**
     * Returns a Modifiers object with the given bitmask.
     */
    public static Modifiers getInstance(int bitmask) {
        switch (bitmask) {
        case 0:
            return NONE;
        case Modifier.PUBLIC:
            return PUBLIC;
        case Modifier.PUBLIC | Modifier.ABSTRACT:
            return PUBLIC_ABSTRACT;
        case Modifier.PUBLIC | Modifier.STATIC:
            return PUBLIC_STATIC;
        case Modifier.PROTECTED:
            return PROTECTED;
        case Modifier.PRIVATE:
            return PRIVATE;
        }

        return new Modifiers(bitmask);
    }

    private static int toPublic(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.PUBLIC) & (~Modifier.PROTECTED & ~Modifier.PRIVATE);
        } else {
            return bitmask & ~Modifier.PUBLIC;
        }
    }
    
    private static int toPrivate(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.PRIVATE) & (~Modifier.PUBLIC & ~Modifier.PROTECTED);
        } else {
            return bitmask & ~Modifier.PRIVATE;
        }
    }

    private static int toProtected(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.PROTECTED) & (~Modifier.PUBLIC & ~Modifier.PRIVATE);
        } else {
            return bitmask & ~Modifier.PROTECTED;
        }
    }
    
    private static int toStatic(int bitmask, boolean b) {
        if (b) {
            return bitmask | Modifier.STATIC;
        } else {
            return bitmask & ~Modifier.STATIC;
        }
    }

    private static int toFinal(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.FINAL) & (~Modifier.INTERFACE & ~Modifier.ABSTRACT);
        } else {
            return bitmask & ~Modifier.FINAL;
        }
    }
    
    private static int toSynchronized(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.SYNCHRONIZED) &
                (~Modifier.VOLATILE & ~Modifier.TRANSIENT & ~Modifier.INTERFACE);
        } else {
            return bitmask & ~Modifier.SYNCHRONIZED;
        }
    }
    
    private static int toVolatile(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.VOLATILE) &
                (~Modifier.SYNCHRONIZED & ~Modifier.NATIVE & ~Modifier.INTERFACE &
                 ~Modifier.ABSTRACT & ~Modifier.STRICT);
        } else {
            return bitmask & ~Modifier.VOLATILE;
        }
    }
    
    private static int toTransient(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.TRANSIENT) &
                (~Modifier.SYNCHRONIZED & ~Modifier.NATIVE &
                 ~Modifier.INTERFACE & ~Modifier.ABSTRACT & ~Modifier.STRICT);
        } else {
            return bitmask & ~Modifier.TRANSIENT;
        }
    }
    
    private static int toNative(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.NATIVE) & 
                (~Modifier.VOLATILE & ~Modifier.TRANSIENT &
                 ~Modifier.INTERFACE & ~Modifier.ABSTRACT & ~Modifier.STRICT);
        } else {
            return bitmask & ~Modifier.NATIVE;
        }
    }
    
    private static int toInterface(int bitmask, boolean b) {
        if (b) {
            return (bitmask | (Modifier.INTERFACE | Modifier.ABSTRACT)) & 
                (~Modifier.FINAL & ~Modifier.SYNCHRONIZED &
                 ~Modifier.VOLATILE & ~Modifier.TRANSIENT & ~Modifier.NATIVE);
        } else {
            return bitmask & ~Modifier.INTERFACE;
        }
    }

    private static int toAbstract(int bitmask, boolean b) {
        if (b) {
            return (bitmask | Modifier.ABSTRACT) & 
                (~Modifier.FINAL & ~Modifier.VOLATILE & ~Modifier.TRANSIENT & ~Modifier.NATIVE &
                 ~Modifier.SYNCHRONIZED & ~Modifier.STRICT);
        } else {
            return bitmask & ~Modifier.ABSTRACT & ~Modifier.INTERFACE;
        }
    }

    private static int toStrict(int bitmask, boolean b) {
        if (b) {
            return bitmask | Modifier.STRICT;
        } else {
            return bitmask & ~Modifier.STRICT;
        }
    }

    private static int toBridge(int bitmask, boolean b) {
        // Bridge re-uses the Modifier.VOLATILE modifier, which used to only
        // apply to fields.
        if (b) {
            return (bitmask | Modifier.VOLATILE) &
                (~Modifier.NATIVE & ~Modifier.INTERFACE & ~Modifier.ABSTRACT);
        } else {
            return bitmask & ~Modifier.VOLATILE;
        }
    }

    private static int toEnum(int bitmask, boolean b) {
        // Enum re-uses the Modifier.NATIVE modifier, which used to only apply
        // to methods.
        if (b) {
            return (bitmask | Modifier.NATIVE) &
                (~Modifier.ABSTRACT & ~Modifier.INTERFACE &
                 ~Modifier.STRICT & ~Modifier.SYNCHRONIZED);
        } else {
            return bitmask & ~Modifier.NATIVE;
        }
    }

    private static int toVarArgs(int bitmask, boolean b) {
        // Enum re-uses the Modifier.TRANSIENT modifier, which used to only
        // apply to fields.
        if (b) {
            return (bitmask | Modifier.TRANSIENT) &
                (~Modifier.INTERFACE & ~Modifier.VOLATILE);
        } else {
            return bitmask & ~Modifier.TRANSIENT;
        }
    }

    private final int mBitmask;
    
    private Modifiers(int bitmask) {
        mBitmask = bitmask;
    }

    /**
     * Returns the bitmask.
     */
    public final int getBitmask() {
        return mBitmask;
    }
    
    public boolean isPublic() {
        return Modifier.isPublic(mBitmask);
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(mBitmask);
    }

    public boolean isProtected() {
        return Modifier.isProtected(mBitmask);
    }
    
    public boolean isStatic() {
        return Modifier.isStatic(mBitmask);
    }

    public boolean isFinal() {
        return Modifier.isFinal(mBitmask);
    }

    public boolean isSynchronized() {
        return Modifier.isSynchronized(mBitmask);
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(mBitmask);
    }

    public boolean isTransient() {
        return Modifier.isTransient(mBitmask);
    }
    
    public boolean isNative() {
        return Modifier.isNative(mBitmask);
    }
    
    public boolean isInterface() {
        return Modifier.isInterface(mBitmask);
    }
    
    public boolean isAbstract() {
        return Modifier.isAbstract(mBitmask);
    }

    public boolean isStrict() {
        return Modifier.isStrict(mBitmask);
    }

    public boolean isBridge() {
        return Modifier.isVolatile(mBitmask);
    }

    public boolean isEnum() {
        return Modifier.isNative(mBitmask);
    }

    public boolean isVarArgs() {
        return Modifier.isTransient(mBitmask);
    }

    /**
     * When set public, the bitmask is cleared from being private or protected.
     *
     * @param b true to set public, false otherwise
     */
    public Modifiers toPublic(boolean b) {
        return convert(toPublic(mBitmask, b));
    }
    
    /**
     * When set private, the bitmask is cleared from being public or protected.
     *
     * @param b true to set private, false otherwise
     */
    public Modifiers toPrivate(boolean b) {
        return convert(toPrivate(mBitmask, b));
    }

    /**
     * When set protected, the bitmask is cleared from being public or private.
     *
     * @param b true to set protected, false otherwise
     */
    public Modifiers toProtected(boolean b) {
        return convert(toProtected(mBitmask, b));
    }

    /**
     * @param b true to set static, false otherwise
     */
    public Modifiers toStatic(boolean b) {
        return convert(toStatic(mBitmask, b));
    }

    /**
     * When set final, the bitmask is cleared from being an interface or
     * abstract.
     *
     * @param b true to set final, false otherwise
     */
    public Modifiers toFinal(boolean b) {
        return convert(toFinal(mBitmask, b));
    }

    /**
     * When set synchronized, non-method settings are cleared.
     *
     * @param b true to set synchronized, false otherwise
     */
    public Modifiers toSynchronized(boolean b) {
        return convert(toSynchronized(mBitmask, b));
    }

    /**
     * When set volatile, non-field settings are cleared.
     *
     * @param b true to set volatile, false otherwise
     */
    public Modifiers toVolatile(boolean b) {
        return convert(toVolatile(mBitmask, b));
    }

    /**
     * When set transient, non-field settings are cleared.
     *
     * @param b true to set transient, false otherwise
     */
    public Modifiers toTransient(boolean b) {
        return convert(toTransient(mBitmask, b));
    }

    /**
     * When set native, non-native-method settings are cleared.
     *
     * @param b true to set native, false otherwise
     */
    public Modifiers toNative(boolean b) {
        return convert(toNative(mBitmask, b));
    }

    /**
     * When set as an interface, non-interface settings are cleared and the
     * bitmask is set abstract.
     *
     * @param b true to set interface, false otherwise
     */
    public Modifiers toInterface(boolean b) {
        return convert(toInterface(mBitmask, b));
    }

    /**
     * When set abstract, the bitmask is cleared from being final, volatile,
     * transient, native, synchronized, and strictfp. When cleared from being
     * abstract, the bitmask is also cleared from being an interface.
     *
     * @param b true to set abstract, false otherwise
     */
    public Modifiers toAbstract(boolean b) {
        return convert(toAbstract(mBitmask, b));
    }

    /**
     * @param b true to set strictfp, false otherwise
     */
    public Modifiers toStrict(boolean b) {
        return convert(toStrict(mBitmask, b));
    }

    /**
     * Used to identify if a method is a bridge method.
     *
     * @param b true to set bridge, false otherwise
     */
    public Modifiers toBridge(boolean b) {
        return convert(toBridge(mBitmask, b));
    }

    /**
     * Used to identify if a field is an enum constant.
     *
     * @param b true to set enum, false otherwise
     */
    public Modifiers toEnum(boolean b) {
        return convert(toEnum(mBitmask, b));
    }

    /**
     * Used to identify if a method accepts a variable amount of
     * arguments.
     *
     * @param b true to set varargs, false otherwise
     */
    public Modifiers toVarArgs(boolean b) {
        return convert(toVarArgs(mBitmask, b));
    }

    public int hashCode() {
        return mBitmask;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Modifiers) {
            Modifiers other = (Modifiers)obj;
            return mBitmask == other.mBitmask;
        }
        return false;
    }

    /**
     * Returns the string value generated by the Modifier class.
     *
     * @see java.lang.reflect.Modifier#toString()
     */
    public String toString() {
        return Modifier.toString(mBitmask);
    }

    private Modifiers convert(int bitmask) {
        return bitmask == mBitmask ? this : getInstance(bitmask);
    }
}
