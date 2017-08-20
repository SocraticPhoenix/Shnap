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
import com.gmail.socraticphoenix.mirror.Reflections;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.type.java.ShnapJavaInterface;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Optional;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.mimicJavaException;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;

public class ShnapDefaultNatives {

    public static void registerDefaults() {
        ShnapNativeFuncRegistry.register("java.javaClass", func(
                Items.buildList(param("class")),
                inst((ctx, trc) -> {
                    ShnapExecution modString = ctx.get("class", trc).mapIfNormal(e -> e.getValue().asString(trc));
                    if (modString.isAbnormal()) {
                        return modString;
                    }

                    String cls = ((ShnapStringNative) modString.getValue()).getValue();
                    Optional<Class> type = Reflections.resolveClass(cls);
                    if (type.isPresent() && !type.get().isPrimitive()) {
                        return ShnapExecution.normal(ShnapJavaInterface.createClass(type.get()), trc, ShnapLoc.BUILTIN);
                    } else {
                        if(!type.isPresent()) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "No java class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        } else {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "Cannot get primitive class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        }
                    }
                })
        ));
        ShnapNativeFuncRegistry.register("java.javaArray", func(
                Items.buildList(param("arr"), param("class")),
                inst((ctx, trc) -> {
                    ShnapExecution shanpArr = ctx.get("arr", trc).mapIfNormal(e -> e.getValue().asArray(trc));
                    if(shanpArr.isAbnormal()) {
                        return shanpArr;
                    } else {
                        ShnapExecution modString = ctx.get("class", trc).mapIfNormal(e -> e.getValue().asString(trc));
                        if (modString.isAbnormal()) {
                            return modString;
                        }

                        String cls = ((ShnapStringNative) modString.getValue()).getValue();
                        Optional<Class> type = Reflections.resolveClass(cls);
                        if (!type.isPresent()) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "No java class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        }

                        Class clazz = type.get();
                        ShnapArrayNative arrayNative = (ShnapArrayNative) shanpArr.getValue();
                        ShnapObject[] val = arrayNative.getValue();
                        Object[] res = new Object[val.length];
                        for (int i = 0; i < val.length; i++) {
                            ShnapExecution asJava = val[i].asJava(trc);
                            if(asJava.isAbnormal()) {
                                return asJava;
                            }

                            res[i] = ((ShnapJavaBackedNative) asJava.getValue()).getJavaBacker();
                        }

                        try {
                            return ShnapExecution.normal(ShnapJavaInterface.createObject(Reflections.deepCast(Array.newInstance(clazz, 0).getClass(), res)), trc, ShnapLoc.BUILTIN);
                        } catch (Throwable e) {
                            return ShnapExecution.throwing(ShnapFactory.mimicJavaException("shnap.JavaInterfaceError", "Could not cast array to type: " + clazz.getName(), e), trc, ShnapLoc.BUILTIN);
                        }
                    }
                })
        ));


        ShnapNativeFuncRegistry.register("sys.sleep", oneArg(inst((ctx, trc) -> {
            long order = -2;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().longValue();
            }
            if (order != -2) {
                try {
                    Thread.sleep(order);
                } catch (InterruptedException e) {
                    return ShnapExecution.throwing(mimicJavaException("shnap.InterruptedError", "sleep was interrupted", e), trc, ShnapLoc.BUILTIN);
                }
            }
            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
        })));
        ShnapNativeFuncRegistry.register("sys.args", noArg(inst((ctx, trc) -> ShnapExecution.normal(trc.getArguments(), trc, ShnapLoc.BUILTIN))));


        ShnapNativeFuncRegistry.register("sys.import", func(
                Items.buildList(param("module")),
                inst((ctx, trc) -> {
                    ShnapExecution modString = ctx.get("module", trc).mapIfNormal(e -> e.getValue().asString(trc));
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
                    return ctx.get("obj", trc).mapIfNormal(objE -> ctx.get("name", trc).mapIfNormal(nameE -> {
                        ShnapObject obj = objE.getValue();
                        ShnapObject name = nameE.getValue();
                        ShnapExecution str = name.asString(trc);
                        if (str.isAbnormal()) {
                            return str;
                        } else {
                            String nameString = ((ShnapStringNative) str.getValue()).getValue();
                            return obj.getContext().get(nameString, trc);
                        }
                    }));
                })
        ));

        ShnapNativeFuncRegistry.register("sys.paramCount", func(
                Items.buildList(param("obj")),
                inst((ctx, trc) -> {
                    return ctx.get("obj", trc).mapIfNormal(e -> {
                        ShnapObject obj = e.getValue();
                        if (obj instanceof ShnapFunction) {
                            return ShnapExecution.normal(ShnapNumberNative.valueOf(((ShnapFunction) obj).paramsSize()), trc, ShnapLoc.BUILTIN);
                        } else {
                            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                        }
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.reqParamCount", func(
                Items.buildList(param("obj")),
                inst((ctx, trc) -> {
                    return ctx.get("obj", trc).mapIfNormal(e -> {
                        ShnapObject obj = e.getValue();
                        if (obj instanceof ShnapFunction) {
                            return ShnapExecution.normal(ShnapNumberNative.valueOf(((ShnapFunction) obj).paramSizeId()), trc, ShnapLoc.BUILTIN);
                        } else {
                            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                        }
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.defParamCount", func(
                Items.buildList(param("obj")),
                inst((ctx, trc) -> {
                    return ctx.get("obj", trc).mapIfNormal(e -> {
                        ShnapObject obj = e.getValue();
                        if (obj instanceof ShnapFunction) {
                            return ShnapExecution.normal(ShnapNumberNative.valueOf(((ShnapFunction) obj).defSize()), trc, ShnapLoc.BUILTIN);
                        } else {
                            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                        }
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.del", func(
                Items.buildList(param("obj"), param("name")),
                inst((ctx, trc) -> {
                    return ctx.get("obj", trc).mapIfNormal(objE -> ctx.get("name", trc).mapIfNormal(nameE -> {
                        ShnapObject obj = objE.getValue();
                        ShnapObject name = nameE.getValue();

                        ShnapExecution str = name.asString(trc);
                        if (str.isAbnormal()) {
                            return str;
                        } else {
                            String nameString = ((ShnapStringNative) str.getValue()).getValue();
                            obj.getContext().del(nameString);
                            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                        }
                    }));
                })
        ));

        ShnapNativeFuncRegistry.register("sys.set", func(
                Items.buildList(param("obj"), param("name"), param("val")),
                inst((ctx, trc) -> {
                    return ctx.get("obj", trc).mapIfNormal(objE -> ctx.get("name", trc).mapIfNormal(nameE -> ctx.get("val", trc).mapIfNormal(valE -> {
                        ShnapObject obj = objE.getValue();
                        ShnapObject name = nameE.getValue();
                        ShnapObject val = valE.getValue();
                        ShnapExecution str = name.asString(trc);
                        if (str.isAbnormal()) {
                            return str;
                        } else {
                            String nameString = ((ShnapStringNative) str.getValue()).getValue();
                            obj.getContext().set(nameString, val);
                        }
                        return ShnapExecution.normal(val, trc, ShnapLoc.BUILTIN);
                    })));
                })
        ));

        ShnapNativeFuncRegistry.register("sys.print", func(
                Items.buildList(param("arg")),
                inst((ctx, trc) -> {
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                        ShnapObject obj = e.getValue();
                        ShnapExecution str = obj.asString(trc);
                        if (str.isAbnormal()) {
                            return str;
                        } else {
                            System.out.print(((ShnapStringNative) str.getValue()).getValue());
                            return str;
                        }
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.printErr", func(
                Items.buildList(param("arg")),
                inst((ctx, trc) -> {
                    return ctx.get("arg", trc).mapIfNormal(e -> {
                        ShnapObject obj = e.getValue();
                        ShnapExecution str = obj.asString(trc);
                        if (str.isAbnormal()) {
                            return str;
                        } else {
                            System.err.print(((ShnapStringNative) str.getValue()).getValue());
                            return str;
                        }
                    });
                })
        ));

        ShnapNativeFuncRegistry.register("sys.fields", oneArg(inst((ctx, trc) -> {
            return ctx.get("arg", trc).mapIfNormal(e -> {
                ShnapObject obj = e.getValue();
                Collection<String> names = obj.getContext().names();
                ShnapObject[] arr = new ShnapObject[names.size()];
                int k = 0;
                for (String s : names) {
                    arr[k++] = new ShnapStringNative(ShnapLoc.BUILTIN, s);
                }
                return ShnapExecution.normal(new ShnapArrayNative(ShnapLoc.BUILTIN, arr), trc, ShnapLoc.BUILTIN);
            });
        })));

        /*
        TYPES:
         */
        ShnapNativeFuncRegistry.register("type.num", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc)))));
        ShnapNativeFuncRegistry.register("type.str", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asString(trc)))));
        ShnapNativeFuncRegistry.register("type.bool", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asBool(trc)))));
        ShnapNativeFuncRegistry.register("type.array", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asArray(trc)))));
        ShnapNativeFuncRegistry.register("type.java", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asJava(trc)))));

        ShnapNativeFuncRegistry.register("type.char", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapCharNative(ShnapLoc.BUILTIN, ((ShnapNumberNative) number.getValue()).getNumber().intValue()), trc, ShnapLoc.BUILTIN);
        })));

        ShnapNativeFuncRegistry.register("type.newArray", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            if (order < 0) {
                return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
            } else {
                return ShnapExecution.normal(new ShnapArrayNative(ShnapLoc.BUILTIN, order), trc, ShnapLoc.BUILTIN);
            }
        })));
    }

}