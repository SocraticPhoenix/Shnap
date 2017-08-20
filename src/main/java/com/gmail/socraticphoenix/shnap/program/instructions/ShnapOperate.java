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
package com.gmail.socraticphoenix.shnap.program.instructions;

import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapBooleanNative;

public class ShnapOperate extends AbstractShnapLocatable implements ShnapInstruction {
    private ShnapInstruction left;
    private ShnapOperators operator;
    private ShnapInstruction right;

    public ShnapOperate(ShnapLoc loc, ShnapInstruction left, ShnapOperators operator, ShnapInstruction right) {
        super(loc);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ShnapInstruction getLeft() {
        return this.left;
    }

    public ShnapOperators getOperator() {
        return this.operator;
    }

    public ShnapInstruction getRight() {
        return this.right;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapExecution left = this.left.exec(context, tracer).resolve(tracer);
        if (left.isAbnormal()) {
            return left;
        }
        if (this.right != null && this.operator.getArity() == 2) {
            if (this.operator.isBool()) {
                boolean leftVal = left.getValue().isTruthy(tracer);
                if (this.operator == ShnapOperators.LOGICAL_AND && !leftVal) {
                    return ShnapExecution.normal(ShnapBooleanNative.FALSE, tracer, this.getLocation());
                } else if (this.operator == ShnapOperators.LOGICAL_OR && leftVal) {
                    return ShnapExecution.normal(ShnapBooleanNative.TRUE, tracer, this.getLocation());
                }
            }

            ShnapExecution right = this.right.exec(context, tracer).resolve(tracer);
            if (right.isAbnormal()) {
                return right;
            }
            return left.getValue().operate(right.getValue(), this.operator, tracer);

        }

        return left.getValue().operate(this.operator, tracer);
    }

    @Override
    public String decompile(int indent) {
        return "(" + (this.operator.getArity() == 1 ? (this.operator.getRep() + "(" + this.left.decompile(indent) + ")") : ("(" + this.left.decompile(indent) + ") " + this.operator.getRep() + " (" + this.right.decompile(indent) + ")")) + ")";
    }

}
