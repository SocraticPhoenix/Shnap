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

package com.gmail.socraticphoenix.shnap.type.natives;

import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigDecimalNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigIntegerNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapDoubleNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapIntNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapLongNative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShnapNativeTypeRegistry {

    public static interface Descriptor {
        ShnapNativeTypeDescriptor BIG_DECIMAL = new ShnapNativeTypeDescriptor(ShnapBigDecimalNative.class);
        ShnapNativeTypeDescriptor BIG_INTEGER = new ShnapNativeTypeDescriptor(ShnapBigIntegerNative.class);
        ShnapNativeTypeDescriptor BOOLEAN = new ShnapNativeTypeDescriptor(ShnapBooleanNative.class);
        ShnapNativeTypeDescriptor CHAR = new ShnapNativeTypeDescriptor(ShnapCharNative.class);
        ShnapNativeTypeDescriptor DOUBLE = new ShnapNativeTypeDescriptor(ShnapDoubleNative.class);
        ShnapNativeTypeDescriptor INT = new ShnapNativeTypeDescriptor(ShnapIntNative.class);
        ShnapNativeTypeDescriptor LONG = new ShnapNativeTypeDescriptor(ShnapLongNative.class);

        ShnapNativeTypeDescriptor ABSENT = new ShnapNativeTypeDescriptor(ShnapAbsentNative.class);
        ShnapNativeTypeDescriptor ARRAY = new ShnapNativeTypeDescriptor(ShnapArrayNative.class);
        ShnapNativeTypeDescriptor STRING = new ShnapNativeTypeDescriptor(ShnapStringNative.class);
    }

    private static Map<String, List<ShnapNativeTypeDescriptor>> types = new HashMap<>();

    static {
        register("all", Descriptor.BIG_DECIMAL, Descriptor.BIG_INTEGER, Descriptor.BOOLEAN, Descriptor.CHAR, Descriptor.DOUBLE, Descriptor.INT, Descriptor.LONG, Descriptor.ABSENT, Descriptor.ARRAY, Descriptor.STRING);

        register("num", Descriptor.BIG_DECIMAL, Descriptor.BIG_INTEGER, Descriptor.BOOLEAN, Descriptor.CHAR, Descriptor.DOUBLE, Descriptor.INT, Descriptor.LONG);
        register("dec", Descriptor.BIG_DECIMAL);
        register("int", Descriptor.BIG_INTEGER);
        register("bool", Descriptor.BOOLEAN);
        register("char", Descriptor.CHAR);
        register("float64", Descriptor.DOUBLE);
        register("int32", Descriptor.INT);
        register("int64", Descriptor.LONG);

        register("absent", Descriptor.ABSENT);
        register("arr", Descriptor.ARRAY);
        register("str", Descriptor.STRING);
    }

    public static Map<String, List<ShnapNativeTypeDescriptor>> getTypes() {
        return types;
    }

    public static void register(String name, ShnapNativeTypeDescriptor... objects) {
        Collections.addAll(types.computeIfAbsent(name, k -> new ArrayList<>()), objects);
    }

}
