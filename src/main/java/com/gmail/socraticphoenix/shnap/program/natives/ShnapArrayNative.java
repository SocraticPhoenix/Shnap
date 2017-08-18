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

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.shnap.program.ShnapFactory;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;

import java.util.Arrays;

import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.forBlock;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.ifTrue;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.literal;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.param;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.returning;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.sequence;

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

    private void applyFunctions() {
        //Conversion functions; AS_STRING is implemented with defaultToString
        this.set(ShnapObject.AS_NUMBER, noArg(instSimple(() -> ShnapNumberNative.valueOf(this.value.length))));
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(this.value.length != 0))));

        //Other functions
        this.set("remove", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg").asNum(trc);
            if(num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, this.getLocation());
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
            ShnapExecution num = ctx.get("arg").asNum(trc);
            if(num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order > this.value.length) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, this.getLocation());
            } else {
                ShnapObject insert = ctx.get("val");

                ShnapExecution toGive = ShnapExecution.normal(order == this.value.length ? ShnapObject.getVoid() : this.value[order], trc, this.getLocation());
                ShnapObject[] value = this.value;
                this.value = new ShnapObject[value.length + 1];
                System.arraycopy(value, 0, this.value, 0, order);
                System.arraycopy(value, order, this.value, order + 1, value.length - order);
                this.value[order] = insert;

                return toGive;
            }
        })));

        this.set("get", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg").asNum(trc);
            if(num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, this.getLocation());
            } else {
                return ShnapExecution.normal(this.value[order], trc, this.getLocation());
            }
        })));

        this.set("set", func(Items.buildList(param("index"), param("val")), inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg").asNum(trc);
            if(num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0 || order >= this.value.length) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, this.getLocation());
            } else {
                ShnapExecution toGive = ShnapExecution.normal(this.value[order], trc, this.getLocation());
                this.value[order] = ctx.get("val");
                return toGive;
            }
        })));

        this.set("append", oneArg(inst((ctx, trc) -> {
            ShnapObject[] value = this.value;
            this.value = new ShnapObject[value.length + 1];
            System.arraycopy(value, 0, this.value, 0, value.length);
            this.value[this.value.length - 1] = ctx.get("arg");
            return ShnapExecution.normal(this, trc, this.getLocation());
        })));

        this.set("len", noArg(instSimple(() -> ShnapNumberNative.valueOf(this.value.length))));

        this.set("iterator", noArg(inst((ctx, trc) -> {
            ShnapObject iterator = new ShnapObject(ShnapLoc.BUILTIN);
            iterator.init(ctx);
            iterator.set("index", ShnapNumberNative.valueOf(0));
            iterator.set("hasNext", noArg(instSimple(() -> {
                ShnapObject val = iterator.get("index");
                if(val instanceof ShnapNumberNative) {
                    return ShnapBooleanNative.of(((ShnapNumberNative) val).getNumber().intValue() < this.value.length);
                } else {
                    return ShnapBooleanNative.FALSE;
                }
            })));
            iterator.set("next", noArg(instSimple(() -> {
                ShnapObject val = iterator.get("index");
                if(val instanceof ShnapNumberNative) {
                    int index = ((ShnapNumberNative) val).getNumber().intValue();
                    if(index < 0 || index >= this.value.length) {
                        return ShnapObject.getVoid();
                    }
                    iterator.set("index", ShnapNumberNative.valueOf(index + 1));
                    return this.value[index];
                } else {
                    return ShnapObject.getVoid();
                }
            })));
            return ShnapExecution.normal(iterator, trc, this.getLocation());
        })));

        this.set("contains", oneArg(sequence(
                forBlock("it", ShnapFactory.get("this"), ifTrue(
                        ShnapFactory.operate(ShnapFactory.get("it"), ShnapOperators.EQUAL, ShnapFactory.get("arg")),
                        returning(literal(true))
                )),
                returning(literal(false))
        )));
    }

}
