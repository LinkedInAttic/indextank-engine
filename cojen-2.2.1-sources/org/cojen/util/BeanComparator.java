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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.Opcode;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;

/**
 * A highly customizable, high-performance Comparator, designed specifically
 * for advanced sorting of JavaBeans. BeanComparators contain dynamically
 * auto-generated code and perform as well as hand written Comparators.
 * <p>
 * BeanComparator instances are immutable; order customization methods
 * return new BeanComparators with refined rules. Calls to customizers can
 * be chained together to read like a formula. The following example produces
 * a Comparator that orders Threads by name, thread group name, and reverse
 * priority.
 *
 * <pre>
 * Comparator c = BeanComparator.forClass(Thread.class)
 *     .orderBy("name")
 *     .orderBy("threadGroup.name")
 *     .orderBy("-priority");
 * </pre>
 *
 * The results of sorting Threads using this Comparator and displaying the
 * results in a table may look like this:
 *
 * <p><table border="2">
 * <tr><th>name</th><th>threadGroup.name</th><th>priority</th></tr>
 * <tr><td>daemon</td><td>appGroup</td><td>9</td></tr>
 * <tr><td>main</td><td>main</td><td>5</td></tr>
 * <tr><td>main</td><td>secureGroup</td><td>5</td></tr>
 * <tr><td>sweeper</td><td>main</td><td>1</td></tr>
 * <tr><td>Thread-0</td><td>main</td><td>5</td></tr>
 * <tr><td>Thread-1</td><td>main</td><td>5</td></tr>
 * <tr><td>worker</td><td>appGroup</td><td>8</td></tr>
 * <tr><td>worker</td><td>appGroup</td><td>5</td></tr>
 * <tr><td>worker</td><td>secureGroup</td><td>8</td></tr>
 * <tr><td>worker</td><td>secureGroup</td><td>5</td></tr>
 * </table><p>
 *
 * An equivalent Thread ordering Comparator may be specified as:
 *
 * <pre>
 * Comparator c = BeanComparator.forClass(Thread.class)
 *     .orderBy("name")
 *     .orderBy("threadGroup")
 *     .using(BeanComparator.forClass(ThreadGroup.class).orderBy("name"))
 *     .orderBy("priority")
 *     .reverse();
 * </pre>
 *
 * The current implementation of BeanComparator has been optimized for fast
 * construction and execution of BeanComparators. For maximum performance,
 * however, save and re-use BeanComparators wherever possible.
 * <p>
 * Even though BeanComparator makes use of auto-generated code, instances are
 * fully Serializable, as long as all passed in Comparators are also
 * Serializable.
 *
 * @author Brian S O'Neill
 */
public class BeanComparator<T> implements Comparator<T>, Serializable {
    // Maps Rules to auto-generated Comparators.
    private static Map cGeneratedComparatorCache;

    static {
        cGeneratedComparatorCache = new SoftValuedHashMap();
    }

    /**
     * Get or create a new BeanComparator for beans of the given type. Without
     * any {@link #orderBy order-by} properties specified, the returned
     * BeanComparator can only order against null beans (null is 
     * {@link #nullHigh high} by default), and treats all other comparisons as
     * equal.
     */
    public static <T> BeanComparator<T> forClass(Class<T> clazz) {
        return new BeanComparator<T>(clazz);
    }

    /**
     * Compare two objects for equality.
     */
    private static boolean equalTest(Object obj1, Object obj2) {
        return (obj1 == obj2) ? true :
            ((obj1 == null || obj2 == null) ? false : obj1.equals(obj2));
    }

    /**
     * Compare two object classes for equality.
     */
    /*
    private static boolean equalClassTest(Object obj1, Object obj2) {
        return (obj1 == obj2) ? true :
            ((obj1 == null || obj2 == null) ? false :
             obj1.getClass().equals(obj2.getClass()));
    }
    */

    private Class<T> mBeanClass;

    // Maps property names to PropertyDescriptors.
    private transient Map<String, BeanProperty> mProperties;

    private String mOrderByName;

    private Comparator<?> mUsingComparator;

    // bit 0: reverse
    // bit 1: null low order
    // bit 2: use String compareTo instead of collator
    private int mFlags;

    // Used for comparing strings.
    private Comparator<String> mCollator;

    private BeanComparator<T> mParent;

    // Auto-generated internal Comparator.
    private transient Comparator<T> mComparator;

    private transient boolean mHasHashCode;
    private transient int mHashCode;

    private BeanComparator(Class<T> clazz) {
        mBeanClass = clazz;
        mCollator = String.CASE_INSENSITIVE_ORDER;
    }

    private BeanComparator(BeanComparator<T> parent) {
        mParent = parent;
        mBeanClass = parent.mBeanClass;
        mProperties = parent.getProperties();
        mCollator = parent.mCollator;
    }

    /**
     * Add an order-by property to produce a more refined Comparator. If the
     * property does not return a {@link Comparable} object when
     * {@link #compare compare} is called on the returned comparator, the
     * property is ignored. Call {@link #using using} on the returned
     * BeanComparator to specify a Comparator to use for this property instead.
     * <p>
     * The specified propery name may refer to sub-properties using a dot
     * notation. For example, if the bean being compared contains a property
     * named "info" of type "Information", and "Information" contains a
     * property named "text", then ordering by the info text can be specified
     * by "info.text". Sub-properties of sub-properties may be refered to as
     * well, a.b.c.d.e etc.
     * <p>
     * If property type is a primitive, ordering is the same as for its
     * Comparable object peer. Primitive booleans are ordered false low, true
     * high. Floating point primitves are ordered exactly the same way as
     * {@link Float#compareTo(Float) Float.compareTo} and
     * {@link Double#compareTo(Double) Double.compareTo}.
     * <p>
     * As a convenience, property names may have a '-' or '+' character prefix
     * to specify sort order. A prefix of '-' indicates that the property
     * is to be sorted in reverse (descending). By default, properties are
     * sorted in ascending order, and so a prefix of '+' has no effect.
     * <p>
     * Any previously applied {@link #reverse reverse-order}, {@link #nullHigh
     * null-order} and {@link #caseSensitive case-sensitive} settings are not
     * carried over, and are reset to the defaults for this order-by property.
     *
     * @throws IllegalArgumentException when property doesn't exist or cannot
     * be read.
     */
    public BeanComparator<T> orderBy(String propertyName)
        throws IllegalArgumentException
    {
        int dot = propertyName.indexOf('.');
        String subName;
        if (dot < 0) {
            subName = null;
        } else {
            subName = propertyName.substring(dot + 1);
            propertyName = propertyName.substring(0, dot);
        }

        boolean reverse = false;
        if (propertyName.length() > 0) {
            char prefix = propertyName.charAt(0);
            switch (prefix) {
            default:
                break;
            case '-':
                reverse = true;
                // Fall through
            case '+':
                propertyName = propertyName.substring(1);
            }
        }

        BeanProperty prop = getProperties().get(propertyName);

        if (prop == null) {
            throw new IllegalArgumentException
                ("Property '" + propertyName + "' doesn't exist in '" +
                 mBeanClass.getName() + '\'');
        }

        if (prop.getReadMethod() == null) {
            throw new IllegalArgumentException
                ("Property '" + propertyName + "' cannot be read");
        }

        if (propertyName.equals(mOrderByName)) {
            // Make String unique so that properties can be specified in
            // consecutive order-by calls without being eliminated by
            // reduceRules. A secondary order-by may wish to further refine an
            // ambiguous comparison using a Comparator.
            propertyName = new String(propertyName);
        }

        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = propertyName;

        if (subName != null) {
            BeanComparator<?> subOrder = forClass(prop.getType());
            subOrder.mCollator = mCollator;
            bc = bc.using(subOrder.orderBy(subName));
        }

        return reverse ? bc.reverse() : bc;
    }

    /**
     * Specifiy a Comparator to use on just the last {@link #orderBy order-by}
     * property. This is good for comparing properties that are not
     * {@link Comparable} or for applying special ordering rules for a
     * property. If no order-by properties have been specified, then Comparator
     * is applied to the compared beans.
     * <p>
     * Any previously applied String {@link #caseSensitive case-sensitive} or
     * {@link #collate collator} settings are overridden by this Comparator.
     * If property values being compared are primitive, they are converted to
     * their object peers before being passed to the Comparator.
     *
     * @param c Comparator to use on the last order-by property. Passing null
     * restores the default comparison for the last order-by property.
     */
    public <S> BeanComparator<T> using(Comparator<S> c) {
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = c;
        bc.mFlags = mFlags;
        return bc;
    }

    /**
     * Toggle reverse-order option on just the last {@link #orderBy order-by}
     * property. By default, order is ascending. If no order-by properties have
     * been specified, then reverse order is applied to the compared beans.
     */
    public BeanComparator<T> reverse() {
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = mUsingComparator;
        bc.mFlags = mFlags ^ 0x01;
        return bc;
    }

    /**
     * Set the order of comparisons against null as being high (the default)
     * on just the last {@link #orderBy order-by} property. If no order-by
     * properties have been specified, then null high order is applied to the
     * compared beans. Null high order is the default for consistency with the
     * high ordering of {@link Float#NaN NaN} by
     * {@link Float#compareTo(Float) Float}.
     * <p>
     * Calling 'nullHigh, reverse' is equivalent to calling 'reverse, nullLow'.
     */
    public BeanComparator<T> nullHigh() {
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = mUsingComparator;
        bc.mFlags = mFlags ^ ((mFlags & 0x01) << 1);
        return bc;
    }

    /**
     * Set the order of comparisons against null as being low
     * on just the last {@link #orderBy order-by} property. If no order-by
     * properties have been specified, then null low order is applied to the
     * compared beans.
     * <p>
     * Calling 'reverse, nullLow' is equivalent to calling 'nullHigh, reverse'.
     */
    public BeanComparator<T> nullLow() {
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = mUsingComparator;
        bc.mFlags = mFlags ^ ((~mFlags & 0x01) << 1);
        return bc;
    }

    /**
     * Override the collator and compare just the last order-by property using
     * {@link String#compareTo(String) String.compareTo}, if it is of type
     * String. If no order-by properties have been specified then this call is
     * ineffective.
     * <p>
     * A {@link #using using} Comparator disables this setting. Passing null to
     * the using method will re-enable a case-sensitive setting.
     */
    public BeanComparator<T> caseSensitive() {
        if ((mFlags & 0x04) != 0) {
            // Already case-sensitive.
            return this;
        }
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = mUsingComparator;
        bc.mFlags = mFlags | 0x04;
        return bc;
    }

    /**
     * Set a Comparator for ordering Strings, which is passed on to all
     * BeanComparators derived from this one. By default, String are compared
     * using {@link String#CASE_INSENSITIVE_ORDER}. Passing null for a collator
     * will cause all String comparisons to use
     * {@link String#compareTo(String) String.compareTo}.
     * <p>
     * A {@link #using using} Comparator disables this setting. Passing null
     * to the using method will re-enable a collator.
     *
     * @param c Comparator to use for ordering all Strings. Passing null
     * causes all Strings to be ordered by
     * {@link String#compareTo(String) String.compareTo}.
     */
    public BeanComparator<T> collate(Comparator<String> c) {
        BeanComparator<T> bc = new BeanComparator<T>(this);
        bc.mOrderByName = mOrderByName;
        bc.mUsingComparator = mUsingComparator;
        bc.mFlags = mFlags & ~0x04;
        bc.mCollator = c;
        return bc;
    }

    public int compare(T obj1, T obj2) throws ClassCastException {
        Comparator<T> c = mComparator;
        if (c == null) {
            mComparator = c = AccessController.doPrivileged(new PrivilegedAction<Comparator<T>>() {
                public Comparator<T> run() {
                    return generateComparator();
                }
            });
        }
        return c.compare(obj1, obj2);
    }

    public int hashCode() {
        if (!mHasHashCode) {
            setHashCode(new Rules(this));
        }
        return mHashCode;
    }

    private void setHashCode(Rules rules) {
        mHashCode = rules.hashCode();
        mHasHashCode = true;
    }

    /**
     * Compares BeanComparators for equality based on their imposed ordering.
     * Returns true only if the given object is a BeanComparater and it can be
     * determined without a doubt that the ordering is identical. Because
     * equality testing is dependent on the behavior of the equals methods of
     * any 'using' Comparators and/or collators, false may be returned even
     * though ordering is in fact identical.
     */
    public boolean equals(Object obj) {
        if (obj instanceof BeanComparator) {
            BeanComparator bc = (BeanComparator)obj;
 
            return mFlags == bc.mFlags &&
                equalTest(mBeanClass, bc.mBeanClass) &&
                equalTest(mOrderByName, bc.mOrderByName) &&
                equalTest(mUsingComparator, bc.mUsingComparator) &&
                equalTest(mCollator, bc.mCollator) &&
                equalTest(mParent, bc.mParent);
        } else {
            return false;
        }
    }

    private Map<String, BeanProperty> getProperties() {
        if (mProperties == null) {
            mProperties = BeanIntrospector.getAllProperties(mBeanClass);
        }
        return mProperties;
    }

    private Comparator<T> generateComparator() {
        Rules rules = new Rules(this);

        if (!mHasHashCode) {
            setHashCode(rules);
        }

        Class clazz;

        synchronized (cGeneratedComparatorCache) {
            Object c = cGeneratedComparatorCache.get(rules);

            if (c == null) {
                clazz = generateComparatorClass(rules);
                cGeneratedComparatorCache.put(rules, clazz);
            } else if (c instanceof Comparator) {
                return (Comparator)c;
            } else {
                clazz = (Class)c;
            }

            BeanComparator[] ruleParts = rules.getRuleParts();
            Comparator[] collators = new Comparator[ruleParts.length];
            Comparator[] usingComparators = new Comparator[ruleParts.length];
            boolean singleton = true;

            for (int i=0; i<ruleParts.length; i++) {
                BeanComparator rp = ruleParts[i];
                Comparator c2 = rp.mCollator;
                if ((collators[i] = c2) != null) {
                    if (c2 != String.CASE_INSENSITIVE_ORDER) {
                        singleton = false;
                    }
                }
                if ((usingComparators[i] = rp.mUsingComparator) != null) {
                    singleton = false;
                }
            }

            try {
                Constructor ctor = clazz.getDeclaredConstructor
                    (new Class[] {Comparator[].class, Comparator[].class});
                c = (Comparator)ctor.newInstance
                    (new Object[] {collators, usingComparators});
            } catch (NoSuchMethodException e) {
                throw new InternalError(e.toString());
            } catch (InstantiationException e) {
                throw new InternalError(e.toString());
            } catch (IllegalAccessException e) {
                throw new InternalError(e.toString());
            } catch (IllegalArgumentException e) {
                throw new InternalError(e.toString());
            } catch (InvocationTargetException e) {
                throw new InternalError(e.getTargetException().toString());
            }

            if (singleton) {
                // Can save and re-use instance since it obeys the requirements
                // for a singleton.
                cGeneratedComparatorCache.put(rules, c);
            }

            return (Comparator<T>)c;
        }
    }

    private Class generateComparatorClass(Rules rules) {
        RuntimeClassFile cf = new RuntimeClassFile
            (getClass().getName(), null, mBeanClass.getClassLoader());
        cf.markSynthetic();
        cf.setSourceFile(BeanComparator.class.getName());
        try {
            cf.setTarget(System.getProperty("java.specification.version"));
        } catch (Exception e) {
        }

        cf.addInterface(Comparator.class);
        cf.addInterface(Serializable.class);

        // Define fields to hold usage comparator and collator.
        TypeDesc comparatorType = TypeDesc.forClass(Comparator.class);
        TypeDesc comparatorArrayType = comparatorType.toArrayType();
        cf.addField(Modifiers.PRIVATE,
                    "mCollators", comparatorArrayType).markSynthetic();
        cf.addField(Modifiers.PRIVATE,
                    "mUsingComparators", comparatorArrayType).markSynthetic();

        // Create constructor to initialize fields.
        TypeDesc[] paramTypes = {
            comparatorArrayType, comparatorArrayType
        };
        MethodInfo ctor = cf.addConstructor(Modifiers.PUBLIC, paramTypes);
        ctor.markSynthetic();
        CodeBuilder builder = new CodeBuilder(ctor);

        builder.loadThis();
        builder.invokeSuperConstructor(null);
        builder.loadThis();
        builder.loadLocal(builder.getParameter(0));
        builder.storeField("mCollators", comparatorArrayType);
        builder.loadThis();
        builder.loadLocal(builder.getParameter(1));
        builder.storeField("mUsingComparators", comparatorArrayType);
        builder.returnVoid();

        // Create the all-important compare method.
        Method compareMethod, compareToMethod;
        try {
            compareMethod = Comparator.class.getMethod
                ("compare", new Class[] {Object.class, Object.class});
            compareToMethod = Comparable.class.getMethod
                ("compareTo", new Class[] {Object.class});
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }

        MethodInfo mi = cf.addMethod(compareMethod);
        mi.markSynthetic();
        builder = new CodeBuilder(mi);

        Label endLabel = builder.createLabel();
        LocalVariable obj1 = builder.getParameter(0);
        LocalVariable obj2 = builder.getParameter(1);

        // The first rule always applies to the beans directly. All others
        // apply to properties.

        BeanComparator[] ruleParts = rules.getRuleParts();
        BeanComparator bc = ruleParts[0];

        if ((bc.mFlags & 0x01) != 0) {
            // Reverse beans.
            LocalVariable temp = obj1;
            obj1 = obj2;
            obj2 = temp;
        }

        // Handle the case when obj1 and obj2 are the same (or both null)
        builder.loadLocal(obj1);
        builder.loadLocal(obj2);
        builder.ifEqualBranch(endLabel, true);

        // Do null order checks for beans.
        boolean nullHigh = (bc.mFlags & 0x02) == 0;
        Label label = builder.createLabel();
        builder.loadLocal(obj1);
        builder.ifNullBranch(label, false);
        builder.loadConstant(nullHigh ? 1 : -1);
        builder.returnValue(TypeDesc.INT);
        label.setLocation();
        label = builder.createLabel();
        builder.loadLocal(obj2);
        builder.ifNullBranch(label, false);
        builder.loadConstant(nullHigh ? -1 : 1);
        builder.returnValue(TypeDesc.INT);
        label.setLocation();

        // Call 'using' Comparator if one is provided.
        LocalVariable result =
            builder.createLocalVariable("result", TypeDesc.INT);
        if (bc.mUsingComparator != null) {
            builder.loadThis();
            builder.loadField("mUsingComparators", comparatorArrayType);
            builder.loadConstant(0);
            builder.loadFromArray(TypeDesc.forClass(Comparator.class));
            builder.loadLocal(obj1);
            builder.loadLocal(obj2);
            builder.invoke(compareMethod);
            builder.storeLocal(result);
            builder.loadLocal(result);
            label = builder.createLabel();
            builder.ifZeroComparisonBranch(label, "==");
            builder.loadLocal(result);
            builder.returnValue(TypeDesc.INT);
            label.setLocation();
        }

        // Cast bean parameters to correct types so that properties may be
        // accessed.
        TypeDesc type = TypeDesc.forClass(bc.mBeanClass);
        builder.loadLocal(obj1);
        builder.checkCast(type);
        builder.storeLocal(obj1);
        builder.loadLocal(obj2);
        builder.checkCast(type);
        builder.storeLocal(obj2);

        // Generate code to perform comparisons against each property.
        for (int i=1; i<ruleParts.length; i++) {
            bc = ruleParts[i];

            BeanProperty prop = 
                (BeanProperty)bc.getProperties().get(bc.mOrderByName);
            Class propertyClass = prop.getType();
            TypeDesc propertyType = TypeDesc.forClass(propertyClass);

            // Create local variable to hold property values.
            LocalVariable p1 = builder.createLocalVariable("p1", propertyType);
            LocalVariable p2 = builder.createLocalVariable("p2", propertyType);

            // Access properties and store in local variables.
            builder.loadLocal(obj1);
            builder.invoke(prop.getReadMethod());
            builder.storeLocal(p1);
            builder.loadLocal(obj2);
            builder.invoke(prop.getReadMethod());
            builder.storeLocal(p2);

            if ((bc.mFlags & 0x01) != 0) {
                // Reverse properties.
                LocalVariable temp = p1;
                p1 = p2;
                p2 = temp;
            }

            Label nextLabel = builder.createLabel();

            // Handle the case when p1 and p2 are the same (or both null)
            if (!propertyClass.isPrimitive()) {
                builder.loadLocal(p1);
                builder.loadLocal(p2);
                builder.ifEqualBranch(nextLabel, true);

                // Do null order checks for properties.
                nullHigh = (bc.mFlags & 0x02) == 0;
                label = builder.createLabel();
                builder.loadLocal(p1);
                builder.ifNullBranch(label, false);
                builder.loadConstant(nullHigh ? 1 : -1);
                builder.returnValue(TypeDesc.INT);
                label.setLocation();
                label = builder.createLabel();
                builder.loadLocal(p2);
                builder.ifNullBranch(label, false);
                builder.loadConstant(nullHigh ? -1 : 1);
                builder.returnValue(TypeDesc.INT);
                label.setLocation();
            }

            // Call 'using' Comparator if one is provided, else assume
            // Comparable.
            if (bc.mUsingComparator != null) {
                builder.loadThis();
                builder.loadField("mUsingComparators", comparatorArrayType);
                builder.loadConstant(i);
                builder.loadFromArray(TypeDesc.forClass(Comparator.class));
                builder.loadLocal(p1);
                builder.convert(propertyType, propertyType.toObjectType());
                builder.loadLocal(p2);
                builder.convert(propertyType, propertyType.toObjectType());
                builder.invoke(compareMethod);
            } else {
                // If case-sensitive is off and a collator is provided and
                // property could be a String, apply collator.
                if ((bc.mFlags & 0x04) == 0 && bc.mCollator != null &&
                    propertyClass.isAssignableFrom(String.class)) {

                    Label resultLabel = builder.createLabel();

                    if (!String.class.isAssignableFrom(propertyClass)) {
                        // Check if both property values are strings at
                        // runtime. If they aren't, cast to Comparable and call
                        // compareTo.

                        TypeDesc stringType = TypeDesc.STRING;

                        builder.loadLocal(p1);
                        builder.instanceOf(stringType);
                        Label notString = builder.createLabel();
                        builder.ifZeroComparisonBranch(notString, "==");
                        builder.loadLocal(p2);
                        builder.instanceOf(stringType);
                        Label isString = builder.createLabel();
                        builder.ifZeroComparisonBranch(isString, "!=");

                        notString.setLocation();
                        generateComparableCompareTo
                            (builder, propertyClass, compareToMethod,
                             resultLabel, nextLabel, p1, p2);

                        isString.setLocation();
                    }

                    builder.loadThis();
                    builder.loadField("mCollators", comparatorArrayType);
                    builder.loadConstant(i);
                    builder.loadFromArray(TypeDesc.forClass(Comparator.class));
                    builder.loadLocal(p1);
                    builder.loadLocal(p2);
                    builder.invoke(compareMethod);

                    resultLabel.setLocation();
                } else if (propertyClass.isPrimitive()) {
                    generatePrimitiveComparison(builder, propertyClass, p1,p2);
                } else {
                    // Assume properties are instances of Comparable.
                    generateComparableCompareTo
                        (builder, propertyClass, compareToMethod,
                         null, nextLabel, p1, p2);
                }
            }

            if (i < (ruleParts.length - 1)) {
                builder.storeLocal(result);
                builder.loadLocal(result);
                builder.ifZeroComparisonBranch(nextLabel, "==");
                builder.loadLocal(result);
            }
            builder.returnValue(TypeDesc.INT);

            // The next property comparison will start here.
            nextLabel.setLocation();
        }

        endLabel.setLocation();
        builder.loadConstant(0);
        builder.returnValue(TypeDesc.INT);

        return cf.defineClass();
    }

    private static void generatePrimitiveComparison(CodeBuilder builder,
                                                    Class type,
                                                    LocalVariable a,
                                                    LocalVariable b)
    {
        if (type == float.class) {
            // Comparison is same as for Float.compareTo(Float).
            Label done = builder.createLabel();

            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.math(Opcode.FCMPG);
            Label label = builder.createLabel();
            builder.ifZeroComparisonBranch(label, ">=");
            builder.loadConstant(-1);
            builder.branch(done);

            label.setLocation();
            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.math(Opcode.FCMPL);
            label = builder.createLabel();
            builder.ifZeroComparisonBranch(label, "<=");
            builder.loadConstant(1);
            builder.branch(done);

            Method floatToIntBits;
            try {
                floatToIntBits = Float.class.getMethod
                    ("floatToIntBits", new Class[] {float.class});
            } catch (NoSuchMethodException e) {
                throw new InternalError(e.toString());
            }

            label.setLocation();
            builder.loadLocal(a);
            builder.invoke(floatToIntBits);
            builder.convert(TypeDesc.INT, TypeDesc.LONG);
            builder.loadLocal(b);
            builder.invoke(floatToIntBits);
            builder.convert(TypeDesc.INT, TypeDesc.LONG);
            builder.math(Opcode.LCMP);

            done.setLocation();
        } else if (type == double.class) {
            // Comparison is same as for Double.compareTo(Double).
            Label done = builder.createLabel();

            builder.loadLocal(a);
            builder.loadLocal(b);
            done = builder.createLabel();
            builder.math(Opcode.DCMPG);
            Label label = builder.createLabel();
            builder.ifZeroComparisonBranch(label, ">=");
            builder.loadConstant(-1);
            builder.branch(done);

            label.setLocation();
            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.math(Opcode.DCMPL);
            label = builder.createLabel();
            builder.ifZeroComparisonBranch(label, "<=");
            builder.loadConstant(1);
            builder.branch(done);

            Method doubleToLongBits;
            try {
                doubleToLongBits = Double.class.getMethod
                    ("doubleToLongBits", new Class[] {double.class});
            } catch (NoSuchMethodException e) {
                throw new InternalError(e.toString());
            }

            label.setLocation();
            builder.loadLocal(a);
            builder.invoke(doubleToLongBits);
            builder.loadLocal(b);
            builder.invoke(doubleToLongBits);
            builder.math(Opcode.LCMP);

            done.setLocation();
        } else if (type == long.class) {
            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.math(Opcode.LCMP);
        } else if (type == int.class) {
            builder.loadLocal(a);
            builder.convert(TypeDesc.INT, TypeDesc.LONG);
            builder.loadLocal(b);
            builder.convert(TypeDesc.INT, TypeDesc.LONG);
            builder.math(Opcode.LCMP);
        } else {
            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.math(Opcode.ISUB);
        }
    }

    private static void generateComparableCompareTo(CodeBuilder builder,
                                                    Class type,
                                                    Method compareToMethod,
                                                    Label goodLabel,
                                                    Label nextLabel,
                                                    LocalVariable a,
                                                    LocalVariable b)
    {
        if (Comparable.class.isAssignableFrom(type)) {
            builder.loadLocal(a);
            builder.loadLocal(b);
            builder.invoke(compareToMethod);
            if (goodLabel != null) {
                builder.branch(goodLabel);
            }
        } else {
            // Cast each property to Comparable only if needed.
            TypeDesc comparableType = TypeDesc.forClass(Comparable.class);

            boolean locateGoodLabel = false;
            if (goodLabel == null) {
                goodLabel = builder.createLabel();
                locateGoodLabel = true;
            }

            Label tryStart = builder.createLabel().setLocation();
            builder.loadLocal(a);
            builder.checkCast(comparableType);
            builder.loadLocal(b);
            builder.checkCast(comparableType);
            Label tryEnd = builder.createLabel().setLocation();
            builder.invoke(compareToMethod);
            builder.branch(goodLabel);

            builder.exceptionHandler(tryStart, tryEnd,
                                     ClassCastException.class.getName());
            // One of the properties is not Comparable, so just go to next.
            // Discard the exception.
            builder.pop();
            if (nextLabel == null) {
                builder.loadConstant(0);
            } else {
                builder.branch(nextLabel);
            }

            if (locateGoodLabel) {
                goodLabel.setLocation();
            }
        }
    }

    // A key that uniquely describes the rules of a BeanComparator.
    private static class Rules {
        private BeanComparator[] mRuleParts;
        private int mHashCode;

        public Rules(BeanComparator bc) {
            mRuleParts = reduceRules(bc);

            // Compute hashCode.
            int hash = 0;

            for (int i = mRuleParts.length - 1; i >= 0; i--) {
                bc = mRuleParts[i];
                hash = 31 * hash;

                hash += bc.mFlags << 4;

                Object obj = bc.mBeanClass;
                if (obj != null) {
                    hash += obj.hashCode();
                }
                obj = bc.mOrderByName;
                if (obj != null) {
                    hash += obj.hashCode();
                }
                obj = bc.mUsingComparator;
                if (obj != null) {
                    hash += obj.getClass().hashCode();
                }
                obj = bc.mCollator;
                if (obj != null) {
                    hash += obj.getClass().hashCode();
                }
            }

            mHashCode = hash;
        }

        public BeanComparator[] getRuleParts() {
            return mRuleParts;
        }

        public int hashCode() {
            return mHashCode;
        }

        /**
         * Equality test determines if rules produce an identical
         * auto-generated Comparator.
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof Rules)) {
                return false;
            }

            BeanComparator[] ruleParts1 = getRuleParts();
            BeanComparator[] ruleParts2 = ((Rules)obj).getRuleParts();

            if (ruleParts1.length != ruleParts2.length) {
                return false;
            }

            for (int i=0; i<ruleParts1.length; i++) {
                BeanComparator bc1 = ruleParts1[i];
                BeanComparator bc2 = ruleParts2[i];

                if (bc1.mFlags != bc2.mFlags) {
                    return false;
                }
                if (!equalTest(bc1.mBeanClass, bc2.mBeanClass)) {
                    return false;
                }
                if (!equalTest(bc1.mOrderByName, bc2.mOrderByName)) {
                    return false;
                }
                if ((bc1.mUsingComparator == null) !=
                    (bc2.mUsingComparator == null)) {
                    return false;
                }
                if ((bc1.mCollator == null) != (bc2.mCollator == null)) {
                    return false;
                }
            }

            return true;
        }

        private BeanComparator[] reduceRules(BeanComparator bc) {
            // Reduce the ordering rules by returning BeanComparators
            // that are at the end of the chain or before an order-by rule.
            List<BeanComparator> rules = new ArrayList<BeanComparator>();

            rules.add(bc);
            String name = bc.mOrderByName;

            while ((bc = bc.mParent) != null) {
                // Don't perform string comparison using equals method.
                if (name != bc.mOrderByName) {
                    rules.add(bc);
                    name = bc.mOrderByName;
                }
            }

            int size = rules.size();
            BeanComparator[] bcs = new BeanComparator[size];
            // Reverse rules so that they are in forward order.
            for (int i=0; i<size; i++) {
                bcs[size - i - 1] = rules.get(i);
            }

            return bcs;
        }
    }
}
