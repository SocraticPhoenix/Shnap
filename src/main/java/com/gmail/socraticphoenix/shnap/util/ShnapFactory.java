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
package com.gmail.socraticphoenix.shnap.util;

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.ShnapParameter;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapGet;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstructionSequence;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInvoke;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapLiteral;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapMakeFunc;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapMakeObj;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapNativeInstruction;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapOperate;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapSet;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapStateChange;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapForBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapIfBlock;
import com.gmail.socraticphoenix.shnap.program.instructions.block.ShnapScopeBlock;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.ADD;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.BITIWSE_XOR;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.BITWISE_AND;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.BITWISE_NOT;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.BITWISE_OR;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.DIVIDE;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.EQUAL;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.GREATER_THAN;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.LEFT_SHIFT;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.MULTIPLY;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.NEGATIVE;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.RAISE;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.REMAINDER;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.RIGHT_SHIFT;
import static com.gmail.socraticphoenix.shnap.program.ShnapOperators.SUBTRACT;

public interface ShnapFactory {

    static ShnapInstruction makeClass(ShnapLoc loc, List<ShnapParameter> params, ShnapInstruction body, List<ShnapInstruction> clsInsts) {
        return new ShnapMakeFunc(loc, params, clsInsts, new ShnapInstructionSequence(loc, Items.buildList(new ShnapStateChange(loc, State.RETURNING, new ShnapMakeObj(body.getLocation(), body)))));
    }

    static ShnapInstruction makeGet(ShnapLoc loc, ShnapInstruction target, List<ShnapInstruction> params, Map<String, ShnapInstruction> defParams) {
        return new ShnapInvoke(loc,  new ShnapGet(loc, target, "get"), params, defParams);
    }

    static ShnapInstruction makeSet(ShnapLoc loc, ShnapInstruction target, List<ShnapInstruction> params, Map<String, ShnapInstruction> defParams, ShnapInstruction val) {
        params.add(0, val);
        return new ShnapInvoke(loc,  new ShnapGet(loc, target, "set"), params, defParams);
    }

    static ShnapInstruction makeSliceGet(ShnapLoc loc, ShnapInstruction target, List<ShnapInstruction> params, Map<String, ShnapInstruction> defParams) {
        return new ShnapInvoke(loc,  new ShnapGet(loc, target, "getSlice"), params, defParams);
    }

    static ShnapInstruction makeSliceSet(ShnapLoc loc, ShnapInstruction target, List<ShnapInstruction> params, Map<String, ShnapInstruction> defParams, ShnapInstruction val) {
        params.add(0, val);
        return new ShnapInvoke(loc,  new ShnapGet(loc, target, "setSlice"), params, defParams);
    }

    static ShnapObject mimicJavaException(String name, String message, Throwable throwable) {
        ShnapObject initial = makeExceptionObj(name, message, null, "shnap.JavaError");
        initial.set("cause", mimicJavaException(throwable));
        return initial;
    }

    static ShnapObject mimicJavaException(Throwable e) {
        ShnapObject initial = makeExceptionObj(e.getClass().getName(), e.getMessage(), null, getAllParents(e.getClass()));
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            initial.set("cause", makeExceptionObj(cause.getClass().getName(), cause.getMessage(), null, getAllParents(cause.getClass())));
        }
        return initial;
    }

    static String[] getAllParents(Class cls) {
        Set<String> parents = new HashSet<>();
        if (cls.getSuperclass() != null) {
            Class sup = cls.getSuperclass();
            parents.add(sup.getName());
            Collections.addAll(parents, getAllParents(sup));
        }

        for (Class inter : cls.getInterfaces()) {
            parents.add(inter.getName());
            Collections.addAll(parents, getAllParents(inter));
        }
        return parents.toArray(new String[parents.size()]);
    }

    static ShnapObject makeExceptionObj(String name, String message, ShnapObject cause, String... parents) {
        ShnapObject[] actualParents = new ShnapObject[parents.length + 1];
        for (int i = 0; i < parents.length; i++) {
            actualParents[i] = new ShnapStringNative(ShnapLoc.BUILTIN, parents[i]);
        }
        actualParents[actualParents.length - 1] = new ShnapStringNative(ShnapLoc.BUILTIN, "shnap.Error");

        ShnapObject err = new ShnapObject(ShnapLoc.BUILTIN);
        err.set("isError", literalObj(true));
        err.set("name", new ShnapStringNative(ShnapLoc.BUILTIN, name));
        err.set("message", message == null ? ShnapObject.getNull() : new ShnapStringNative(ShnapLoc.BUILTIN, message));
        err.set("cause", cause == null ? ShnapObject.getNull() : cause);
        err.set("parents", new ShnapArrayNative(ShnapLoc.BUILTIN, actualParents));
        err.set(ShnapObject.AS_STRING, noArg(inst((ctx, trc) -> {
            return err.get("name", trc).mapIfNormal(nameE -> err.get("message", trc).mapIfNormal(messageE -> {
                ShnapExecution nStr = nameE.getValue().asString(trc);
                ShnapExecution mStr = messageE.getValue().asString(trc);

                if (nStr.isAbnormal()) {
                    return nStr;
                } else if (mStr.isAbnormal()) {
                    return mStr;
                } else {
                    String n = ((ShnapStringNative) nStr.getValue()).getValue();
                    String m = ((ShnapStringNative) mStr.getValue()).getValue();

                    String result = n + ": " + m;

                    ShnapExecution cas = err.get("cause", trc);
                    if (cas.isAbnormal()) {
                        return cas;
                    } else if (cas.getValue() != ShnapObject.getNull()) {
                        ShnapExecution casStr = cas.getValue().asString(trc);
                        if (casStr.isAbnormal()) {
                            return casStr;
                        } else {
                            result += ", caused by:\n" + ((ShnapStringNative) casStr.getValue()).getValue();
                        }
                    } else {
                        result += "\n";
                    }

                    return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, result), trc, ShnapLoc.BUILTIN);
                }
            }));
        })));
        err.set("is", funcExactly(Items.buildList(param("type")), sequence(
                ifTrue(operate(operate(get("cause"), ShnapOperators.NOT_EQUAL, literal(ShnapObject.getNull())), ShnapOperators.LOGICAL_AND, invoke(get("cause", "is"), get("type"))),
                        returning(literal(true))
                ),
                ifTrue(operate(get("type"), ShnapOperators.EQUAL, get("name")),
                        returning(literal(true))
                ),
                ifTrue(
                        operate(get("type", "parents"), ShnapOperators.NOT_EQUAL, literal(ShnapObject.getVoid())),
                        sequence(
                                ifTrue(
                                        operate(operate(get("other", "iterator"), ShnapOperators.NOT_EQUAL, literal(ShnapObject.getVoid())), ShnapOperators.LOGICAL_OR, operate(operate(get("other", "hasNext"), ShnapOperators.NOT_EQUAL, literal(ShnapObject.getVoid())), ShnapOperators.LOGICAL_AND, operate(get("other", "next"), ShnapOperators.NOT_EQUAL, literal(ShnapObject.getVoid())))),
                                        sequence(
                                                forBlock("otherType", get("other"), sequence(
                                                        ifTrue(
                                                                invoke(get("parents", "contains"), get("type")),
                                                                returning(literal(true)))
                                                )),
                                                returning(literal(false))
                                        )))),
                returning(invoke(get("parents", "contains"), get("type")))
        )).init(err));
        return err;
    }

    static ShnapInstruction invoke(ShnapInstruction target, ShnapInstruction... params) {
        return new ShnapInvoke(ShnapLoc.BUILTIN, target, Items.buildList(params), Collections.emptyMap());
    }

    static ShnapInstruction returning(ShnapInstruction val) {
        return new ShnapStateChange(ShnapLoc.BUILTIN, State.RETURNING, val);
    }

    static ShnapInstruction set(String name, ShnapInstruction value) {
        return new ShnapSet(ShnapLoc.BUILTIN, null, name, value, null);
    }

    static ShnapInstruction forBlock(String var, ShnapInstruction iter, ShnapInstruction block) {
        return new ShnapForBlock(ShnapLoc.BUILTIN, null, var, iter, block);
    }

    static ShnapInstruction sequence(ShnapInstruction... insts) {
        return new ShnapInstructionSequence(ShnapLoc.BUILTIN, Items.buildList(insts));
    }

    static ShnapInstruction scoped(ShnapInstruction... insts) {
        return new ShnapScopeBlock(ShnapLoc.BUILTIN, null, sequence(insts));
    }

    static ShnapInstruction literal(ShnapObject obj) {
        return new ShnapLiteral(ShnapLoc.BUILTIN, obj == null ? ShnapObject.getNull() : obj);
    }

    static ShnapInstruction get(String target, String name) {
        return new ShnapGet(ShnapLoc.BUILTIN, get(target), name);
    }

    static ShnapIfBlock ifTrue(ShnapInstruction val, ShnapInstruction ifTrue) {
        return new ShnapIfBlock(ShnapLoc.BUILTIN, null, val, ifTrue, null);
    }

    static ShnapIfBlock branch(ShnapInstruction val, ShnapInstruction ifTrue, ShnapInstruction ifFalse) {
        return new ShnapIfBlock(ShnapLoc.BUILTIN, null, val, ifTrue, new ShnapIfBlock(ShnapLoc.BUILTIN, null, null, ifFalse, null));
    }

    static ShnapInstruction literal(Object obj) {
        return new ShnapLiteral(ShnapLoc.BUILTIN, literalObj(obj));
    }

    static ShnapObject literalObj(Object obj) {
        if (obj instanceof Boolean) {
            return ShnapBooleanNative.of((boolean) obj);
        } else if (obj instanceof Number) {
            return ShnapNumberNative.valueOf((Number) obj);
        } else if (obj instanceof String) {
            return new ShnapStringNative(ShnapLoc.BUILTIN, (String) obj);
        } else if (obj == null) {
            return ShnapObject.getNull();
        } else {
            throw new IllegalArgumentException("Unknown literal: " + obj);
        }
    }

    static ShnapGet get(String name) {
        return new ShnapGet(ShnapLoc.BUILTIN, null, name);
    }

    static ShnapOperate operate(ShnapInstruction left, ShnapOperators operator, ShnapInstruction right) {
        return new ShnapOperate(ShnapLoc.BUILTIN, left, operator, right);
    }

    static ShnapFunction func(List<ShnapParameter> params, ShnapInstruction instruction) {
        return new ShnapFunction(ShnapLoc.BUILTIN, params, new ShnapStateChange(ShnapLoc.BUILTIN, State.RETURNING, instruction));
    }

    static ShnapFunction funcExactly(List<ShnapParameter> params, ShnapInstruction instruction) {
        return new ShnapFunction(ShnapLoc.BUILTIN, params, instruction);
    }

    static ShnapFunction oneArg(ShnapInstruction instruction) {
        return ShnapFactory.func(Items.buildList(ShnapFactory.param("arg")), instruction);
    }

    static ShnapFunction noArg(ShnapInstruction instruction) {
        return ShnapFactory.func(Items.buildList(), instruction);
    }

    static ShnapInstruction inst(BiFunction<ShnapContext, ShnapEnvironment, ShnapExecution> exec) {
        return new ShnapNativeInstruction(ShnapLoc.BUILTIN, exec);
    }

    static ShnapInstruction inst(Supplier<ShnapExecution> exec) {
        return inst((c, t) -> exec.get());
    }

    static ShnapInstruction instSimple(Supplier<ShnapObject> exec) {
        return inst((ctx, trc) -> ShnapExecution.normal(exec.get(), trc, ShnapLoc.BUILTIN));
    }

    static ShnapParameter param(String name) {
        return new ShnapParameter(ShnapLoc.BUILTIN, name);
    }

    static ShnapParameter param(String name, ShnapObject def) {
        return new ShnapParameter(ShnapLoc.BUILTIN, name, new ShnapLiteral(ShnapLoc.BUILTIN, def));
    }

    static ShnapParameter param(String name, ShnapInstruction inst) {
        return new ShnapParameter(ShnapLoc.BUILTIN, name, inst);
    }

    static void implementOperators(ShnapObject target,
                                   ShnapFunction negate,
                                   ShnapFunction bitwiseNot,
                                   ShnapFunction raise,
                                   ShnapFunction multiply,
                                   ShnapFunction divide,
                                   ShnapFunction remainder,
                                   ShnapFunction add,
                                   ShnapFunction subtract,
                                   ShnapFunction leftShift,
                                   ShnapFunction rightShift,
                                   ShnapFunction compareTo,
                                   ShnapFunction equals,
                                   ShnapFunction bitwiseAnd,
                                   ShnapFunction bitwiseXor,
                                   ShnapFunction bitwiseOr
    ) {
        implementOperator(target, NEGATIVE, negate);
        implementOperator(target, BITWISE_NOT, bitwiseNot);
        implementOperator(target, RAISE, raise);
        implementOperator(target, MULTIPLY, multiply);
        implementOperator(target, DIVIDE, divide);
        implementOperator(target, REMAINDER, remainder);
        implementOperator(target, ADD, add);
        implementOperator(target, SUBTRACT, subtract);
        implementOperator(target, LEFT_SHIFT, leftShift);
        implementOperator(target, RIGHT_SHIFT, rightShift);
        implementOperator(target, GREATER_THAN, compareTo);
        implementOperator(target, EQUAL, equals);
        implementOperator(target, BITWISE_OR, bitwiseOr);
        implementOperator(target, BITWISE_AND, bitwiseAnd);
        implementOperator(target, BITIWSE_XOR, bitwiseXor);
    }

    static void implementOperator(ShnapObject target, ShnapOperators operator, ShnapFunction function) {
        target.getContext().set(operator.getFunc(), function);
    }

}
