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
    NEGATIVE("-", "negate", 1, true, 100, false),
    BITWISE_NOT("~", "bnot", 1, true, 100, false),
    RANGE("..", "rangeTo", 2, false, 97, true),
    RAISE("**", "pow", 2, false, 95, true),
    MULTIPLY("*", "multiply", 2, true, 90, true),
    DIVIDE("/", "divide", 2, true, 90, true),
    REMAINDER("%", "remainder", 2, true, 90, true),
    ADD("+", "add", 2, true, 85, true),
    SUBTRACT("-", "subtract", 2, true, 85, true),
    COMPARE_TO("<>", "compareTo", 2, true, 50, true),
    LEFT_SHIFT("<<", "shiftLeft", 2, true, 80, true), //shifts MUST come before comparisons, as operators are checked in sequential order
    RIGHT_SHIFT(">>", "shiftRight", 2, true, 80, true),
    LESS_THAN_EQUAL_TO("<=", "compareTo", 2, true, true, 75, false), //equal to's MUST come before <, >, as operators are checked in sequential order
    GREATER_THAN_EQUAL_TO(">=", "compareTo", 2, true, true, 75, false),
    LESS_THAN("<", "compareTo", 2, true, true, 75, false),
    GREATER_THAN(">", "compareTo", 2, true, true, 75, false),
    EQUAL_EXACTLY("===", "same", 2, true, false, 70, false),
    EQUAL("==", "equals", 2, true, false, 70, false),
    NOT_EQUAL_EXACTLY("!==", "same", 2, true, false, 70, false),
    NOT_EQUAL("!=", "equals", 2, true, false, 70, false),
    NOT("!", "not", 1, true, false, true, 100, false), //not MUST come after !=, as operators are checked in sequential order
    LOGICAL_AND("&&", "and", 2, true, false, true, 50, true), //logical MUST come before bitwise, as operators are checked in sequential order
    LOGICAL_OR("||", "or", 2, true, false, true, 45, true),
    BITWISE_AND("&", "band", 2, true, 65, true),
    BITIWSE_XOR("`", "bxor", 2, true, 60, true),
    BITWISE_OR("|", "bor", 2, true, 55, true),
    MAP(":::", "map", 2, true, 50, true),
    FOR_EACH("::", "forEach", 2, true, 50, true),
    SENTINEL("\0", "sentinel", 0, true, Integer.MIN_VALUE, false);

    private String rep;
    private String func;
    private int arity;
    private boolean leftAssociative;
    private boolean comparative;
    private int precedence;
    private boolean bool;
    private boolean set;

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, int precedence, boolean set) {
        this(rep, func, arity, leftAssociative, false, precedence, set);
    }

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, boolean comparative, int precedence, boolean set) {
        this(rep, func, arity, leftAssociative, comparative, false, precedence, set);
    }

    ShnapOperators(String rep, String func, int arity, boolean leftAssociative, boolean comparative, boolean bool, int precedence, boolean set) {
        this.rep = rep;
        this.func = func;
        this.arity = arity;
        this.leftAssociative = leftAssociative;
        this.comparative = comparative;
        this.precedence = precedence;
        this.bool = bool;
        this.set = set;
    }

    public boolean isSet() {
        return this.set;
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
