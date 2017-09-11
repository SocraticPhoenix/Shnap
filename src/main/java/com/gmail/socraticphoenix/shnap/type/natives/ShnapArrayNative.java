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
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;

import java.util.Arrays;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.forBlock;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.funcExactly;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.ifTrue;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.literal;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.returning;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.sequence;

public class ShnapArrayNative extends ShnapObject {
    private ShnapObject[] value;

    public ShnapArrayNative(ShnapLoc loc, ShnapObject... value) {
        super(loc);
        this.value = value;
        this.applyFunctions();
    }

    public ShnapArrayNative(ShnapLoc loc, int size) {
        super(loc);
        this.value = new ShnapObject[size];
        Arrays.fill(this.value, ShnapObject.getNull());
        this.applyFunctions();
    }

    @Override
    public String defaultToString() {
        return "arr[" + this.value.length + "]::" + this.identityStr();
    }

    public ShnapObject[] getValue() {
        return this.value;
    }

    private void applyFunctions() {
        //TODO throw errors instead of returning void
        //Conversion functions; AS_STRING is implemented with defaultToString
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(this.value.length != 0))));

        //Other functions
        this.set("remove", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            } else {
                ShnapExecution toGive = ShnapExecution.normal(this.value[order], trc, this.getLocation());
                ShnapObject[] value = this.value;
                this.value = new ShnapObject[value.length - 1];
                System.arraycopy(value, 0, this.value, 0, order);
                System.arraycopy(value, order + 1, this.value, order, value.length - order - 1);
                return toGive;
            }
        })));

        this.set("insert", func(Items.buildList(param("index"), param("val")), inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("index", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order > this.value.length) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            } else {
                int finalOrder = order;
                return ctx.get("val", trc).mapIfNormal(e -> {
                    ShnapObject insert = e.getValue();
                    ShnapExecution toGive = ShnapExecution.normal(finalOrder == this.value.length ? ShnapObject.getVoid() : this.value[finalOrder], trc, this.getLocation());
                    ShnapObject[] value = this.value;
                    this.value = new ShnapObject[value.length + 1];
                    System.arraycopy(value, 0, this.value, 0, finalOrder);
                    System.arraycopy(value, finalOrder, this.value, finalOrder + 1, value.length - finalOrder);
                    this.value[finalOrder] = insert;

                    return toGive;
                });
            }
        })));

        this.set("get", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            } else {
                return ShnapExecution.normal(this.value[order], trc, this.getLocation());
            }
        })));

        this.set("set", func(Items.buildList(param("val"), param("index")), inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("index", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            } else {
                ShnapExecution toGive = ShnapExecution.normal(this.value[order], trc, this.getLocation());
                int finalOrder = order;
                return ctx.get("val", trc).mapIfNormal(e -> {
                    this.value[finalOrder] = e.getValue();
                    return toGive;
                });
            }
        })));

        this.set("append", oneArg(inst((ctx, trc) -> {
            ShnapObject[] value = this.value;
            this.value = new ShnapObject[value.length + 1];
            System.arraycopy(value, 0, this.value, 0, value.length);
            return ctx.get("arg", trc).mapIfNormal(e -> {
                this.value[this.value.length - 1] = e.getValue();
                return ShnapExecution.normal(this, trc, this.getLocation());
            });
        })));

        this.set("resize", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.IndexError", "Index out of bounds: " + order, null), trc, this.getLocation());
            }

            ShnapObject[] newArray = new ShnapObject[order];
            if (order > this.value.length) {
                System.arraycopy(this.value, 0, newArray, 0, this.value.length);
                for (int i = this.value.length; i < newArray.length; i++) {
                    newArray[i] = ShnapObject.getNull();
                }
            } else if (order > 0) {
                System.arraycopy(this.value, 0, newArray, 0, newArray.length);
            }

            this.value = newArray;

            return ShnapExecution.normal(this, trc, this.getLocation());
        })));

        this.set("copy", noArg(instSimple(() -> {
            ShnapObject[] dst = new ShnapObject[this.value.length];
            System.arraycopy(this.value, 0, dst, 0, this.value.length);
            return new ShnapArrayNative(this.getLocation(), dst);
        })));

        this.set("contains", funcExactly(Items.buildList(), sequence(
                forBlock("it", ShnapFactory.get("this"), ifTrue(
                        ShnapFactory.operate(ShnapFactory.get("it"), ShnapOperators.EQUAL, ShnapFactory.get("arg")),
                        returning(literal(true))
                )),
                returning(literal(false))
        )));

        this.set("len", noArg(instSimple(() -> ShnapNumberNative.valueOf(this.value.length))));

        this.set("iterator", noArg(inst((ctx, trc) -> {
            ShnapObject iterator = new ShnapObject(this.getLocation());
            iterator.init(ctx);
            iterator.set("index", ShnapNumberNative.valueOf(0));
            iterator.set("hasNext", noArg(inst((con, tra) -> {
                ShnapExecution num = iterator.get("index", tra).mapIfNormal(e -> e.getValue().asNum(tra));
                if (num.isAbnormal()) {
                    return num;
                }

                int index = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                return ShnapExecution.normal(ShnapBooleanNative.of(index < this.value.length), tra, this.getLocation());
            })));
            iterator.set("next", noArg(inst((con, tra) -> {
                ShnapExecution num = iterator.get("index", trc).mapIfNormal(e -> e.getValue().asNum(trc));
                if (num.isAbnormal()) {
                    return num;
                }

                int index = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
                if (index < 0 || index >= this.value.length) {
                    return ShnapExecution.normal(ShnapObject.getVoid(), tra, this.getLocation());
                }
                iterator.set("index", ShnapNumberNative.valueOf(index + 1));
                return ShnapExecution.normal(this.value[index], tra, this.getLocation());
            })));
            return ShnapExecution.normal(iterator, trc, this.getLocation());
        })));
    }

}
