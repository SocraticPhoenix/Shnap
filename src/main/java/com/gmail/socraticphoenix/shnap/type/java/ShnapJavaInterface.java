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

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.java.provider.ShnapJavaClassProvider;
import com.gmail.socraticphoenix.shnap.type.java.provider.ShnapReflectiveJavaClassProvider;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ShnapJavaInterface {
    private static Function<Class, ShnapJavaClassProvider> defaultProvider = ShnapReflectiveJavaClassProvider::new;
    private static Map<Class, ShnapJavaClassProvider> providers = new LinkedHashMap<>();

    public static Function<Class, ShnapJavaClassProvider> getDefaultProvider() {
        return defaultProvider;
    }

    public static void setDefaultProvider(Function<Class, ShnapJavaClassProvider> defaultProvider) {
        ShnapJavaInterface.defaultProvider = defaultProvider;
    }

    public static void registerProvider(Class cls, ShnapJavaClassProvider provider) {
        providers.put(cls, provider);
    }

    public static ShnapObject createClass(Class java) {
        if(java == null) {
            return ShnapObject.getNull();
        } else {
            Class target = java;
            ShnapJavaClassProvider provider = providers.get(target);
            if(provider == null) {
                provider = defaultProvider.apply(target);
                providers.put(target, provider);
            }

            return provider.createClassObject();
        }
    }

    public static ShnapObject createObject(Object java) {
        if(java == null) {
            return ShnapObject.getNull();
        } else {
            Class target = java.getClass();
            ShnapJavaClassProvider provider = providers.get(target);
            if(provider == null) {
                provider = defaultProvider.apply(target);
                providers.put(target, provider);
            }

            return provider.createObject(java);
        }
    }

    public static ShnapLoc createJavaLoc(Class owner, Method method) {
        return ShnapLoc.BUILTIN;
    }

    public static ShnapLoc createJavaLoc(Class owner, Constructor method) {
        return ShnapLoc.BUILTIN;
    }

    public static ShnapLoc createJavaLoc(Class cls) {
        return ShnapLoc.BUILTIN;
    }

}
