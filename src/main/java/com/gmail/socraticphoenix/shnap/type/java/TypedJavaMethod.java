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

package com.gmail.socraticphoenix.shnap.type.java;

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.collect.coupling.Triple;
import com.gmail.socraticphoenix.mirror.Reflections;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapJavaBackedNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TypedJavaMethod {
    private Class owner;
    private Method method;
    private List<Class> params;
    private Class retVal;

    public TypedJavaMethod(Class owner, Method method) {
        this.method = method;
        this.owner = owner;
        this.params = Items.buildList(method.getParameterTypes());
        this.retVal = Reflections.boxingType(method.getReturnType());
    }

    public ShnapExecution execute(ShnapJavaBackedNative owner, List<Object> resolved, ShnapEnvironment environment) {
        ShnapLoc loc = ShnapJavaInterface.createJavaLoc(this.owner, this.method);

        try {
            Object result = this.method.invoke(owner == null ? null : owner.getJavaBacker(), resolved.toArray());
            if (this.retVal.equals(Void.class)) {
                return ShnapExecution.returning(ShnapObject.getVoid(), environment, loc);
            } else {
                return ShnapExecution.returning(ShnapJavaInterface.createObject(result), environment, loc);
            }
        } catch (Throwable e) {
            return ShnapExecution.throwing(ShnapFactory.mimicJavaException("shnap.JavaInterfaceError", "Failed to invoke java method: " + this.format(), e), environment, loc);
        }
    }

    public String format() {
        StringBuilder k = new StringBuilder(this.owner.getName() + "#" + this.method.getName() + "(");
        for (int i = 0; i < this.params.size(); i++) {
            k.append(this.params.get(i).getName());
            if (i < this.params.size() - 1) {
                k.append(", ");
            }
        }
        return k.append(")").toString();
    }

    public TypedJavaMethod resolveConflict(TypedJavaMethod other, List<Class> params) {
        List<Class> myParams = this.params;
        List<Class> otherParams = other.params;
        if (myParams.size() != otherParams.size() || params.size() != myParams.size()) {
            return params.size() == myParams.size() ? this : params.size() == otherParams.size() ? other : null;
        } else {
            List<Triple<Class, Class, Class>> conflicts = new ArrayList<>();
            for (int i = 0; i < myParams.size(); i++) {
                if (params.get(i) != null) {
                    Class myParam = Reflections.boxingType(myParams.get(i));
                    Class otherParam = Reflections.boxingType(otherParams.get(i));
                    if (!myParam.equals(otherParam) && (myParam.isAssignableFrom(otherParam) || otherParam.isAssignableFrom(myParam))) {
                        conflicts.add(Triple.of(params.get(i), myParam, otherParam));
                    }
                } else if (myParams.get(i).isPrimitive()) {
                    return other; //Can't put null in a primitive, ever
                }
            }

            int leftTotal = 0;
            int rightTotal = 0;

            for (Triple<Class, Class, Class> cls : conflicts) {
                leftTotal += distance(cls.getA(), cls.getB());
                rightTotal += distance(cls.getA(), cls.getC());

            }

            return leftTotal >= rightTotal ? this : other;
        }
    }

    public boolean conflictsWith(TypedJavaMethod other) {
        return this.matches(other.params);
    }

    public boolean matches(List<Class> input) {
        if (input.size() != this.params.size()) {
            return false;
        }

        for (int i = 0; i < this.params.size(); i++) {
            Class param = input.get(i);
            if ((param != null && !Reflections.boxingType(this.params.get(i)).isAssignableFrom(Reflections.boxingType(param))) || (param == null && this.params.get(i).isPrimitive())) {
                return false;
            }
        }

        return true;
    }

    public static int distance(Class a, Class b) {
        int dst = oneDirectionalDistance(a, b);
        if (dst == -1) {
            return oneDirectionalDistance(b, a);
        } else {
            return dst;
        }
    }

    public static int oneDirectionalDistance(Class child, Class parent) {
        if (child == null) {
            return -1;
        } else if (child.isInterface() && parent.equals(Object.class)) {
            return 1; //Special case for interface distance to Object, since EVERYTHING is instanceof Object...
        }

        int def = parent.isAssignableFrom(child) ? 0 : -1;

        if (def == -1) {
            return def;
        }

        if (child.equals(parent)) {
            return 0;
        } else {
            int dst = distance(child.getSuperclass(), parent);
            if (dst == -1) {
                for (Class inter : child.getInterfaces()) {
                    int dst2 = distance(inter, parent);
                    if (dst2 != -1) {
                        return dst2 + 1;
                    }
                }
                return def;
            } else {
                return dst + 1;
            }
        }
    }

}
