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

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.*;

public class ShnapAbsentNative extends ShnapObject implements ShnapJavaBackedNative, ShnapNativeType {
    private String name;

    public ShnapAbsentNative(ShnapLoc loc, String name) {
        super(loc, "absent");
        this.name = name;
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(false))));
        this.set("equals", ShnapFactory.func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.ONE)),
                inst((ctx, trc) -> {
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                       ShnapObject obj = e.getValue();

                       boolean res;
                       if (obj instanceof ShnapAbsentNative) {
                           res = ((ShnapAbsentNative) obj).name.equals(this.name);
                       } else {
                           res = false;
                       }

                       return ShnapExecution.normal(ShnapBooleanNative.of(res), trc, this.getLocation());
                    });
                })
        ));        this.descriptor().applyTo(this);
    }

    @Override
    public String defaultToString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Object getJavaBacker() {
        return null;
    }

    @Override
    public ShnapNativeTypeDescriptor descriptor() {
        return ShnapNativeTypeRegistry.Descriptor.ABSENT;
    }

}
