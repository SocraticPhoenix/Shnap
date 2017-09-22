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
package com.gmail.socraticphoenix.shnap.type.natives.num;

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapNativeTypeDescriptor;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapNativeTypeRegistry;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.math.BigInteger;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;

public class ShnapBooleanNative extends ShnapObject implements ShnapNumberNative {
    public static final ShnapBooleanNative TRUE = new ShnapBooleanNative(ShnapLoc.BUILTIN, true);
    public static final ShnapBooleanNative FALSE = new ShnapBooleanNative(ShnapLoc.BUILTIN, false);

    private Boolean value;

    private ShnapBooleanNative(ShnapLoc loc, Boolean value) {
        super(loc);
        this.value = value;
        ShnapNumberNative.implementFunctions(this, this);
        this.set(ShnapObject.AS_STRING, ShnapFactory.noArg(instSimple(() -> new ShnapStringNative(this.getLocation(), String.valueOf(this.value)))));
        this.set("and", ShnapFactory.func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.ONE)),
                inst((ctx, trc) -> {
                    int order = 1;
                    if (ctx.directlyContains("order")) {
                        ShnapExecution ord = ctx.get("order", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                        if (ord.isAbnormal()) {
                            return ord;
                        } else {
                            order = ((ShnapNumberNative) ord.getValue()).getNumber().intValue();
                        }
                    }

                    return ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asBool(trc).mapIfNormal(be -> {
                        boolean other = ((ShnapBooleanNative) be.getValue()).getValue();
                        return ShnapExecution.normal(ShnapBooleanNative.of(this.value && other), trc, this.getLocation());
                    }));
                })
        ));
        this.set("or", ShnapFactory.func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.ONE)),
                inst((ctx, trc) -> {
                    int order = 1;
                    if (ctx.directlyContains("order")) {
                        ShnapExecution ord = ctx.get("order", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                        if (ord.isAbnormal()) {
                            return ord;
                        } else {
                            order = ((ShnapNumberNative) ord.getValue()).getNumber().intValue();
                        }
                    }

                    return ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asBool(trc).mapIfNormal(be -> {
                        boolean other = ((ShnapBooleanNative) be.getValue()).getValue();
                        return ShnapExecution.normal(ShnapBooleanNative.of(this.value || other), trc, this.getLocation());
                    }));
                })
        ));
        this.set("not", noArg(instSimple(() -> ShnapBooleanNative.of(!this.value))));
    }

    @Override
    public Number getNumber() {
        return this.value ? BigInteger.ONE : BigInteger.ZERO;
    }

    @Override
    public ShnapObject copyWith(Number n) {
        if (n.doubleValue() == 0) {
            return FALSE;
        } else if (n.doubleValue() == 1) {
            return TRUE;
        }

        return ShnapNumberNative.valueOf(n);
    }

    @Override
    public int castingPrecedence(Number result) {
        return 0;
    }

    public boolean getValue() {
        return this.value;
    }

    public static ShnapBooleanNative of(boolean b) {
        return b ? ShnapBooleanNative.TRUE : ShnapBooleanNative.FALSE;
    }

    @Override
    public Object getJavaBacker() {
        return this.value;
    }

    @Override
    public ShnapNativeTypeDescriptor descriptor() {
        return ShnapNativeTypeRegistry.Descriptor.BOOLEAN;
    }

}
