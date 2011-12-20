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

package org.cojen.util;

import java.util.Arrays;

/**
 * KeyFactory generates keys which can be hashed or compared for any kind of
 * object including arrays, arrays of arrays, and null. All hashcode
 * computations, equality tests, and ordering comparsisons fully recurse into
 * arrays.
 *
 * @author Brian S O'Neill
 */
public class KeyFactory {
    static final Object NULL = new Comparable() {
        public int compareTo(Object obj) {
            return obj == this || obj == null ? 0 : 1;
        }
    };

    public static Object createKey(boolean[] obj) {
        return obj == null ? NULL : new BooleanArrayKey(obj);
    }

    public static Object createKey(byte[] obj) {
        return obj == null ? NULL : new ByteArrayKey(obj);
    }

    public static Object createKey(char[] obj) {
        return obj == null ? NULL : new CharArrayKey(obj);
    }

    public static Object createKey(double[] obj) {
        return obj == null ? NULL : new DoubleArrayKey(obj);
    }

    public static Object createKey(float[] obj) {
        return obj == null ? NULL : new FloatArrayKey(obj);
    }

    public static Object createKey(int[] obj) {
        return obj == null ? NULL : new IntArrayKey(obj);
    }

    public static Object createKey(long[] obj) {
        return obj == null ? NULL : new LongArrayKey(obj);
    }

    public static Object createKey(short[] obj) {
        return obj == null ? NULL : new ShortArrayKey(obj);
    }

    public static Object createKey(Object[] obj) {
        return obj == null ? NULL : new ObjectArrayKey(obj);
    }

    public static Object createKey(Object obj) {
        if (obj == null) {
            return NULL;
        }
        if (!obj.getClass().isArray()) {
            return obj;
        }
        if (obj instanceof Object[]) {
            return createKey((Object[])obj);
        } else if (obj instanceof int[]) {
            return createKey((int[])obj);
        } else if (obj instanceof float[]) {
            return createKey((float[])obj);
        } else if (obj instanceof long[]) {
            return createKey((long[])obj);
        } else if (obj instanceof double[]) {
            return createKey((double[])obj);
        } else if (obj instanceof byte[]) {
            return createKey((byte[])obj);
        } else if (obj instanceof char[]) {
            return createKey((char[])obj);
        } else if (obj instanceof boolean[]) {
            return createKey((boolean[])obj);
        } else if (obj instanceof short[]) {
            return createKey((short[])obj);
        } else {
            return obj;
        }
    }

    static int hashCode(boolean[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + (a[i] ? 0 : 1);
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(byte[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(char[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(double[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            long v = Double.doubleToLongBits(a[i]);
            hash = hash * 31 + (int)(v ^ v >>> 32);
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(float[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = hash * 31 + Float.floatToIntBits(a[i]);
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(int[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(long[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            long v = a[i];
            hash = hash * 31 + (int)(v ^ v >>> 32);
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(short[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int hashCode(Object[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = hash * 31 + hashCode(a[i]);
        }
        return hash == 0 ? -1 : hash;
    }

    // Compute object or array hashcode and recurses into arrays within.
    static int hashCode(Object a) {
        if (a == null) {
            return -1;
        }
        if (!a.getClass().isArray()) {
            return a.hashCode();
        }
        if (a instanceof Object[]) {
            return hashCode((Object[])a);
        } else if (a instanceof int[]) {
            return hashCode((int[])a);
        } else if (a instanceof float[]) {
            return hashCode((float[])a);
        } else if (a instanceof long[]) {
            return hashCode((long[])a);
        } else if (a instanceof double[]) {
            return hashCode((double[])a);
        } else if (a instanceof byte[]) {
            return hashCode((byte[])a);
        } else if (a instanceof char[]) {
            return hashCode((char[])a);
        } else if (a instanceof boolean[]) {
            return hashCode((boolean[])a);
        } else if (a instanceof short[]) {
            return hashCode((short[])a);
        } else {
            int hash = a.getClass().hashCode();
            return hash == 0 ? -1 : hash;
        }
    }

    // Compares object arrays and recurses into arrays within.
    static boolean equals(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        int i;
        if ((i = a.length) != b.length) {
            return false;
        }
        while (--i >= 0) {
            if (!equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    // Compares objects or arrays and recurses into arrays within.
    static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Class ac = a.getClass();
        if (!(ac.isArray())) {
            return a.equals(b);
        }
        if (ac != b.getClass()) {
            return false;
        }
        if (a instanceof Object[]) {
            return equals((Object[])a, (Object[])b);
        } else if (a instanceof int[]) {
            return Arrays.equals((int[])a, (int[])b);
        } else if (a instanceof float[]) {
            return Arrays.equals((float[])a, (float[])b);
        } else if (a instanceof long[]) {
            return Arrays.equals((long[])a, (long[])b);
        } else if (a instanceof double[]) {
            return Arrays.equals((double[])a, (double[])b);
        } else if (a instanceof byte[]) {
            return Arrays.equals((byte[])a, (byte[])b);
        } else if (a instanceof char[]) {
            return Arrays.equals((char[])a, (char[])b);
        } else if (a instanceof boolean[]) {
            return Arrays.equals((boolean[])a, (boolean[])b);
        } else if (a instanceof short[]) {
            return Arrays.equals((short[])a, (short[])b);
        } else {
            return a.equals(b);
        }
    }

    static int compare(boolean[] a, boolean[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int av = a[i] ? 0 : 1;
            int bv = b[i] ? 0 : 1;
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            byte av = a[i];
            byte bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(char[] a, char[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            char av = a[i];
            char bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(double[] a, double[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = Double.compare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(float[] a, float[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = Float.compare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(int[] a, int[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int av = a[i];
            int bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(long[] a, long[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            long av = a[i];
            long bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int compare(short[] a, short[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            short av = a[i];
            short bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    // Compares object arrays and recurses into arrays within.
    static int compare(Object[] a, Object[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = compare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    // Compares objects or arrays and recurses into arrays within.
    static int compare(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        Class ac = a.getClass();
        if (!(ac.isArray())) {
            return ((Comparable)a).compareTo(b);
        }
        if (ac != b.getClass()) {
            throw new ClassCastException();
        }
        if (a instanceof Object[]) {
            return compare((Object[])a, (Object[])b);
        } else if (a instanceof int[]) {
            return compare((int[])a, (int[])b);
        } else if (a instanceof float[]) {
            return compare((float[])a, (float[])b);
        } else if (a instanceof long[]) {
            return compare((long[])a, (long[])b);
        } else if (a instanceof double[]) {
            return compare((double[])a, (double[])b);
        } else if (a instanceof byte[]) {
            return compare((byte[])a, (byte[])b);
        } else if (a instanceof char[]) {
            return compare((char[])a, (char[])b);
        } else if (a instanceof boolean[]) {
            return compare((boolean[])a, (boolean[])b);
        } else if (a instanceof short[]) {
            return compare((short[])a, (short[])b);
        } else {
            throw new ClassCastException();
        }
    }

    protected KeyFactory() {
    }

    private static interface ArrayKey extends Comparable, java.io.Serializable {
        int hashCode();

        boolean equals(Object obj);

        int compareTo(Object obj);
    }

    private static class BooleanArrayKey implements ArrayKey {
        protected final boolean[] mArray;
        private transient int mHash;

        BooleanArrayKey(boolean[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof BooleanArrayKey ?
                 Arrays.equals(mArray, ((BooleanArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((BooleanArrayKey) obj).mArray);
        }
    }

    private static class ByteArrayKey implements ArrayKey {
        protected final byte[] mArray;
        private transient int mHash;

        ByteArrayKey(byte[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ByteArrayKey ?
                 Arrays.equals(mArray, ((ByteArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((ByteArrayKey) obj).mArray);
        }
    }

    private static class CharArrayKey implements ArrayKey {
        protected final char[] mArray;
        private transient int mHash;

        CharArrayKey(char[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof CharArrayKey ?
                 Arrays.equals(mArray, ((CharArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((CharArrayKey) obj).mArray);
        }
    }

    private static class DoubleArrayKey implements ArrayKey {
        protected final double[] mArray;
        private transient int mHash;

        DoubleArrayKey(double[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof DoubleArrayKey ?
                 Arrays.equals(mArray, ((DoubleArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((DoubleArrayKey) obj).mArray);
        }
    }

    private static class FloatArrayKey implements ArrayKey {
        protected final float[] mArray;
        private transient int mHash;

        FloatArrayKey(float[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof FloatArrayKey ?
                 Arrays.equals(mArray, ((FloatArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((FloatArrayKey) obj).mArray);
        }
    }

    private static class IntArrayKey implements ArrayKey {
        protected final int[] mArray;
        private transient int mHash;

        IntArrayKey(int[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof IntArrayKey ?
                 Arrays.equals(mArray, ((IntArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((IntArrayKey) obj).mArray);
        }
    }

    private static class LongArrayKey implements ArrayKey {
        protected final long[] mArray;
        private transient int mHash;

        LongArrayKey(long[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof LongArrayKey ?
                 Arrays.equals(mArray, ((LongArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((LongArrayKey) obj).mArray);
        }
    }

    private static class ShortArrayKey implements ArrayKey {
        protected final short[] mArray;
        private transient int mHash;

        ShortArrayKey(short[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ShortArrayKey ?
                 Arrays.equals(mArray, ((ShortArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((ShortArrayKey) obj).mArray);
        }
    }

    private static class ObjectArrayKey implements ArrayKey {
        protected final Object[] mArray;
        private transient int mHash;

        ObjectArrayKey(Object[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = KeyFactory.hashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ObjectArrayKey ?
                 KeyFactory.equals(mArray, ((ObjectArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return compare(mArray, ((ObjectArrayKey) obj).mArray);
        }
    }
}
