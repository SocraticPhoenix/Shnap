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
import com.gmail.socraticphoenix.shnap.repack.org.nevec.rjm.BigDecimalMath;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapJavaBackedNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.inst;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.noArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.oneArg;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.param;

public interface ShnapNumberNative extends ShnapJavaBackedNative {
    ShnapObject ONE = ShnapNumberNative.valueOf(1);
    ShnapObject ZERO = ShnapNumberNative.valueOf(-1);


    static ShnapObject valueOf(ShnapLoc loc, Number number) {
        if (number instanceof BigInteger) {
            return new ShnapBigIntegerNative(loc, (BigInteger) number);
        } else if (number instanceof BigDecimal) {
            return new ShnapBigDecimalNative(loc, (BigDecimal) number);
        } else {
            if (number instanceof Float || number instanceof Double) {
                return new ShnapBigDecimalNative(loc, new BigDecimal(number.doubleValue()));
            } else {
                return new ShnapBigIntegerNative(loc, BigInteger.valueOf(number.longValue()));
            }
        }
    }

    static ShnapObject valueOf(Number number) {
        if (number instanceof BigInteger) {
            return new ShnapBigIntegerNative(ShnapLoc.BUILTIN, (BigInteger) number);
        } else if (number instanceof BigDecimal) {
            return new ShnapBigDecimalNative(ShnapLoc.BUILTIN, (BigDecimal) number);
        } else {
            if (number instanceof Float || number instanceof Double) {
                return new ShnapBigDecimalNative(ShnapLoc.BUILTIN, new BigDecimal(number.doubleValue()));
            } else {
                return new ShnapBigIntegerNative(ShnapLoc.BUILTIN, BigInteger.valueOf(number.longValue()));
            }
        }
    }

    Number getNumber();

    ShnapObject copyWith(Number n);

    default ShnapBooleanNative convertToBool() {
        if (this instanceof ShnapBooleanNative) {
            return (ShnapBooleanNative) this;
        } else {
            if (this.getNumber().doubleValue() == 0) {
                return ShnapBooleanNative.FALSE;
            } else {
                return ShnapBooleanNative.TRUE;
            }
        }
    }

    static void implementFunctions(ShnapObject target, ShnapNumberNative alsoTarget) {
        //Conversion functions; bool() is not needed as numbers are implicitly converted to bools if necessary
        target.set(ShnapObject.AS_STRING, noArg(instSimple(() -> new ShnapStringNative(ShnapLoc.BUILTIN, String.valueOf(alsoTarget.getNumber())))));
        target.set(ShnapObject.AS_ARRAY, noArg(instSimple(() -> new ShnapArrayNative(ShnapLoc.BUILTIN, target))));

        //Other functions
        ShnapFactory.implementOperators(target,
                noArg(instSimple(() -> alsoTarget.copyWith(negate(alsoTarget.getNumber())))),
                noArg(instSimple(() -> alsoTarget.copyWith(bitwiseNot(alsoTarget.getNumber())))),
                noArg(instSimple(() -> not(alsoTarget.getNumber()))),
                func(alsoTarget, ShnapNumberNative::pow),
                func(alsoTarget, ShnapNumberNative::multiply),
                func(alsoTarget, ShnapNumberNative::divide),
                func(alsoTarget, ShnapNumberNative::remainder),
                func(alsoTarget, ShnapNumberNative::add),
                func(alsoTarget, ShnapNumberNative::subtract),
                func(alsoTarget, ShnapNumberNative::leftShift),
                func(alsoTarget, ShnapNumberNative::rightShift),
                func(alsoTarget, ShnapNumberNative::compareTo),
                func(alsoTarget, ShnapNumberNative::bitwiseAnd),
                func(alsoTarget, ShnapNumberNative::bitwiseXor),
                func(alsoTarget, ShnapNumberNative::bitwiseOr),
                func2(alsoTarget, ShnapNumberNative::and),
                func2(alsoTarget, ShnapNumberNative::or)
        );

        target.set("round", oneArg(inst((ctx, trc) -> {
            int order = -1;
            ShnapExecution num = ctx.get("arg", trc).mapIfNormal(e -> e.getValue().asNum(trc));
            if (num.isAbnormal()) {
                return num;
            } else {
                order = ((ShnapNumberNative) num.getValue()).getNumber().intValue();
            }

            Number number = alsoTarget.getNumber();
            BigDecimal dec = number instanceof BigInteger ? new BigDecimal((BigInteger) number) : number instanceof BigDecimal ? (BigDecimal) number : new BigDecimal(number.doubleValue());
            return ShnapExecution.normal(ShnapNumberNative.valueOf(dec.setScale(order, RoundingMode.HALF_UP)), trc, ShnapLoc.BUILTIN);
        })));
    }

    static ShnapFunction func2(ShnapNumberNative target, BiFunction<Number, Number, ShnapObject> op) {
        return ShnapFactory.func(
                Items.buildList(param("arg"), param("order", ONE)),
                inst((c, t) -> {
                    try {
                        int order = 1;
                        if (c.directlyContains("order")) {
                            ShnapExecution ord = c.get("order", t).mapIfNormal(e -> e.getValue().asNum(t));
                            if (ord.isAbnormal()) {
                                return ord;
                            } else {
                                order = ((ShnapNumberNative) ord.getValue()).getNumber().intValue();
                            }
                        }

                        int finalOrder = order;
                        return c.get("arg", t).mapIfNormal(e -> {
                            ShnapObject obj = e.getValue();
                            if (obj instanceof ShnapNumberNative) {
                                ShnapNumberNative argNative = (ShnapNumberNative) obj;
                                Number left = target.getNumber();
                                Number right = ((ShnapNumberNative) obj).getNumber();
                                if (left instanceof BigDecimal && right instanceof BigDecimal) {
                                    BigDecimal leftDec = (BigDecimal) left;
                                    BigDecimal rightDec = (BigDecimal) right;
                                    int scale = Math.max(leftDec.scale(), rightDec.scale());
                                    scale = Math.max(scale, 32);
                                    left = leftDec.setScale(scale, RoundingMode.HALF_UP);
                                    right = rightDec.setScale(scale, RoundingMode.HALF_UP);
                                }

                                ShnapObject result;
                                if (finalOrder == 1) {
                                    result = op.apply(left, right);
                                } else {
                                    result = op.apply(right, left);
                                }

                                if (result instanceof ShnapNumberNative) {
                                    Number resNum = ((ShnapNumberNative) result).getNumber();
                                    if (argNative.castingPrecedence(resNum) > target.castingPrecedence(resNum)) {
                                        result = argNative.copyWith(resNum);
                                    } else {
                                        result = target.copyWith(resNum);
                                    }
                                }

                                return ShnapExecution.normal(result, t, ShnapLoc.BUILTIN);
                            }
                            return ShnapExecution.normal(ShnapObject.getVoid(), t, ShnapLoc.BUILTIN);
                        });
                    } catch (ArithmeticException e) {
                        return ShnapExecution.normal(ShnapObject.getVoid(), t, ShnapLoc.BUILTIN);
                    }
                }));
    }

    int castingPrecedence(Number result);

    static ShnapFunction func(ShnapNumberNative target, BinaryOperator<Number> op) {
        return func2(target, (n1, n2) -> target.copyWith(op.apply(n1, n2)));
    }

    static Number negate(Number number) {
        return operate(number, BigInteger::negate, BigDecimal::negate);
    }

    static Number bitwiseNot(Number number) {
        return operate(number, BigInteger::not, d -> new BigDecimal(d.unscaledValue().not(), d.scale()));
    }

    static ShnapObject not(Number number) {
        if (number.doubleValue() == 0) {
            return ShnapBooleanNative.TRUE;
        } else {
            return ShnapBooleanNative.FALSE;
        }
    }

    static Number multiply(Number a, Number b) {
        return operate(a, n -> operate(b, n::multiply, d -> d.multiply(new BigDecimal(n))),
                d -> operate(b, n -> d.multiply(new BigDecimal(n)), d::multiply));
    }

    static Number divide(Number a, Number b) {
        return operate(a, n -> operate(b, n::divide, d -> {
                    BigDecimal d2 = new BigDecimal(n);
                    return d2.divide(d, ctx(d2, d));
                }),
                d -> operate(b, n -> {
                    BigDecimal d2 = new BigDecimal(n);
                    return d.divide(d2, ctx(d, d2));
                }, d2 -> d.divide(d2, ctx(d, d2))));
    }

    static Number remainder(Number a, Number b) {
        return operate(a, n -> operate(b, n::remainder, d -> {
                    BigDecimal d2 = new BigDecimal(n);
                    return d2.remainder(d, ctx2(d2, d));
                }),
                d -> operate(b, n -> {
                    BigDecimal d2 = new BigDecimal(n);
                    return d.remainder(d2, ctx2(d, d2));
                }, d2 -> d.remainder(d2, ctx2(d, d2))));
    }

    static Number add(Number a, Number b) {
        return operate(a, n -> operate(b, n::add, d -> new BigDecimal(n).add(d)),
                d -> operate(b, n -> d.add(new BigDecimal(n)), d::divide));
    }

    static Number subtract(Number a, Number b) {
        return operate(a, n -> operate(b, n::subtract, d -> new BigDecimal(n).subtract(d)),
                d -> operate(b, n -> d.subtract(new BigDecimal(n)), d::subtract));
    }

    static Number myPow(BigDecimal a, BigDecimal b) {
        if (a.compareTo(BigDecimal.ZERO) == 0 || a.compareTo(BigDecimal.ONE) == 0) {
            return a.toBigInteger();
        } else if (b.compareTo(BigDecimal.ZERO) == 0) {
            return BigInteger.ONE;
        }

        if (a.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("I don't know how to raise a negative number to a decimal power...");
        }
        return BigDecimalMath.exp(b.multiply(BigDecimalMath.log(a)));
    }

    static Number intPower(BigDecimal a, BigInteger b) {
        boolean negate = false;
        if (a.compareTo(BigDecimal.ZERO) < 0) {
            a = a.negate();
            negate = b.mod(TWO).compareTo(BigInteger.ZERO) != 0;
        }

        BigDecimal res = BigDecimal.ONE;
        for (BigInteger i = BigInteger.ZERO; i.compareTo(b) < 0; i = i.add(BigInteger.ONE)) {
            res = res.multiply(res);
        }
        return negate ? res.negate() : res;
    }

    static BigInteger TWO = new BigInteger("2");

    static BigInteger intIntPower(BigInteger a, BigInteger b) {
        boolean negate = false;
        if (a.compareTo(BigInteger.ZERO) < 0) {
            a = a.negate();
            negate = b.mod(TWO).compareTo(BigInteger.ZERO) != 0;
        }

        BigInteger res = BigInteger.ONE;
        while (b.compareTo(BigInteger.ZERO) != 0) {
            if ((b.and(BigInteger.ONE)).compareTo(BigInteger.ONE) == 0) {
                res = res.multiply(a);
            }
            b = b.shiftRight(1);
            a = a.multiply(a);
        }

        return negate ? res.negate() : res;
    }

    static boolean isInt(BigDecimal dec) {
        try {
            dec.toBigIntegerExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }


    static Number pow(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> intIntPower(n, n2), d -> myPow(new BigDecimal(n), d)),
                d -> operate(b, n -> intPower(d, n), d2 -> myPow(d, d2)));
    }

    static Number leftShift(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> n.shiftLeft(n.intValue()), d -> new BigDecimal(n.shiftLeft(d.intValue()))),
                d -> operate(b, n -> new BigDecimal(d.unscaledValue().shiftLeft(n.intValue()), d.scale()), d2 -> new BigDecimal(d.unscaledValue().shiftLeft(d2.intValue()), d.scale())));
    }

    static Number rightShift(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> n.shiftRight(n.intValue()), d -> new BigDecimal(n.shiftRight(d.intValue()))),
                d -> operate(b, n -> new BigDecimal(d.unscaledValue().shiftRight(n.intValue()), d.scale()), d2 -> new BigDecimal(d.unscaledValue().shiftRight(d2.intValue()), d.scale())));
    }

    static Number compareTo(Number a, Number b) {
        return BigInteger.valueOf(operate(a, n -> operate(b, n::compareTo, d -> new BigDecimal(n).compareTo(d)),
                d -> operate(b, n -> d.compareTo(new BigDecimal(n)), d::compareTo)).longValue());
    }

    static Number bitwiseAnd(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> n.and(n), d -> new BigDecimal(n.and(d.unscaledValue()), d.scale())),
                d -> operate(b, n -> new BigDecimal(d.unscaledValue().and(n), d.scale()), d2 -> {
                    int scale = Math.max(d.scale(), d2.scale());
                    return new BigDecimal(d.setScale(scale, RoundingMode.HALF_UP).unscaledValue().and(d2.setScale(scale, RoundingMode.HALF_UP).unscaledValue()), scale);
                }));
    }

    static Number bitwiseOr(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> n.or(n), d -> new BigDecimal(n.or(d.unscaledValue()), d.scale())),
                d -> operate(b, n -> new BigDecimal(d.unscaledValue().or(n), d.scale()), d2 -> {
                    int scale = Math.max(d.scale(), d2.scale());
                    return new BigDecimal(d.setScale(scale, RoundingMode.HALF_UP).unscaledValue().or(d2.setScale(scale, RoundingMode.HALF_UP).unscaledValue()), scale);
                }));
    }

    static Number bitwiseXor(Number a, Number b) {
        return operate(a, n -> operate(b, n2 -> n.xor(n), d -> new BigDecimal(n.xor(d.unscaledValue()), d.scale())),
                d -> operate(b, n -> new BigDecimal(d.unscaledValue().xor(n), d.scale()), d2 -> {
                    int scale = Math.max(d.scale(), d2.scale());
                    return new BigDecimal(d.setScale(scale, RoundingMode.HALF_UP).unscaledValue().xor(d2.setScale(scale, RoundingMode.HALF_UP).unscaledValue()), scale);
                }));
    }

    static ShnapObject and(Number a, Number b) {
        if (a.doubleValue() != 0 && b.doubleValue() != 0) {
            return ShnapBooleanNative.TRUE;
        }

        return ShnapBooleanNative.FALSE;
    }

    static ShnapObject or(Number a, Number b) {
        if (a.doubleValue() != 0 || b.doubleValue() != 0) {
            return ShnapBooleanNative.TRUE;
        }

        return ShnapBooleanNative.FALSE;
    }

    static MathContext ctx(BigDecimal a, BigDecimal b) {
        return new MathContext((int) Math.min(a.precision() +
                        (long) Math.ceil(10.0 * b.precision() / 3.0),
                Integer.MAX_VALUE),
                RoundingMode.HALF_UP);
    }

    static MathContext ctx2(BigDecimal a, BigDecimal b) {
        int maxDigits = (int) Math.min(a.precision() +
                        (long) Math.ceil(10.0 * b.precision() / 3.0) +
                        Math.abs((long) a.scale() - b.scale()) + 2,
                Integer.MAX_VALUE);
        return new MathContext(maxDigits, RoundingMode.HALF_UP);
    }

    static Number operate(Number number,
                          Function<BigInteger, Number> ifInt,
                          Function<BigDecimal, Number> ifDec) {
        if (number instanceof BigInteger) {
            return ifInt.apply((BigInteger) number);
        } else if (number instanceof BigDecimal) {
            return ifDec.apply((BigDecimal) number);
        } else {
            return ifDec.apply(new BigDecimal(number.doubleValue()));
        }
    }

    static ShnapObject operate2(Number number,
                                Function<BigInteger, ShnapObject> ifInt,
                                Function<BigDecimal, ShnapObject> ifDec) {
        if (number instanceof BigInteger) {
            return ifInt.apply((BigInteger) number);
        } else if (number instanceof BigDecimal) {
            return ifDec.apply((BigDecimal) number);
        } else {
            return ifDec.apply(new BigDecimal(number.doubleValue()));
        }
    }

}