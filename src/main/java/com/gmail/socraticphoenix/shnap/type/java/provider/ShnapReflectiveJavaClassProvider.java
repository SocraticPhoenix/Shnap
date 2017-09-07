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

package com.gmail.socraticphoenix.shnap.type.java.provider;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.java.MultiJavaConstructor;
import com.gmail.socraticphoenix.shnap.type.java.MultiJavaMethod;
import com.gmail.socraticphoenix.shnap.type.java.ShnapJavaInterface;
import com.gmail.socraticphoenix.shnap.type.java.TypedJavaConstructor;
import com.gmail.socraticphoenix.shnap.type.java.TypedJavaMethod;
import com.gmail.socraticphoenix.shnap.type.java.shnap.ShnapJavaClass;
import com.gmail.socraticphoenix.shnap.type.java.shnap.ShnapJavaConstructor;
import com.gmail.socraticphoenix.shnap.type.java.shnap.ShnapJavaField;
import com.gmail.socraticphoenix.shnap.type.java.shnap.ShnapJavaMethod;
import com.gmail.socraticphoenix.shnap.type.java.shnap.ShnapJavaObject;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.DeepArrays;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;

public class ShnapReflectiveJavaClassProvider implements ShnapJavaClassProvider {
    public static final String STATIC_PREFIX = "static";
    public static final String METHOD_PREFIX = "method";
    public static final String FIELD_PREFIX = "field";

    private Class cls;

    private MultiJavaConstructor constructor;

    private Map<String, MultiJavaMethod> staticMethods;
    private Map<String, MultiJavaMethod> methods;

    private Map<String, Field> staticFields;
    private Map<String, Field> fields;

    private List<String> allShnapFields;

    public ShnapReflectiveJavaClassProvider(Class cls) {
        this.cls = cls;
        this.allShnapFields = new ArrayList<>();
        this.methods = new LinkedHashMap<>();
        this.staticMethods = new LinkedHashMap<>();
        this.fields = new LinkedHashMap<>();
        this.staticFields = new LinkedHashMap<>();

        this.constructor = new MultiJavaConstructor();
        for (Constructor constructor : cls.getConstructors()) {
            this.constructor.add(new TypedJavaConstructor(cls, constructor));
        }

        for (Method method : cls.getMethods()) {
            this.allShnapFields.add(formatMethodName(method));
            if (Modifier.isStatic(method.getModifiers())) {
                this.staticMethods.computeIfAbsent(formatMethodName(method), k -> new MultiJavaMethod()).add(new TypedJavaMethod(cls, method));
            } else {
                this.methods.computeIfAbsent(formatMethodName(method), k -> new MultiJavaMethod()).add(new TypedJavaMethod(cls, method));
            }
        }

        for (Field field : cls.getFields()) {
            this.allShnapFields.add(formatFieldName(field));
            if (Modifier.isStatic(field.getModifiers())) {
                this.staticFields.put(formatFieldName(field), field);
            } else {
                this.fields.put(formatFieldName(field), field);
            }
        }
    }

    public Class getCls() {
        return this.cls;
    }

    public Map<String, MultiJavaMethod> getStaticMethods() {
        return this.staticMethods;
    }

    public Map<String, MultiJavaMethod> getMethods() {
        return this.methods;
    }

    public Map<String, Field> getStaticFields() {
        return this.staticFields;
    }

    public Map<String, Field> getFields() {
        return this.fields;
    }

    public List<String> getAllShnapFields() {
        return this.allShnapFields;
    }

    public static String formatFieldName(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return STATIC_PREFIX + "_" + FIELD_PREFIX + "_" + field.getName();
        } else {
            return FIELD_PREFIX + "_" + field.getName();
        }
    }

    public static String formatMethodName(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return STATIC_PREFIX + "_" + METHOD_PREFIX + "_" + method.getName();
        } else {
            return METHOD_PREFIX + "_" + method.getName();
        }
    }

    @Override
    public ShnapJavaClass createClassObject() {
        ShnapLoc loc = ShnapJavaInterface.createJavaLoc(this.cls);
        ShnapJavaClass javaClass = new ShnapJavaClass(loc, this.cls);
        javaClass.set("class", ShnapFactory.noArg(ShnapFactory.instSimple(() -> ShnapJavaInterface.createObject(this.cls))));

        if(!this.constructor.isEmpty()) {
            javaClass.set("new", new ShnapJavaConstructor(loc, this.constructor));
        }
        this.staticFields.forEach((s, f) -> javaClass.set(s, new ShnapJavaField(loc, null, f)));
        this.staticMethods.forEach((s, m) -> javaClass.set(s, new ShnapJavaMethod(loc, m, null)));
        javaClass.set(ShnapObject.AS_STRING, oneArg(inst((ctx, trc) -> {
            try {
                return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, this.cls.toString()), trc, loc);
            } catch (Throwable e) {
                return ShnapExecution.throwing(ShnapFactory.mimicJavaException("shnap.JavaInterfaceError", "Failed to invoke java method: " + "Class#toString()", e), trc, loc);
            }
        })));
        return javaClass;
    }

    @Override
    public ShnapJavaObject createObject(Object object) {
        ShnapLoc loc = ShnapJavaInterface.createJavaLoc(this.cls);
        ShnapJavaObject javaObject = new ShnapJavaObject(loc, object);
        if(!this.constructor.isEmpty()) {
            javaObject.set("new", new ShnapJavaConstructor(loc, this.constructor));
        }
        this.staticFields.forEach((s, f) -> javaObject.set(s, new ShnapJavaField(loc, null, f)));
        this.staticMethods.forEach((s, m) -> javaObject.set(s, new ShnapJavaMethod(loc, m, null)));

        this.fields.forEach((s, f) -> javaObject.set(s, new ShnapJavaField(loc, javaObject, f)));
        this.methods.forEach((s, m) -> javaObject.set(s, new ShnapJavaMethod(loc, m, javaObject)));
        javaObject.set(ShnapObject.AS_STRING, noArg(inst((ctx, trc) -> {
            try {
                return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, String.valueOf(object)), trc, loc);
            } catch (Throwable e) {
                return ShnapExecution.throwing(ShnapFactory.mimicJavaException("shnap.JavaInterfaceError", "Failed to invoke java method: " + "Class#toString()", e), trc, loc);
            }
        })));
        if(object instanceof Number || object instanceof Character) {
            javaObject.set(ShnapObject.AS_NUMBER, noArg(instSimple(() -> object instanceof Character ? new ShnapCharNative(loc, (int) (Character) object) : ShnapNumberNative.valueOf(loc, (Number) object))));
        }
        if(object instanceof Boolean) {
            javaObject.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of((Boolean) object))));
        }
        if (object.getClass().isArray()) {
            javaObject.set(ShnapObject.AS_ARRAY, noArg(inst((ctx, trc) -> {
                return DeepArrays.deeplyConvertToShnap(object, trc);
            })));
        }
        return javaObject;
    }

}
