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
package com.gmail.socraticphoenix.shnap.parse;

import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapOperate;

public class OperatorTree {
    private ShnapInstruction instValue;

    private ShnapLoc loc;
    private OperatorTree left;
    private ShnapOperators operator;
    private OperatorTree right;

    public OperatorTree(ShnapInstruction instValue) {
        this.instValue = instValue;
    }

    public OperatorTree(ShnapLoc loc, OperatorTree left, ShnapOperators operator, OperatorTree right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.loc = loc;
    }

    public ShnapLoc getLoc() {
        return this.isLeaf() ? this.instValue.getLocation() : this.loc;
    }

    public ShnapInstruction consolidate() {
        if(this.isLeaf()) {
            return this.instValue;
        } else {
            return new ShnapOperate(this.loc, this.left.consolidate(), this.operator, this.right.consolidate());
        }
    }

    public boolean isLeaf() {
        return this.instValue != null;
    }

    public ShnapInstruction getInstValue() {
        return this.instValue;
    }

    public OperatorTree getLeft() {
        return this.left;
    }

    public ShnapOperators getOperator() {
        return this.operator;
    }

    public OperatorTree getRight() {
        return this.right;
    }

    public boolean isSentinel() {
        return this.operator == ShnapOperators.SENTINEL;
    }

}
