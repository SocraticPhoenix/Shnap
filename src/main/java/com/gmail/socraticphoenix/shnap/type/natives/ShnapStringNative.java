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
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigDecimalNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigIntegerNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.mimicJavaException;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;

public class ShnapStringNative extends ShnapObject implements ShnapJavaBackedNative, ShnapNativeType {
    private String value;
    private int[] pts;

    public ShnapStringNative(ShnapLoc loc, String value) {
        super(loc, "str");
        this.pts = value.codePoints().toArray();
        this.value = value.intern();
        //Conversion functions
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(this.value.equalsIgnoreCase("true")))));
        this.set(ShnapObject.AS_ARRAY, noArg(instSimple(() -> {
            ShnapObject[] arr = new ShnapObject[this.pts.length];
            for (int i = 0; i < this.pts.length; i++) {
                arr[i] = new ShnapCharNative(this.getLocation(), this.pts[i]);
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
        this.set("compareTo", func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.valueOf(1))),
                inst((ctx, trc) -> {
                    int order = 1;
                    ShnapExecution num = ctx.get("order", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                    if (num.isAbnormal()) {
                        return num;
                    } else {
                        order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                    }
                    int finalOrder = order;
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                        ShnapObject other = e.getValue();
                        ShnapExecution otherAsString = other.asString(trc);
                        if (otherAsString.isAbnormal()) {
                            return otherAsString;
                        }
                        String comp = ((ShnapStringNative) otherAsString.getValue()).getValue();
                        return ShnapExecution.normal(ShnapNumberNative.valueOf(finalOrder == 1 ? this.getValue().compareTo(comp) : comp.compareTo(this.getValue())), trc, this.getLocation());
                    });
                })));
        this.set("equals", func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.valueOf(1))),
                inst((ctx, trc) -> {
                    int order = 1;
                    ShnapExecution num = ctx.get("order", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                    if (num.isAbnormal()) {
                        return num;
                    } else {
                        order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                    }
                    int finalOrder = order;
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                        ShnapObject other = e.getValue();
                        ShnapExecution otherAsString = other.asString(trc);
                        if (otherAsString.isAbnormal()) {
                            return otherAsString;
                        }
                        String comp = ((ShnapStringNative) otherAsString.getValue()).getValue();
                        return ShnapExecution.normal(ShnapBooleanNative.of(this.equals(comp)), trc, this.getLocation());
                    });
                })));
        this.set("add", func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.valueOf(1))),
                inst((ctx, trc) -> {
                    int order = 1;
                    ShnapExecution num = ctx.get("order", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                    if (num.isAbnormal()) {
                        return num;
                    } else {
                        order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                    }


                    int finalOrder = order;
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                        ShnapObject other = e.getValue();
                        ShnapExecution otherAsString = other.asString(trc);
                        if (otherAsString.isAbnormal()) {
                            return otherAsString;
                        }
                        String comp = ((ShnapStringNative) otherAsString.getValue()).getValue();

                        if (finalOrder == 1) {
                            return ShnapExecution.normal(new ShnapStringNative(this.getLocation(), this.value + comp), trc, this.getLocation());
                        } else {
                            return ShnapExecution.normal(new ShnapStringNative(this.getLocation(), comp + this.value), trc, this.getLocation());
                        }
                    });
                })));
        this.set("multiply", func(
                Items.buildList(param("arg"), param("order", ShnapNumberNative.valueOf(1))),
                inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc).mapIfNormal(en -> {
                    BigDecimal target = ShnapNumberNative.asDec(((ShnapNumberNative) en.getValue()).getNumber());
                    BigInteger intPart = target.toBigInteger();
                    BigDecimal decPart = target.subtract(new BigDecimal(intPart));
                    StringBuilder res = new StringBuilder();
                    while ((intPart = intPart.subtract(BigInteger.ONE)).compareTo(BigInteger.ZERO) >= 0) {
                        res.append(this.value);
                    }

                    int index = decPart.multiply(new BigDecimal(this.pts.length)).intValue();
                    for (int i = 0; i < index; i++) {
                        res.appendCodePoint(this.pts[i]);
                    }

                    return ShnapExecution.normal(new ShnapStringNative(this.getLocation(), res.toString()), trc, this.getLocation());
                })))
        ));
        this.set("len", noArg(instSimple(() -> ShnapNumberNative.valueOf(this.pts.length))));
        this.set("get", oneArg(inst((ctx, trc) -> {
            int order = 1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.pts.length) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            } else {
                return ShnapExecution.normal(new ShnapCharNative(this.getLocation(), this.pts[order]), trc, this.getLocation());
            }
        })));
        this.set("contains", oneArg(inst((ctx, trc) -> {
            ShnapExecution arg = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asString(trc));
            if (arg.isAbnormal()) {
                return arg;
            }

            return ShnapExecution.normal(ShnapBooleanNative.of(this.value.contains(((ShnapStringNative) arg.getValue()).getValue())), trc, this.getLocation());
        })));
        this.set("iterator", noArg(inst((ctx, trc) -> {
            ShnapObject iterator = new ShnapObject(this.getLocation(), "iterator");
            iterator.init(ctx);
            iterator.set("index", ShnapNumberNative.valueOf(0));
            iterator.set("hasNext", noArg(inst((con, tra) -> {
                ShnapExecution num = iterator.get("index", tra).mapIfNormal(e -> e.getValue().asNum(tra));
                if (num.isAbnormal()) {
                    return num;
                }

                int index = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                return ShnapExecution.normal(ShnapBooleanNative.of(index < this.pts.length), tra, this.getLocation());
            })));
            iterator.set("next", noArg(inst((con, tra) -> {
                ShnapExecution num = iterator.get("index", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                if (num.isAbnormal()) {
                    return num;
                }

                int index = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                if (index < 0 || index >= this.pts.length) {
                    return ShnapExecution.normal(ShnapObject.getVoid(), tra, this.getLocation());
                }
                iterator.set("index", ShnapNumberNative.valueOf(index + 1));
                return ShnapExecution.normal(new ShnapCharNative(this.getLocation(), this.pts[index]), tra, this.getLocation());
            })));
            return ShnapExecution.normal(iterator, trc, this.getLocation());
        })));
        this.set("isEmpty", noArg(instSimple(() -> ShnapBooleanNative.of(this.pts.length == 0))));
        this.set("equalsIgnoreCase", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asString(trc).mapIfNormal(strE -> {
            String comp = ((ShnapStringNative) strE.getValue()).getValue();
            return ShnapExecution.normal(ShnapBooleanNative.of(this.value.equalsIgnoreCase(comp)), trc, this.getLocation());
        })))));
        this.set("getSlice", func(
                Items.buildList(param("start"), param("end")),
                inst((ctx, trc) -> ctx.get("start", trc).mapIfNormal(se -> se.getValue().asNum(trc).mapIfNormal(sne -> ctx.get("end", trc).mapIfNormal(ee -> ee.getValue().asNum(trc).mapIfNormal(ene -> {
                    int start = ((ShnapNumberNative) sne.getValue()).getNumber().intValue();
                    int end = ((ShnapNumberNative) ene.getValue()).getNumber().intValue();
                    if (end < start) {
                        return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.RangeError", start + ".." + end, null), trc, this.getLocation());
                    } else if (start < 0) {
                        return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", String.valueOf(start), null), trc, this.getLocation());
                    } else if (end > this.pts.length) {
                        return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", String.valueOf(end), null), trc, this.getLocation());
                    } else {
                        int len = end - start;
                        int[] pts = new int[len];
                        System.arraycopy(this.pts, start, pts, 0, len);
                        return ShnapExecution.normal(new ShnapStringNative(this.getLocation(), new String(pts, 0, len)));
                    }
                })))))
        ));
        this.descriptor().applyTo(this);
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public Object getJavaBacker() {
        return this.value;
    }

    @Override
    public ShnapNativeTypeDescriptor descriptor() {
        return ShnapNativeTypeRegistry.Descriptor.STRING;
    }

}
