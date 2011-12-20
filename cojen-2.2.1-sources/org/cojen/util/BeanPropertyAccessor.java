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

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.math.BigInteger;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.Opcode;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;

/**
 * Provides a simple and efficient means of reading and writing bean
 * properties. BeanPropertyAccessor auto-generates code, eliminating the
 * need to invoke methods via reflection. Bean access methods are bound-to
 * directly, using a special hash/switch design pattern.
 *
 * @author Brian S O'Neill
 * @see BeanPropertyMapFactory
 */
public abstract class BeanPropertyAccessor<B> {
    public static enum PropertySet {
        /** Set of all properties */
        ALL,
        /** Set of all properties which declare only unchecked exceptions */
        UNCHECKED_EXCEPTIONS,
        /** Set of all read-write properties */
        READ_WRITE,
        /** Set of all read-write properties which declare only unchecked exceptions */
        READ_WRITE_UNCHECKED_EXCEPTIONS,
    }

    private static final int READ_METHOD = 1;
    private static final int WRITE_METHOD = 2;
    private static final int TRY_READ_METHOD = 3;
    private static final int TRY_WRITE_METHOD = 4;
    private static final int HAS_READ_METHOD = 5;
    private static final int HAS_WRITE_METHOD = 6;

    private static final
        Map<PropertySet, Map<Class, SoftReference<BeanPropertyAccessor>>> cAccessors =
        new HashMap<PropertySet, Map<Class, SoftReference<BeanPropertyAccessor>>>();

    /**
     * Returns a new or cached BeanPropertyAccessor for the given class.
     */
    public static <B> BeanPropertyAccessor<B> forClass(Class<B> clazz) {
        return forClass(clazz, PropertySet.ALL);
    }

    public static <B> BeanPropertyAccessor<B> forClass(Class<B> clazz, PropertySet set) {
        synchronized (cAccessors) {
            Map<Class, SoftReference<BeanPropertyAccessor>> accessors = cAccessors.get(set);
            if (accessors == null) {
                accessors = new WeakIdentityMap<Class, SoftReference<BeanPropertyAccessor>>();
                cAccessors.put(set, accessors);
            }

            BeanPropertyAccessor bpa;
            SoftReference<BeanPropertyAccessor> ref = accessors.get(clazz);
            if (ref != null) {
                bpa = ref.get();
                if (bpa != null) {
                    return bpa;
                }
            }
            bpa = generate(clazz, set);
            accessors.put(clazz, new SoftReference<BeanPropertyAccessor>(bpa));
            return bpa;
        }
    }

    private static <B> BeanPropertyAccessor<B> generate(final Class<B> beanType,
                                                        final PropertySet set)
    {
        return AccessController.doPrivileged(new PrivilegedAction<BeanPropertyAccessor<B>>() {
            public BeanPropertyAccessor<B> run() {
                Class clazz = generateClassFile(beanType, set).defineClass();
                try {
                    return (BeanPropertyAccessor<B>) clazz.newInstance();
                } catch (InstantiationException e) {
                    throw new InternalError(e.toString());
                } catch (IllegalAccessException e) {
                    throw new InternalError(e.toString());
                }
            }
        });
    }

    private static RuntimeClassFile generateClassFile(Class beanType, PropertySet set) {
        BeanProperty[][] props = getBeanProperties(beanType, set);

        RuntimeClassFile cf = new RuntimeClassFile
            (BeanPropertyAccessor.class.getName(),
             BeanPropertyAccessor.class.getName(),
             beanType.getClassLoader());
        cf.markSynthetic();
        cf.setSourceFile(BeanPropertyAccessor.class.getName());
        try {
            cf.setTarget(System.getProperty("java.specification.version"));
        } catch (Exception e) {
        }

        MethodInfo ctor = cf.addConstructor(Modifiers.PUBLIC, null);
        ctor.markSynthetic();
        CodeBuilder b = new CodeBuilder(ctor);

        b.loadThis();
        b.invokeSuperConstructor(null);
        b.returnVoid();

        generateAccessMethod(cf, beanType, props[0], READ_METHOD);
        generateAccessMethod(cf, beanType, props[0], TRY_READ_METHOD);
        generateAccessMethod(cf, beanType, props[0], HAS_READ_METHOD);
        generateAccessMethod(cf, beanType, props[1], WRITE_METHOD);
        generateAccessMethod(cf, beanType, props[1], TRY_WRITE_METHOD);
        generateAccessMethod(cf, beanType, props[1], HAS_WRITE_METHOD);

        generateSearchMethod(cf, beanType, props[0]);

        return cf;
    }

    private static void generateAccessMethod(ClassFile cf,
                                             Class beanType,
                                             BeanProperty[] properties,
                                             int methodType)
    {
        MethodInfo mi;
        switch (methodType) {
        case READ_METHOD: default: {
            TypeDesc[] params = {TypeDesc.OBJECT, TypeDesc.STRING};
            mi = cf.addMethod
                (Modifiers.PUBLIC, "getPropertyValue", TypeDesc.OBJECT, params);
            break;
        }
        case WRITE_METHOD: {
            TypeDesc[] params = new TypeDesc[] {
                TypeDesc.OBJECT, TypeDesc.STRING, TypeDesc.OBJECT
            };
            mi = cf.addMethod(Modifiers.PUBLIC, "setPropertyValue", null, params);
            break;
        }
        case TRY_READ_METHOD: {
            TypeDesc[] params = {TypeDesc.OBJECT, TypeDesc.STRING};
            mi = cf.addMethod
                (Modifiers.PUBLIC, "tryGetPropertyValue", TypeDesc.OBJECT, params);
            break;
        }
        case TRY_WRITE_METHOD: {
            TypeDesc[] params = new TypeDesc[] {
                TypeDesc.OBJECT, TypeDesc.STRING, TypeDesc.OBJECT
            };
            mi = cf.addMethod(Modifiers.PUBLIC, "trySetPropertyValue", TypeDesc.BOOLEAN, params);
            break;
        }
        case HAS_READ_METHOD: {
            TypeDesc[] params = {TypeDesc.STRING};
            mi = cf.addMethod(Modifiers.PUBLIC, "hasReadableProperty", TypeDesc.BOOLEAN, params);
            break;
        }
        case HAS_WRITE_METHOD: {
            TypeDesc[] params = {TypeDesc.STRING};
            mi = cf.addMethod(Modifiers.PUBLIC, "hasWritableProperty", TypeDesc.BOOLEAN, params);
            break;
        }
        }

        mi.markSynthetic();
        CodeBuilder b = new CodeBuilder(mi);

        LocalVariable beanVar, propertyVar, valueVar;

        switch (methodType) {
        case READ_METHOD: case TRY_READ_METHOD: default:
            beanVar = b.getParameter(0);
            propertyVar = b.getParameter(1);
            valueVar = null;
            break;
        case WRITE_METHOD: case TRY_WRITE_METHOD:
            beanVar = b.getParameter(0);
            propertyVar = b.getParameter(1);
            valueVar = b.getParameter(2);
            break;
        case HAS_READ_METHOD: case HAS_WRITE_METHOD:
            beanVar = null;
            propertyVar = b.getParameter(0);
            valueVar = null;
            break;
        }

        if (beanVar != null) {
            b.loadLocal(beanVar);
            b.checkCast(TypeDesc.forClass(beanType));
            b.storeLocal(beanVar);
        }

        if (properties.length > 0) {
            int[] cases = new int[hashCapacity(properties.length)];
            int caseCount = cases.length;
            for (int i=0; i<caseCount; i++) {
                cases[i] = i;
            }

            Label[] switchLabels = new Label[caseCount];
            Label noMatch = b.createLabel();
            List[] caseMethods = caseMethods(caseCount, properties);
            
            for (int i=0; i<caseCount; i++) {
                List matches = caseMethods[i];
                if (matches == null || matches.size() == 0) {
                    switchLabels[i] = noMatch;
                } else {
                    switchLabels[i] = b.createLabel();
                }
            }

            if (properties.length > 1) {
                b.loadLocal(propertyVar);
                b.invokeVirtual(String.class.getName(), "hashCode", TypeDesc.INT, null);
                b.loadConstant(0x7fffffff);
                b.math(Opcode.IAND);
                b.loadConstant(caseCount);
                b.math(Opcode.IREM);
            
                b.switchBranch(cases, switchLabels, noMatch);
            }
            
            // Params to invoke String.equals.
            TypeDesc[] params = {TypeDesc.OBJECT};
            
            for (int i=0; i<caseCount; i++) {
                List matches = caseMethods[i];
                if (matches == null || matches.size() == 0) {
                    continue;
                }
                
                switchLabels[i].setLocation();
                
                int matchCount = matches.size();
                for (int j=0; j<matchCount; j++) {
                    BeanProperty bp = (BeanProperty)matches.get(j);
                    
                    // Test against name to find exact match.
                    
                    b.loadConstant(bp.getName());
                    b.loadLocal(propertyVar);
                    b.invokeVirtual(String.class.getName(), "equals", TypeDesc.BOOLEAN, params);
                    
                    Label notEqual;
                    
                    if (j == matchCount - 1) {
                        notEqual = null;
                        b.ifZeroComparisonBranch(noMatch, "==");
                    } else {
                        notEqual = b.createLabel();
                        b.ifZeroComparisonBranch(notEqual, "==");
                    }
                    
                    switch (methodType) {
                    case READ_METHOD: case TRY_READ_METHOD: default: {
                        b.loadLocal(beanVar);
                        b.invoke(bp.getReadMethod());
                        TypeDesc type = TypeDesc.forClass(bp.getType());
                        b.convert(type, type.toObjectType());
                        b.returnValue(TypeDesc.OBJECT);
                        break;
                    }
                    case WRITE_METHOD: case TRY_WRITE_METHOD: {
                        b.loadLocal(beanVar);
                        b.loadLocal(valueVar);
                        TypeDesc type = TypeDesc.forClass(bp.getType());
                        b.checkCast(type.toObjectType());
                        b.convert(type.toObjectType(), type);
                        b.invoke(bp.getWriteMethod());
                        if (methodType == WRITE_METHOD) {
                            b.returnVoid();
                        } else {
                            b.loadConstant(true);
                            b.returnValue(TypeDesc.BOOLEAN);
                        }
                        break;
                    }
                    case HAS_READ_METHOD: case HAS_WRITE_METHOD: {
                        b.loadConstant(true);
                        b.returnValue(TypeDesc.BOOLEAN);
                        break;
                    }
                    }
                    
                    if (notEqual != null) {
                        notEqual.setLocation();
                    }
                }
            }
            
            noMatch.setLocation();
        }

        if (methodType == HAS_READ_METHOD || methodType == HAS_WRITE_METHOD
            || methodType == TRY_WRITE_METHOD)
        {
            b.loadConstant(false);
            b.returnValue(TypeDesc.BOOLEAN);
        } else if (methodType == TRY_READ_METHOD) {
            b.loadNull();
            b.returnValue(TypeDesc.OBJECT);
        } else {
            b.newObject(TypeDesc.forClass(NoSuchPropertyException.class));
            b.dup();
            b.loadLocal(propertyVar);
            b.loadConstant(methodType == READ_METHOD);

            // Params to invoke NoSuchPropertyException.<init>.
            TypeDesc[] params = {TypeDesc.STRING, TypeDesc.BOOLEAN};

            b.invokeConstructor(NoSuchPropertyException.class.getName(), params);
            b.throwObject();
        }
    }

    /**
     * Returns a prime number, at least twice as large as needed. This should
     * minimize hash collisions. Since all the hash keys are known up front,
     * the capacity could be tweaked until there are no collisions, but this
     * technique is easier and deterministic.
     */
    private static int hashCapacity(int min) {
        BigInteger capacity = BigInteger.valueOf(min * 2 + 1);
        while (!capacity.isProbablePrime(10)) {
            capacity = capacity.add(BigInteger.valueOf(2));
        }
        return capacity.intValue();
    }

    /**
     * Returns an array of Lists of BeanProperties. The first index
     * matches a switch case, the second index provides a list of all the
     * BeanProperties whose name hash matched on the case.
     */
    private static List[] caseMethods(int caseCount,
                                      BeanProperty[] props) {
        List[] cases = new List[caseCount];

        for (int i=0; i<props.length; i++) {
            BeanProperty prop = props[i];
            int hashCode = prop.getName().hashCode();
            int caseValue = (hashCode & 0x7fffffff) % caseCount;
            List matches = cases[caseValue];
            if (matches == null) {
                matches = cases[caseValue] = new ArrayList();
            }
            matches.add(prop);
        }

        return cases;
    }

    private static void generateSearchMethod(ClassFile cf,
                                             Class beanType,
                                             BeanProperty[] properties)
    {
        MethodInfo mi;
        {
            TypeDesc[] params = {TypeDesc.OBJECT, TypeDesc.OBJECT};
            mi = cf.addMethod(Modifiers.PUBLIC, "hasPropertyValue", TypeDesc.BOOLEAN, params);
        }

        mi.markSynthetic();
        CodeBuilder b = new CodeBuilder(mi);

        LocalVariable beanVar = b.getParameter(0);
        b.loadLocal(beanVar);
        b.checkCast(TypeDesc.forClass(beanType));
        b.storeLocal(beanVar);

        LocalVariable valueVar = b.getParameter(1);

        // If search value is null, only check properties which might be null.
        b.loadLocal(valueVar);
        Label searchNotNull = b.createLabel();
        b.ifNullBranch(searchNotNull, false);

        for (BeanProperty bp : properties) {
            if (bp.getType().isPrimitive()) {
                continue;
            }

            b.loadLocal(beanVar);
            b.invoke(bp.getReadMethod());

            Label noMatch = b.createLabel();
            b.ifNullBranch(noMatch, false);
            b.loadConstant(true);
            b.returnValue(TypeDesc.BOOLEAN);

            noMatch.setLocation();
        }

        b.loadConstant(false);
        b.returnValue(TypeDesc.BOOLEAN);

        searchNotNull.setLocation();

        // Handle search for non-null value. Search non-primitive properties
        // first, to avoid object conversion.

        // Params to invoke Object.equals.
        TypeDesc[] params = {TypeDesc.OBJECT};

        for (int pass = 1; pass <= 2; pass++) {
            for (BeanProperty bp : properties) {
                boolean primitive = bp.getType().isPrimitive();
                if (pass == 1 && primitive) {
                    continue;
                } else if (pass == 2 && !primitive) {
                    continue;
                }

                b.loadLocal(valueVar);
                b.loadLocal(beanVar);
                b.invoke(bp.getReadMethod());
                b.convert(TypeDesc.forClass(bp.getType()), TypeDesc.OBJECT);
                b.invokeVirtual(Object.class.getName(), "equals", TypeDesc.BOOLEAN, params);

                Label noMatch = b.createLabel();
                b.ifZeroComparisonBranch(noMatch, "==");
                b.loadConstant(true);
                b.returnValue(TypeDesc.BOOLEAN);

                noMatch.setLocation();
            }
        }

        b.loadConstant(false);
        b.returnValue(TypeDesc.BOOLEAN);
    }

    /**
     * Returns two arrays of BeanProperties. Array 0 contains read
     * BeanProperties, array 1 contains the write BeanProperties.
     */
    private static BeanProperty[][] getBeanProperties(Class beanType, PropertySet set) {
        List readProperties = new ArrayList();
        List writeProperties = new ArrayList();

        Map map = BeanIntrospector.getAllProperties(beanType);

        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            BeanProperty bp = (BeanProperty)it.next();

            if (set == PropertySet.READ_WRITE ||
                set == PropertySet.READ_WRITE_UNCHECKED_EXCEPTIONS)
            {
                if (bp.getReadMethod() == null || bp.getWriteMethod() == null) {
                    continue;
                }
            }
 
            boolean checkedAllowed = 
                set != PropertySet.UNCHECKED_EXCEPTIONS &&
                set != PropertySet.READ_WRITE_UNCHECKED_EXCEPTIONS;

            if (bp.getReadMethod() != null) {
                if (checkedAllowed || !throwsCheckedException(bp.getReadMethod())) {
                    readProperties.add(bp);
                }
            }
            if (bp.getWriteMethod() != null) {
                if (checkedAllowed || !throwsCheckedException(bp.getWriteMethod())) {
                    writeProperties.add(bp);
                }
            }
        }

        BeanProperty[][] props = new BeanProperty[2][];
        
        props[0] = new BeanProperty[readProperties.size()];
        readProperties.toArray(props[0]);
        props[1] = new BeanProperty[writeProperties.size()];
        writeProperties.toArray(props[1]);

        return props;
    }

    static boolean throwsCheckedException(Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        if (exceptionTypes == null) {
            return false;
        }

        for (Class<?> exceptionType : exceptionTypes) {
            if (RuntimeException.class.isAssignableFrom(exceptionType)) {
                continue;
            }
            if (Error.class.isAssignableFrom(exceptionType)) {
                continue;
            }
            return true;
        }

        return false;
    }

    protected BeanPropertyAccessor() {
    }

    // The actual public methods that will need to be defined.

    public abstract Object getPropertyValue(B bean, String property)
        throws NoSuchPropertyException;

    public abstract void setPropertyValue(B bean, String property, Object value)
        throws NoSuchPropertyException;

    /**
     * Returns true if readable bean property exists.
     *
     * @since 2.1
     */
    public abstract boolean hasReadableProperty(String property);

    /**
     * Returns true if writable bean property exists.
     *
     * @since 2.1
     */
    public abstract boolean hasWritableProperty(String property);

    /**
     * Returns true if at least one property is set to the given value.
     *
     * @since 2.1
     */
    public abstract boolean hasPropertyValue(B bean, Object value);

    /**
     * Returns property value or null if it does not exist.
     *
     * @since 2.1
     */
    public abstract Object tryGetPropertyValue(B bean, String property);

    /**
     * Tries to set property value, if it exists.
     *
     * @return false if property doesn't exist
     * @since 2.1
     */
    public abstract boolean trySetPropertyValue(B bean, String property, Object value);

    // Auto-generated code sample:
    /*
    public Object getPropertyValue(Object bean, String property) {
        Bean bean = (Bean)bean;
        
        switch ((property.hashCode() & 0x7fffffff) % 11) {
        case 0:
            if ("name".equals(property)) {
                return bean.getName();
            }
            break;
        case 1:
            // No case
            break;
        case 2:
            // Hash collision
            if ("value".equals(property)) {
                return bean.getValue();
            } else if ("age".equals(property)) {
                return new Integer(bean.getAge());
            }
            break;
        case 3:
            if ("start".equals(property)) {
                return bean.getStart();
            }
            break;
        case 4:
        case 5:
        case 6:
            // No case
            break;
        case 7:
            if ("end".equals(property)) {
                return bean.isEnd() ? Boolean.TRUE : Boolean.FALSE;
            }
            break;
        case 8:
        case 9:
        case 10:
            // No case
            break;
        }
        
        throw new NoSuchPropertyException(property, true);
    }

    public void setPropertyValue(Object bean, String property, Object value) {
        Bean bean = (Bean)bean;
        
        switch ((property.hashCode() & 0x7fffffff) % 11) {
        case 0:
            if ("name".equals(property)) {
                bean.setName(value);
            }
            break;
        case 1:
            // No case
            break;
        case 2:
            // Hash collision
            if ("value".equals(property)) {
                bean.setValue(value);
            } else if ("age".equals(property)) {
                bean.setAge(((Integer)value).intValue());
            }
            break;
        case 3:
            if ("start".equals(property)) {
                bean.setStart(value);
            }
            break;
        case 4:
        case 5:
        case 6:
            // No case
            break;
        case 7:
            if ("end".equals(property)) {
                bean.setEnd(((Boolean)value).booleanValue());
            }
            break;
        case 8:
        case 9:
        case 10:
            // No case
            break;
        }
        
        throw new NoSuchPropertyException(property, false);
    }
    */
}
