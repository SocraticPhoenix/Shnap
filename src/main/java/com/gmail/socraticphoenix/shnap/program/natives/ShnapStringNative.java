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

import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBigDecimalNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBigIntegerNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.mimicJavaException;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.oneArg;

public class ShnapStringNative extends ShnapObject {
    private String value;
    private int[] pts;

    public ShnapStringNative(ShnapLoc loc, String value) {
        super(loc);
        this.pts = value.codePoints().toArray();
        this.value = value.intern();
        //Conversion functions
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(this.value.equalsIgnoreCase("true")))));
        this.set(ShnapObject.AS_ARRAY, noArg(instSimple(() -> {
            ShnapObject[] arr = new ShnapObject[this.pts.length];
            for (int i = 0; i < this.pts.length; i++) {
                arr[i] = new ShnapCharNative(this.getLocation(), BigInteger.valueOf(this.pts[i]));
            }
            return new ShnapArrayNative(this.getLocation(), arr);
        })));
        this.set(ShnapObject.AS_NUMBER, noArg(inst((ctx, trc) -> {
            String toTest = this.value;
            NumberFormatException ex = null;
            try {
                if (toTest.endsWith("i")) {
                    return ShnapExecution.normal(new ShnapBigIntegerNative(this.getLocation(), new BigInteger(Strings.cutLast(toTest))), trc, this.getLocation());
                } else if (toTest.endsWith("d")) {
                    return ShnapExecution.normal(new ShnapBigDecimalNative(this.getLocation(), new BigDecimal(Strings.cutLast(toTest))), trc, this.getLocation());
                } else {
                    try {
                        return ShnapExecution.normal(new ShnapBigIntegerNative(this.getLocation(), new BigInteger(toTest)), trc, this.getLocation());
                    } catch (NumberFormatException e) {
                        return ShnapExecution.normal(new ShnapBigDecimalNative(this.getLocation(), new BigDecimal(toTest)), trc, this.getLocation());
                    }
                }
            } catch (NumberFormatException ignore) {
                ex = ignore;
            }
            return ShnapExecution.throwing(mimicJavaException("shnap.TypeError", "cannot convert type to number", ex), trc, this.getLocation());
        })));

        //Other functions
        this.set("compareTo", oneArg(inst((ctx, trc) -> {
            ShnapObject other = ctx.get("arg");
            ShnapExecution otherAsString = other.asString(trc);
            if (otherAsString.isAbnormal()) {
                return otherAsString;
            }
            String comp = ((ShnapStringNative) otherAsString.getValue()).getValue();
            return ShnapExecution.normal(ShnapNumberNative.valueOf(this.getValue().compareTo(comp)), trc, this.getLocation());
        })));
        this.set("add", oneArg(inst((ctx, trc) -> {
            int order = 1;
            if (ctx.directlyContains("order")) {
                ShnapObject orderObj = ctx.get("order");
                if (orderObj instanceof ShnapNumberNative) {
                    order = ((ShnapNumberNative) orderObj).getNumber().intValue();
                }
            }

            ShnapObject other = ctx.get("arg");
            ShnapExecution otherAsString = other.asString(trc);
            if (otherAsString.isAbnormal()) {
                return otherAsString;
            }
            String comp = ((ShnapStringNative) otherAsString.getValue()).getValue();

            if (order == 1) {
                return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, this.value + comp), trc, this.getLocation());
            } else {
                return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, comp + this.value), trc, this.getLocation());
            }
        })));
        this.set("len", noArg(instSimple(() -> ShnapNumberNative.valueOf(this.pts.length))));
        this.set("charAt", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg").asNum(trc);
            if(num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if(order < 0 || order >= this.pts.length) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, this.getLocation());
            } else {
                return ShnapExecution.normal(new ShnapCharNative(this.getLocation(), BigInteger.valueOf(this.pts[order])), trc, this.getLocation());
            }
        })));
        this.set("iterator", noArg(inst((ctx, trc) -> {
            ShnapObject iterator = new ShnapObject(ShnapLoc.BUILTIN);
            iterator.init(ctx);
            iterator.set("index", ShnapNumberNative.valueOf(0));
            iterator.set("hasNext", noArg(instSimple(() -> {
                ShnapObject val = iterator.get("index");
                if(val instanceof ShnapNumberNative) {
                    return ShnapBooleanNative.of(((ShnapNumberNative) val).getNumber().intValue() < this.pts.length);
                } else {
                    return ShnapBooleanNative.FALSE;
                }
            })));
            iterator.set("next", noArg(instSimple(() -> {
                ShnapObject val = iterator.get("index");
                if(val instanceof ShnapNumberNative) {
                    int index = ((ShnapNumberNative) val).getNumber().intValue();
                    if(index < 0 || index >= this.pts.length) {
                        return ShnapObject.getVoid();
                    }
                    iterator.set("index", ShnapNumberNative.valueOf(index + 1));
                    return new ShnapCharNative(this.getLocation(), BigInteger.valueOf(this.pts[index]));
                } else {
                    return ShnapObject.getVoid();
                }
            })));
            return ShnapExecution.normal(iterator, trc, this.getLocation());
        })));
    }

    public String getValue() {
        return this.value;
    }

}
