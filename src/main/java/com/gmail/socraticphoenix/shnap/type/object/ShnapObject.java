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
package com.gmail.socraticphoenix.shnap.type.object;

import com.gmail.socraticphoenix.collect.Items;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapAbsentNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapJavaBackedNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public class ShnapObject extends AbstractShnapLocatable {
    public static final String AS_STRING = "asString";
    public static final String AS_NUMBER = "asNumber";
    public static final String AS_BOOLEAN = "asBoolean";
    public static final String AS_ARRAY = "asArray";
    public static final String AS_JAVA = "asJava";

    protected ShnapContext context;

    public ShnapObject(ShnapLoc loc) {
        super(loc);
        this.context = new ShnapContext();
        context.setLocally("this", this);
        context.setFlag("this", ShnapContext.Flag.DONT_IMPORT);
    }

    public ShnapObject(ShnapLoc loc, ShnapContext context) {
        super(loc);
        this.context = context;
    }

    public ShnapExecution resolve(ShnapEnvironment trc) {
        return ShnapExecution.normal(this, trc, this.getLocation());
    }

    public static ShnapObject getNull() {
        return ShnapAbsentNative.NULL;
    }

    public static ShnapObject getVoid() {
        return ShnapAbsentNative.VOID;
    }

    public void init(ShnapContext context) {
        this.context = ShnapContext.childOf(context);
        context.set("this", this);
        context.setFlag("this", ShnapContext.Flag.DONT_IMPORT);
    }

    public ShnapContext getContext() {
        return this.context;
    }

    public ShnapExecution get(String name, ShnapEnvironment trc) {
        return this.context.get(name, trc);
    }

    public void set(String name, ShnapObject val) {
        this.context.set(name, val);
    }

    /**
     * Returns an execution which is either abnormal (in case of failure), or has a value that is an instance of ShnapJavaBackedNative
     */
    public ShnapExecution asJava(ShnapEnvironment tracer) {
        if (this instanceof ShnapJavaBackedNative) {
            return ShnapExecution.normal(this, tracer, this.getLocation());
        } else {
            return this.get(AS_JAVA, tracer).mapIfNormal(func -> {
                if (func.getValue() instanceof ShnapFunction) {
                    ShnapFunction function = (ShnapFunction) func.getValue();
                    if (function.paramSizeId() == 0) {
                        ShnapExecution e = function.invoke(tracer);
                        if (!e.isAbnormal() && !(e.getValue() instanceof ShnapJavaBackedNative)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ReturnTypeError", AS_JAVA + "() function returned a non-java object (" + e.getValue().getClass().getSimpleName() + ")", null, "shnap.TypeError"), tracer, this.getLocation());
                        }
                        return e;
                    }
                }
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "object cannot be converted to java", null), tracer, this.getLocation());
            });
        }
    }

    /**
     * Returns an execution which is either abnormal (in case of failure), or has a value that is an instance of ShnapNumberNative
     */
    public ShnapExecution asNum(ShnapEnvironment tracer) {
        if (this instanceof ShnapNumberNative) {
            return ShnapExecution.normal(this, tracer, this.getLocation());
        } else {
            return this.get(AS_NUMBER, tracer).mapIfNormal(func -> {
                if (func.getValue() instanceof ShnapFunction) {
                    ShnapFunction function = (ShnapFunction) func.getValue();
                    if (function.paramSizeId() == 0) {
                        ShnapExecution e = function.invoke(tracer);
                        if (!e.isAbnormal() && !(e.getValue() instanceof ShnapNumberNative)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ReturnTypeError", AS_NUMBER + "() function returned a non-number object (" + e.getValue().getClass().getSimpleName() + ")", null, "shnap.TypeError"), tracer, this.getLocation());
                        }
                        return e;
                    }
                }
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "object cannot be converted to a number", null), tracer, this.getLocation());
            });
        }
    }

    /**
     * Returns an execution which is either abnormal (in case of failure), or has a value that is an instance of ShnapStringNative
     */
    public ShnapExecution asString(ShnapEnvironment tracer) {
        if (this instanceof ShnapStringNative) {
            return ShnapExecution.normal(this, tracer, this.getLocation());
        } else {
            return this.get(AS_STRING, tracer).mapIfNormal(func -> {
                if (func.getValue() instanceof ShnapFunction) {
                    ShnapFunction function = (ShnapFunction) func.getValue();
                    if (function.paramSizeId() == 0) {
                        ShnapExecution e = function.invoke(tracer);
                        if (!e.isAbnormal() && !(e.getValue() instanceof ShnapStringNative)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ReturnTypeError", AS_STRING + "() function returned a non-string object (" + e.getValue().getClass().getSimpleName() + ")", null, "shnap.TypeError"), tracer, this.getLocation());
                        }
                        return e;
                    }
                }
                return ShnapExecution.normal(new ShnapStringNative(ShnapLoc.BUILTIN, this.defaultToString()), tracer, this.getLocation());
            });
        }
    }

    public String defaultToString() {
        return "obj::" + this.identityStr();
    }

    public String identityStr() {
        return Integer.toString(System.identityHashCode(this), 16);
    }

    /**
     * Returns an execution which is either abnormal (in case of failure), or has a value that is an instance of ShnapArrayNative
     */
    public ShnapExecution asArray(ShnapEnvironment tracer) {
        if (this instanceof ShnapArrayNative) {
            return ShnapExecution.normal(this, tracer, this.getLocation());
        } else {
            return this.get(AS_ARRAY, tracer).mapIfNormal(func -> {
                if (func.getValue() instanceof ShnapFunction) {
                    ShnapFunction function = (ShnapFunction) func.getValue();
                    if (function.paramSizeId() == 0) {
                        ShnapExecution e = function.invoke(tracer);
                        if (!e.isAbnormal() && !(e.getValue() instanceof ShnapArrayNative)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ReturnTypeError", AS_ARRAY + "() function returned a non-array object (" + e.getValue().getClass().getSimpleName() + ")", null, "shnap.TypeError"), tracer, this.getLocation());
                        }
                        return e;
                    }
                }
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.TypeError", "object cannot be converted to array", null), tracer, this.getLocation());
            });
        }
    }

    /**
     * Returns an execution which is either abnormal (in case of failure), or has a value that is an instance of ShnapBooleanNative
     */
    public ShnapExecution asBool(ShnapEnvironment tracer) {
        if (this instanceof ShnapBooleanNative) {
            return ShnapExecution.normal(this, tracer, this.getLocation());
        } else {
            return this.get(AS_BOOLEAN, tracer).mapIfNormal(func -> {
                if (func.getValue() instanceof ShnapFunction) {
                    ShnapFunction function = (ShnapFunction) func.getValue();
                    if (function.paramSizeId() == 0) {
                        ShnapExecution e = function.invoke(tracer);
                        if (!e.isAbnormal() && !(e.getValue() instanceof ShnapBooleanNative)) {
                            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ReturnTypeError", AS_BOOLEAN + "() function returned a non-java object (" + e.getValue().getClass().getSimpleName() + ")", null, "shnap.TypeError"), tracer, this.getLocation());
                        }
                        return e;
                    }
                }
                return this.asNum(tracer).mapIfNormal(e -> e.getValue().asBool(tracer));
            });
        }
    }

    /**
     * Uses {@link ShnapObject#asBool(ShnapEnvironment)} and returns false if the conversion failed, or the java boolean value of the conversion
     */
    public boolean isTruthy(ShnapEnvironment tracer) {
        ShnapExecution exec = this.asBool(tracer);
        return !exec.isAbnormal() && ((ShnapBooleanNative) exec.getValue()).getValue();
    }

    /**
     * Attempts to operate on this and the given argument, using the operator {@link ShnapOperators#EQUAL}. Returns true if the objects are equal, or false if they are unequal or the operation failed
     */
    public boolean isEqualTo(ShnapObject other, ShnapEnvironment tracer) {
        ShnapExecution res = this.operate(other, ShnapOperators.EQUAL, tracer);
        return !res.isAbnormal() && res.getValue() instanceof ShnapBooleanNative && ((ShnapBooleanNative) res.getValue()).getValue();
    }

    public ShnapExecution operate(ShnapOperators operator, ShnapEnvironment tracer) {
        if (operator.getArity() != 1) {
            throw new IllegalArgumentException("Arity of " + operator.getArity() + ", required 1");
        }

        return this.operate(null, operator, tracer);
    }

    public ShnapExecution operate(ShnapObject arg, ShnapOperators operator, ShnapEnvironment tracer) {
        if (this == arg && (operator == ShnapOperators.EQUAL || operator == ShnapOperators.NOT_EQUAL)) {
            return ShnapExecution.normal(ShnapBooleanNative.of(operator == ShnapOperators.EQUAL), tracer, this.getLocation());
        }

        String func = operator.getFunc();
        int args = operator.getArity() - 1;
        ShnapExecution res;

        if (args == 0) {
            ShnapExecution f1 = this.get(func, tracer);
            res = f1.mapIfNormal(e -> {
                if (e.getValue() instanceof ShnapFunction && ((ShnapFunction) e.getValue()).paramSizeId() == 1) {
                    return ((ShnapFunction) e.getValue()).invokeOperator(new ArrayList<>(), 1, tracer);
                }
                return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
            });
        } else {
            ShnapExecution f1 = this.get(func, tracer);
            res = f1.mapIfNormal(e -> {
                if (e.getValue() instanceof ShnapFunction && ((ShnapFunction) e.getValue()).paramSizeId() == 1) {
                    return ((ShnapFunction) e.getValue()).invokeOperator(Items.buildList(arg), 1, tracer);
                }
                return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
            });

            if (res.isAbnormal() || res.getValue() == ShnapObject.getVoid()) {
                ShnapExecution f2 = arg.get(func, tracer);
                ShnapExecution callRes = f2.mapIfNormal(e -> {
                    if (e.getValue() instanceof ShnapFunction && ((ShnapFunction) e.getValue()).paramSizeId() == 1) {
                        return ((ShnapFunction) e.getValue()).invokeOperator(Items.buildList(this), 2, tracer);
                    }
                    return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
                });

                if (!callRes.isAbnormal()) {
                    res = callRes;
                }
            }
        }

        if (res.isAbnormal()) {
            return res;
        } else if (res.getValue() == ShnapObject.getVoid()) {
            return res;
        }

        if (operator.isComparative()) {
            boolean val = false;
            ShnapObject resObj = res.getValue();
            if (resObj instanceof ShnapNumberNative) {
                ShnapNumberNative numVal = (ShnapNumberNative) resObj;
                int resVal = (int) ShnapNumberNative.operate(numVal.getNumber(),
                        n -> n.compareTo(BigInteger.ZERO),
                        d -> d.compareTo(BigDecimal.ZERO));
                switch (operator) {
                    case LESS_THAN:
                        val = resVal < 0;
                        break;
                    case GREATER_THAN:
                        val = resVal > 0;
                        break;
                    case LESS_THAN_EQUAL_TO:
                        val = resVal <= 0;
                        break;
                    case GREATER_THAN_EQUAL_TO:
                        val = resVal >= 0;
                        break;
                    case EQUAL:
                        val = resVal == 0;
                        break;
                    case NOT_EQUAL:
                        val = resVal != 0;
                        break;
                }
            }
            return ShnapExecution.normal(ShnapBooleanNative.of(val), tracer, this.getLocation());
        } else {
            return res;
        }
    }

}