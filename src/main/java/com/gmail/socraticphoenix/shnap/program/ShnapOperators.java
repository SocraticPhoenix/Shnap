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
package com.gmail.socraticphoenix.shnap.program;

public enum ShnapOperators {
    NEGATIVE("-", "negate", 1, true, 100),
    BITWISE_NOT("~", "bnot", 1, true, 100),
    RAISE("**", "pow", 2, false, 95),
    MULTIPLY("*", "multiply", 2, true, 90),
    DIVIDE("/", "divide", 2, true, 90),
    REMAINDER("%", "remainder", 2, true, 90),
    ADD("+", "add", 2, true, 85),
    SUBTRACT("-", "subtract", 2, true, 85),
    LEFT_SHIFT("<<", "shiftLeft", 2, true, 80), //shifts MUST come before comparisons, as operators are checked in sequential order
    RIGHT_SHIFT(">>", "shiftRight", 2, true, 80),
    LESS_THAN_EQUAL_TO("<=", "compareTo", 2, true, true, 75), //equal to's MUST come before <, >, as operators are checked in sequential order
    GREATER_THAN_EQUAL_TO(">", "compareTo", 2, true, true, 75),
    LESS_THAN("<", "compareTo", 2, true, true, 75),
    GREATER_THAN(">", "compareTo", 2, true, true, 75),
    EQUAL("==", "compareTo", 2, true, true, 70),
    NOT_EQUAL("!=", "compareTo", 2, true, true, 70),
    NOT("!", "not", 1, true, false, true, 100), //not MUST come after !=, as operators are checked in sequential order
    LOGICAL_AND("&&", "and", 2, true, false, true, 50), //logical MUST come before bitwise, as operators are checked in sequential order
    LOGICAL_OR("||", "or", 2, true, false, true, 45),
    BITWISE_AND("&", "band", 2, true, 65),
    BITIWSE_XOR("`", "bxor", 2, true, 60),
    BITWISE_OR("|", "bor", 2, true, 55),
    SENTINEL("\0", "sentinel", 0, true, Integer.MIN_VALUE);

    private String rep;
    private String func;
    private int arity;
    private boolean leftAssociative;
    private boolean comparative;
    private int precedence;
    private boolean bool;

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, int precedence) {
        this(rep, func, arity, leftAssociative, false, precedence);
    }

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, boolean comparative, int precedence) {
        this(rep, func, arity, leftAssociative, comparative, false, precedence);
    }

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, boolean comparative, boolean bool, int precedence) {
        this.rep = rep;
        this.func = func;
        this.arity = arity;
        this.leftAssociative = leftAssociative;
        this.comparative = comparative;
        this.precedence = precedence;
        this.bool = bool;
    }

    public boolean isBool() {
        return this.bool;
    }

    public int getPrecedence() {
        return this.precedence;
    }

    public String getRep() {
        return this.rep;
    }

    public String getFunc() {
        return this.func;
    }

    public int getArity() {
        return this.arity;
    }

    public boolean isLeftAssociative() {
        return this.leftAssociative;
    }

    public boolean isComparative() {
        return this.comparative;
    }

    public static ShnapOperators fromRep(String s) {
        for(ShnapOperators op : values()) {
            if(op.getRep().equals(s)) {
                return op;
            }
        }

        return null;
    }
}
