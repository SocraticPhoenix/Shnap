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
package com.gmail.socraticphoenix.shnap.program.instructions.block;

import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapNode;
import com.gmail.socraticphoenix.shnap.program.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapLiteral;

public class ShnapDoWhileBlock extends AbstractShnapNode implements ShnapInstruction {
    private ShnapInstruction name;
    private ShnapInstruction val;
    private ShnapInstruction instruction;

    public ShnapDoWhileBlock(ShnapLoc loc, ShnapInstruction name, ShnapInstruction val, ShnapInstruction instruction) {
        super(loc);
        this.name = name == null ? new ShnapLiteral(loc, ShnapObject.getVoid()) : name;
        this.val = val;
        this.instruction = instruction;
    }

    public ShnapInstruction getName() {
        return this.name;
    }

    public ShnapInstruction getVal() {
        return this.val;
    }

    public ShnapInstruction getInstruction() {
        return this.instruction;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapExecution e = this.name.exec(context, tracer);
        if (e.isAbnormal()) {
            return e;
        }

        ShnapObject name = e.getValue();

        boolean condition;
        ShnapExecution ret;
        do {
            ShnapExecution block = this.instruction.exec(ShnapContext.childOf(context), tracer);
            ret = block;

            if (block.getState() == State.RETURNING || block.getState() == State.THROWING) {
                return block;
            } else if (name.isEqualTo(block.getValue(), tracer)) {
                if (block.getState() == State.BREAKING) {
                    return ShnapExecution.normal(block.getValue(), tracer, this.getLocation());
                }
            } else if (block.getState() == State.BREAKING || block.getState() == State.CONTINUING) {
                return block;
            }

            ShnapExecution e2 = this.val.exec(context, tracer);
            if (e2.isAbnormal()) {
                return e2;
            }

            condition = e2.getValue().isTruthy(tracer);
        } while (condition);
        return ret;
    }


    @Override
    public String decompile(int indent) {
        StringBuilder block = new StringBuilder();
        if (!(this.name instanceof ShnapLiteral) || ((ShnapLiteral) this.name).getValue() != ShnapObject.getVoid()) {
            block.append(this.name.decompile(indent)).append(":").append(System.lineSeparator());
            block.append(Strings.indent(indent - 1));
        }
        return block.append("dowhile(").append(this.val.decompile(indent)).append(") ").append(this.instruction.decompile(indent)).toString();
    }

}