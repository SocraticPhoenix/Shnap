/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 socraticphoenix@gmail.com
 * Copyright (c) 2017 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gmail.socraticphoenix.shnap.util;

import com.gmail.socraticphoenix.collect.coupling.Switch;
import com.gmail.socraticphoenix.mirror.Reflections;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.java.ShnapJavaInterface;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapJavaBackedNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Map;

public class DeepArrays {

    public static ShnapExecution deeplyConvertToShnap(Object array, ShnapEnvironment environment) {
        Map<Object, Reference> seen = new IdentityHashMap<>();
        Reference initial = new Reference(null);
        seen.put(array, initial);

        Switch<Reference[], ShnapExecution> result = deeplyConvertToShnap(array, environment, seen);
        if (result.containsB()) {
            return result.getB().get();
        } else {
            Reference[] theArray = result.getA().get();
            initial.object = theArray;
            return ShnapExecution.normal(transformShnapReferences(theArray, new IdentityHashMap<>()), environment, ShnapLoc.BUILTIN);
        }
    }

    public static ShnapExecution deeplyConvertToJava(ShnapArrayNative arrayNative, ShnapEnvironment environment, Class component) {
        Map<ShnapObject, Reference> seen = new IdentityHashMap<>();
        Reference initial = new Reference(null);
        seen.put(arrayNative, initial);

        Switch<Reference[], ShnapExecution> result = deeplyConvertToJava(arrayNative, environment, seen);
        if (result.containsB()) {
            return result.getB().get();
        } else {
            Reference[] theArray = result.getA().get();
            initial.object = theArray;
            return ShnapExecution.normal(ShnapJavaInterface.createObject(transformReferences(theArray, component, new IdentityHashMap<>())), environment, ShnapLoc.BUILTIN);
        }

    }

    private static ShnapObject transformShnapReferences(Reference[] array, Map<Object, ShnapObject> seen) {
        ShnapObject[] res = new ShnapObject[array.length];
        ShnapArrayNative val = new ShnapArrayNative(ShnapLoc.BUILTIN, res);
        seen.put(array, val);
        for (int i = 0; i < array.length; i++) {
            Reference reference = array[i];
            if (reference.object instanceof Reference[]) {
                Reference[] object = (Reference[]) reference.object;
                if (seen.containsKey(object)) {
                    res[i] = seen.get(object);
                } else {
                    res[i] = transformShnapReferences(object, seen);
                }
            } else {
                res[i] = (ShnapObject) reference.object;
            }
        }

        return val;
    }

    private static Object transformReferences(Reference[] array, Class component, Map<Object, Object> seen) {
        Object[] res = new Object[array.length];
        seen.put(array, res);
        for (int i = 0; i < array.length; i++) {
            Reference reference = array[i];
            if (reference.object instanceof Reference[]) {
                Reference[] object = (Reference[]) reference.object;
                if (seen.containsKey(object)) {
                    res[i] = seen.get(object);
                } else {
                    res[i] = transformReferences(object, component, seen);
                }
            } else {
                res[i] = reference.object;
            }
        }

        Object actual = Array.newInstance(component, res.length);
        Class boxed = Reflections.boxingType(component);
        for (int i = 0; i < res.length; i++) {
            Object k = res[i];
            if (k == res) {
                Array.set(actual, i, actual);
            } else {

                if (canCastNumber(k, component)) {
                    k = castNumber(k, component);
                } else {
                    k = boxed.cast(k);
                }

                Array.set(actual, i, k);
            }
        }

        return actual;
    }

    private static Switch<Reference[], ShnapExecution> deeplyConvertToShnap(Object array, ShnapEnvironment environment, Map<Object, Reference> seen) {
        int len = Array.getLength(array);
        Reference[] working = new Reference[len];
        for (int i = 0; i < len; i++) {
            Object theObject = Array.get(array, i);
            if (seen.containsKey(theObject)) {
                working[i] = seen.get(theObject);
            } else if (theObject != null && theObject.getClass().isArray()) {
                Reference value = new Reference(null);
                seen.put(theObject, value);
                working[i] = value;
                Switch<Reference[], ShnapExecution> result = deeplyConvertToShnap(theObject, environment, seen);
                if (result.containsB()) {
                    return result;
                } else {
                    value.object = result.getA().get();
                }
            } else {
                Object object = ShnapJavaInterface.revert(theObject);
                Reference reference = new Reference(object);
                working[i] = reference;
                seen.put(theObject, reference);
            }
        }

        return Switch.ofA(working);
    }

    private static Switch<Reference[], ShnapExecution> deeplyConvertToJava(ShnapArrayNative shnapArrayNative, ShnapEnvironment environment, Map<ShnapObject, Reference> seen) {
        ShnapObject[] values = shnapArrayNative.getValue();
        Reference[] working = new Reference[values.length];
        for (int i = 0; i < values.length; i++) {
            ShnapObject theObject = values[i];
            if (seen.containsKey(theObject)) {
                working[i] = seen.get(theObject);
            } else {
                ShnapExecution asJava = theObject.asJava(environment);
                if (asJava.isAbnormal()) {
                    ShnapExecution asArray = theObject.asArray(environment);
                    if (asArray.isAbnormal()) {
                        return Switch.ofB(asJava);
                    }

                    ShnapArrayNative theArray = (ShnapArrayNative) asArray.getValue();
                    Reference value = new Reference(null);
                    seen.put(theObject, value);
                    working[i] = value;
                    Switch<Reference[], ShnapExecution> result = deeplyConvertToJava(theArray, environment, seen);
                    if (result.containsB()) {
                        return result;
                    } else {
                        value.object = result.getA().get();
                    }
                } else {
                    Object value = ((ShnapJavaBackedNative) asJava.getValue()).getJavaBacker();
                    Reference reference = new Reference(value);
                    working[i] = reference;
                    seen.put(theObject, reference);
                }
            }
        }

        return Switch.ofA(working);
    }

    private static Object castNumber(Object val, Class target) {
        if (target.isInstance(val)) {
            return val;
        } else if (val instanceof Number) {
            Number number = (Number) val;
            if (target == Long.class) {
                return number.longValue();
            } else if (target == Integer.class) {
                return number.intValue();
            } else if (target == Short.class) {
                return number.shortValue();
            } else if (target == Byte.class) {
                return number.byteValue();
            } else if (target == Double.class) {
                return number.doubleValue();
            } else if (target == Float.class) {
                return number.floatValue();
            } else if (target == BigDecimal.class) {
                if (number instanceof BigInteger) {
                    return new BigDecimal((BigInteger) number);
                } else {
                    return number.doubleValue() - (double) ((int) number.doubleValue()) == 0.0D ? BigDecimal.valueOf(number.longValue()) : BigDecimal.valueOf(number.doubleValue());
                }
            } else if (target == BigInteger.class) {
                return number instanceof BigDecimal ? ((BigDecimal) number).toBigInteger() : BigInteger.valueOf(number.longValue());
            } else {
                return target == Boolean.class ? number.intValue() > 0 : number;
            }
        } else {
            Boolean bool = (Boolean) val;
            return castNumber(bool.booleanValue() ? 1 : 0, target);
        }
    }

    private static boolean canCastNumber(Object a, Class target) {
        return a instanceof Number && Number.class.isAssignableFrom(target) || a instanceof Boolean && Number.class.isAssignableFrom(target) || a instanceof Number && Boolean.class.isAssignableFrom(target) || a instanceof Boolean && Boolean.class.isAssignableFrom(target);
    }

    private static class Reference {
        public Object object;

        public Reference(Object object) {
            this.object = object;
        }

    }

}
