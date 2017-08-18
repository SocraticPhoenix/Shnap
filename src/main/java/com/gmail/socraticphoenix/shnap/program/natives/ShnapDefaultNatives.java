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
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;

import java.util.Collection;

import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.param;

public class ShnapDefaultNatives {

    public static void registerDefaults() {
        ShnapNativeFuncRegistry.register("sys.args", noArg(inst((ctx, trc) -> ShnapExecution.normal(trc.getArguments(), trc, ShnapLoc.BUILTIN))));
        ShnapNativeFuncRegistry.register("sys.importFrom", func(
                Items.buildList(param("module"), param("field")),
                inst((ctx, trc) -> {
                    ShnapObject moduleObj = ctx.get("module");
                    ShnapObject fieldObj = ctx.get("field");

                    ShnapExecution modString = moduleObj.asString(trc);
                    if (modString.isAbnormal()) {
                        return modString;
                    }

                    ShnapExecution fieldString = fieldObj.asString(trc);
                    if (fieldString.isAbnormal()) {
                        return fieldString;
                    }

                    String field = ((ShnapStringNative) fieldString.getValue()).getValue();
                    String module = ((ShnapStringNative) modString.getValue()).getValue();

                    return trc.getModuleExecution(module).mapIfNormal(exe -> {
                        ShnapObject obj = exe.getValue();
                        if(obj.getContext().hasFlag(field, ShnapContext.Flag.PRIVATE)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.AccessError", "field " + field + " is flagged with PRIVATE", null), trc, ShnapLoc.BUILTIN);
                        }
                        return ShnapExecution.normal(obj.get(field), trc, ShnapLoc.BUILTIN);
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.import", func(
                Items.buildList(param("module")),
                inst((ctx, trc) -> {
                    ShnapObject moduleObj = ctx.get("module");

                    ShnapExecution modString = moduleObj.asString(trc);
                    if (modString.isAbnormal()) {
                        return modString;
                    }

                    String module = ((ShnapStringNative) modString.getValue()).getValue();

                    return trc.getModuleExecution(module);
                })
        ));

        ShnapNativeFuncRegistry.register("sys.get", func(
                Items.buildList(param("obj"), param("name")),
                inst((ctx, trc) -> {
                    ShnapObject obj = ctx.get("obj");
                    ShnapObject name = ctx.get("name");

                    ShnapExecution str = name.asString(trc);
                    if (str.isAbnormal()) {
                        return str;
                    } else {
                        String nameString = ((ShnapStringNative) str.getValue()).getValue();
                        return ShnapExecution.normal(obj.getContext().get(nameString), trc, ShnapLoc.BUILTIN);
                    }
                })
        ));

        ShnapNativeFuncRegistry.register("sys.del", func(
                Items.buildList(param("obj"), param("name")),
                inst((ctx, trc) -> {
                    ShnapObject obj = ctx.get("obj");
                    ShnapObject name = ctx.get("name");

                    ShnapExecution str = name.asString(trc);
                    if (str.isAbnormal()) {
                        return str;
                    } else {
                        String nameString = ((ShnapStringNative) str.getValue()).getValue();
                        ctx.del(nameString);
                        return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                    }
                })
        ));

        ShnapNativeFuncRegistry.register("sys.set", func(
                Items.buildList(param("obj"), param("name"), param("val")),
                inst((ctx, trc) -> {
                    ShnapObject obj = ctx.get("obj");
                    ShnapObject name = ctx.get("name");
                    ShnapObject val = ctx.get("val");

                    ShnapExecution str = name.asString(trc);
                    if (str.isAbnormal()) {
                        return str;
                    } else {
                        String nameString = ((ShnapStringNative) str.getValue()).getValue();
                        obj.getContext().set(nameString, val);
                    }
                    return ShnapExecution.normal(val, trc, ShnapLoc.BUILTIN);
                })
        ));

        ShnapNativeFuncRegistry.register("sys.print", func(
                Items.buildList(param("arg")),
                inst((ctx, trc) -> {
                    ShnapObject obj = ctx.get("arg");
                    ShnapExecution str = obj.asString(trc);
                    if (str.isAbnormal()) {
                        return str;
                    } else {
                        System.out.print(((ShnapStringNative) str.getValue()).getValue());
                        return str;
                    }
                })
        ));

        ShnapNativeFuncRegistry.register("sys.printErr", func(
                Items.buildList(param("arg")),
                inst((ctx, trc) -> {
                    ShnapObject obj = ctx.get("arg");
                    ShnapExecution str = obj.asString(trc);
                    if (str.isAbnormal()) {
                        return str;
                    } else {
                        System.err.print(((ShnapStringNative) str.getValue()).getValue());
                        return str;
                    }
                })
        ));

        ShnapNativeFuncRegistry.register("sys.fields", oneArg(inst((ctx, trc) -> {
            ShnapObject obj = ctx.get("arg");
            Collection<String> names = obj.getContext().names();
            ShnapObject[] arr = new ShnapObject[names.size()];
            int k = 0;
            for(String s : names) {
                arr[k++] = new ShnapStringNative(ShnapLoc.BUILTIN, s);
            }
            return ShnapExecution.normal(new ShnapArrayNative(ShnapLoc.BUILTIN, arr), trc, ShnapLoc.BUILTIN);
        })));

        ShnapNativeFuncRegistry.register("sys.identity", oneArg(inst((ctx, trc) -> ShnapExecution.normal(ShnapNumberNative.valueOf(System.identityHashCode(ctx.get("arg"))), trc, ShnapLoc.BUILTIN))));

        /*
        TYPES:
         */
        ShnapNativeFuncRegistry.register("type.num", oneArg(inst((ctx, trc) -> ctx.get("arg").asNum(trc))));
        ShnapNativeFuncRegistry.register("type.str", oneArg(inst((ctx, trc) -> ctx.get("arg").asString(trc))));
        ShnapNativeFuncRegistry.register("type.array", oneArg(inst((ctx, trc) -> ctx.get("arg").asArray(trc))));
        ShnapNativeFuncRegistry.register("type.bool", oneArg(inst((ctx, trc) -> ShnapExecution.normal(ShnapBooleanNative.of(ctx.get("arg").isTruthy(trc)), trc, ShnapLoc.BUILTIN))));

        ShnapNativeFuncRegistry.register("type.newArray", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapObject orderObj = ctx.get("arg");
            if (orderObj instanceof ShnapNumberNative) {
                order = ((ShnapNumberNative) orderObj).getNumber().intValue();
            }

            if (order < 0) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
            } else {
                return ShnapExecution.normal(new ShnapArrayNative(ShnapLoc.BUILTIN, order), trc, ShnapLoc.BUILTIN);
            }
        })));
    }

}
