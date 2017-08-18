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
package com.gmail.socraticphoenix.shnap.program.natives;

import com.gmail.socraticphoenix.shnap.program.ShnapFactory;
import com.gmail.socraticphoenix.shnap.program.ShnapFunction;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShnapNativeFuncRegistry {
    private static Map<String, ShnapFunction> funcs = new HashMap<>();

    public static void register(String name, ShnapFunction function) {
        if(funcs.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate native function key: " + name);
        }

        funcs.put(name, function);
    }

    public static ShnapFunction get(String name) {
        ShnapFunction get = funcs.get(name);
        if(get == null) {
            return ShnapFactory.func(Collections.emptyList(), ShnapFactory.instSimple(ShnapObject::getVoid));
        } else {
            return get;
        }
    }

}
