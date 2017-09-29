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
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.parse.ShnapParser;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.type.java.ShnapJavaInterface;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigDecimalNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBigIntegerNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapDoubleNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapIntNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapLongNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;
import com.gmail.socraticphoenix.shnap.util.DeepArrays;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.func;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.literalObj;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.mimicJavaException;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;

public class ShnapDefaultNatives {

    public static void registerDefaults() {
        ShnapNativeFuncRegistry.register("intakeBuiltins", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> {
            ShnapObject target = e.getValue();
            trc.applyDefaults(target.getContext());
            return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
        }))));

        ShnapNativeFuncRegistry.register("sys.escape", oneArg(inst((ctx, trc) -> ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asString(trc).mapIfNormal(strE -> {
            String val = ((ShnapStringNative) strE.getValue()).getValue();
            return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, Strings.escape(val)), trc, ShnapLoc.BUILTIN);
        })))));

        ShnapNativeFuncRegistry.register("sys.implementNative", func(
                Items.buildList(param("name"), param("field"), param("function")),
                inst((ctx, trc) -> ctx.get("name", trc).mapIfNormal(nameExec -> ctx.get("field", trc).mapIfNormal(fieldExec -> ctx.get("function", trc).mapIfNormal(funcExec -> nameExec.getValue().asString(trc).mapIfNormal(nameStrExec -> fieldExec.getValue().asString(trc).mapIfNormal(fieldStrExec -> {
                    String name = ((ShnapStringNative) nameStrExec.getValue()).getValue();
                    String field = ((ShnapStringNative) fieldStrExec.getValue()).getValue();
                    List<ShnapNativeTypeDescriptor> descriptor = ShnapNativeTypeRegistry.getTypes().get(name);
                    if (descriptor != null) {
                        descriptor.forEach(d -> d.getRegistry().put(field, funcExec.getValue()));
                    }
                    return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                }))))))
        ));

        ShnapNativeFuncRegistry.register("java.javaCast", func(
                Items.buildList(param("val"), param("class")),
                inst((ctx, trc) -> {
                    return ctx.get("val", trc).mapIfNormal(val -> ctx.get("class", trc).mapIfNormal(cls -> cls.getValue().asString(trc).mapIfNormal(clsStr -> {
                        Optional<Class> type = Reflections.resolveClass(((ShnapStringNative) clsStr.getValue()).getValue());
                        if (type.isPresent()) {
                            return val.getValue().asJava(trc).mapIfNormal(exe -> {
                                ShnapJavaBackedNative backed = (ShnapJavaBackedNative) exe.getValue();
                                return ShnapExecution.returning(ShnapJavaInterface.createObject(nullSafetyCast(type.get(), backed.getJavaBacker())), trc, ShnapLoc.BUILTIN);
                            });
                        } else {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "Cannot get primitive class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        }
                    })));
                })
        ));
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
                        if (!type.isPresent()) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "No java class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        } else {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "Cannot get primitive class: " + cls, null), trc, ShnapLoc.BUILTIN);
                        }
                    }
                })
        ));
        ShnapNativeFuncRegistry.register("java.javaArray", func(
                Items.buildList(param("arr"), param("class", literalObj("java.lang.Object"))),
                inst((ctx, trc) -> {
                    ShnapExecution shanpArr = ctx.get("arr", trc).mapIfNormal(e -> e.getValue().asArray(trc));
                    if (shanpArr.isAbnormal()) {
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
                        try {
                            return DeepArrays.deeplyConvertToJava((ShnapArrayNative) shanpArr.getValue(), trc, clazz);
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

        ShnapNativeFuncRegistry.register("sys.eval", oneArg(inst((ctx, trc) -> {
            return ctx.get("arg", trc).resolve(trc).mapIfNormal(e -> e.getValue().asString(trc)).mapIfNormal(e -> {
                ShnapScript eval = new ShnapScript("eval", "eval");
                trc.applyDefaults(eval);
                String str = ((ShnapStringNative) e.getValue()).getValue();
                eval.setContent(() -> str);
                ShnapParser parser = new ShnapParser(str, eval);
                try {
                    ShnapInstruction res = parser.parseAll();
                    return res.exec(eval.getContext(), trc);
                } catch (ShnapParseError k) {
                    return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParseError", "\n" + k.formatSafely(), null), trc, ShnapLoc.BUILTIN);
                }
            });
        })));

        ShnapNativeFuncRegistry.register("sys.evalIn", func(
                Items.buildList(param("arg"), param("object")),
                inst((ctx, trc) -> {
                    return ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asString(trc)).mapIfNormal(e -> ctx.get("object", trc).mapIfNormal(obj -> {
                        ShnapScript eval = new ShnapScript("eval", "eval");
                        ShnapObject target = obj.getValue();
                        String str = ((ShnapStringNative) e.getValue()).getValue();
                        eval.setContent(() -> str);
                        ShnapParser parser = new ShnapParser(str, eval);
                        try {
                            ShnapInstruction res = parser.parseAll();
                            return res.exec(target.getContext(), trc);
                        } catch (ShnapParseError k) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParseError", "\n" + k.formatSafely(), null), trc, ShnapLoc.BUILTIN);
                        }
                    }));
                })));

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

        ShnapNativeFuncRegistry.register("sys.identity", oneArg(inst((ctx, trc) -> {
            return ctx.get("arg", trc).resolve(trc).mapIfNormal(e -> {
                int hash = System.identityHashCode(e.getValue());
                return ShnapExecution.normal(new ShnapIntNative(ShnapLoc.BUILTIN, hash), trc, ShnapLoc.BUILTIN);
            });
        })));

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

        ShnapNativeFuncRegistry.register("sys.setFlag", func(
                Items.buildList(param("obj"), param("name"), param("flag")),
                inst((ctx, trc) -> ctx.get("flag", trc).mapIfNormal(flag -> flag.getValue().asString(trc).mapIfNormal(flagString -> ctx.get("obj", trc).mapIfNormal(objExec -> ctx.get("name", trc).mapIfNormal(nameExec -> {
                    ShnapObject object = objExec.getValue();
                    String name = ((ShnapStringNative) nameExec.getValue()).getValue();
                    String flagName = ((ShnapStringNative) flagString.getValue()).getValue();
                    ShnapContext.Flag flg = ShnapContext.Flag.getByRep(flagName);
                    if (flg == null) {
                        return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.AbsentFlagError", "No flag called: " + flagName, null), trc, ShnapLoc.BUILTIN);
                    }

                    object.getContext().setFlag(name, flg);
                    return ShnapExecution.normal(ShnapObject.getVoid(), trc, ShnapLoc.BUILTIN);
                })))))
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

        ShnapNativeFuncRegistry.register("sys.fieldData", oneArg(inst((ctx, trc) -> {
            return ctx.get("arg", trc).mapIfNormal(e -> {
                ShnapObject obj = e.getValue();
                Collection<String> names = obj.getContext().names();
                ShnapObject[] arr = new ShnapObject[names.size()];
                int k = 0;
                for (String s : names) {
                    ShnapObject field = new ShnapObject(ShnapLoc.BUILTIN);
                    field.set("name", new ShnapStringNative(ShnapLoc.BUILTIN, s));
                    field.set("resolvedValue", obj.get(s, trc).resolve(trc).mapIfAbnormal(ex -> ex.setState(ShnapExecution.State.NORMAL).setValue(ShnapObject.getVoid())).getValue());
                    field.set("exactValue", obj.getContext().getExactly(s));
                    for (ShnapContext.Flag flag : ShnapContext.Flag.values()) {
                        String name = flag.getRep();
                        name = "is" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        field.set(name, ShnapBooleanNative.of(obj.getContext().hasFlag(s, flag)));
                    }

                    field.set(ShnapObject.AS_STRING, ShnapFactory.noArg(ShnapFactory.literal(new ShnapStringNative(ShnapLoc.BUILTIN, "field::" + s))));
                    arr[k++] = field;
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
        ShnapNativeFuncRegistry.register("type.int", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapBigIntegerNative(ShnapLoc.BUILTIN, ShnapNumberNative.asInt(((ShnapNumberNative) number.getValue()).getNumber())), trc, ShnapLoc.BUILTIN);
        })));
        ShnapNativeFuncRegistry.register("type.dec", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapBigDecimalNative(ShnapLoc.BUILTIN, ShnapNumberNative.asDec(((ShnapNumberNative) number.getValue()).getNumber())), trc, ShnapLoc.BUILTIN);
        })));
        ShnapNativeFuncRegistry.register("type.char", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapCharNative(ShnapLoc.BUILTIN, ((ShnapNumberNative) number.getValue()).getNumber().intValue()), trc, ShnapLoc.BUILTIN);
        })));

        ShnapNativeFuncRegistry.register("type.int32", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapIntNative(ShnapLoc.BUILTIN, ((ShnapNumberNative) number.getValue()).getNumber().intValue()), trc, ShnapLoc.BUILTIN);
        })));

        ShnapNativeFuncRegistry.register("type.int64", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapLongNative(ShnapLoc.BUILTIN, ((ShnapNumberNative) number.getValue()).getNumber().longValue()), trc, ShnapLoc.BUILTIN);
        })));

        ShnapNativeFuncRegistry.register("type.float64", oneArg(inst((ctx, trc) -> {
            ShnapExecution number = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (number.isAbnormal()) {
                return number;
            }

            return ShnapExecution.normal(new ShnapDoubleNative(ShnapLoc.BUILTIN, ((ShnapNumberNative) number.getValue()).getNumber().doubleValue()), trc, ShnapLoc.BUILTIN);
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

    public static Object nullSafetyCast(Class type, Object val) {
        if (val == null && type.isPrimitive()) {
            throw new ClassCastException("Cannot cast null to " + type.getName());
        } else {
            return Reflections.deepCast(type, val);
        }
    }

}
