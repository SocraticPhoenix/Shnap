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

import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapOperators;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;

public class ShnapSet extends AbstractShnapLocatable implements ShnapInstruction {
    private ShnapInstruction target;
    private String name;
    private ShnapInstruction val;
    private ShnapOperators op;

    public ShnapSet(ShnapLoc loc, ShnapInstruction target, String name, ShnapInstruction val, ShnapOperators op) {
        super(loc);
        this.target = target;
        this.name = name;
        this.val = val;
        this.op = op;
    }

    public ShnapInstruction getTarget() {
        return this.target;
    }

    public String getName() {
        return this.name;
    }

    public ShnapInstruction getVal() {
        return this.val;
    }

    public ShnapOperators getOp() {
        return this.op;
    }

    public void setTarget(ShnapInstruction target) {
        this.target = target;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapContext targetContext = context;
        if(this.target != null) {
            ShnapExecution e = this.target.exec(context, tracer).resolve(tracer);
            if(e.isAbnormal()) {
                return e;
            } else {
                ShnapObject object = e.getValue();
                targetContext = object.getContext();
            }
        }

        if(targetContext.hasFlag(this.name, ShnapContext.Flag.FINALIZED)) {
            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.AccessError", "field " + name + " is flagged with FINALIZED", null), tracer, this.getLocation());
        } else if (!context.isChildOf(targetContext) && targetContext.hasFlag(this.name, ShnapContext.Flag.PRIVATE)) {
            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.AccessError", "field " + name + " is flagged with PRIVATE", null), tracer, this.getLocation());
        }

        ShnapExecution execution = this.val.exec(context, tracer);
        if(execution.isAbnormal()) {
            return execution;
        }

        if(this.op == null) {
            targetContext.set(this.name, execution.getValue());
            return execution;
        } else {
            ShnapContext finalTargetContext = targetContext;
            return targetContext.get(this.name, tracer).mapIfNormal(e -> {
                ShnapObject prev = e.getValue();
                ShnapExecution op = prev.operate(execution.getValue(), this.op, tracer).resolve(tracer);
                if(op.isAbnormal()) {
                    return op;
                }
                finalTargetContext.set(this.name, op.getValue());
                return op;
            });
        }
    }


    @Override
    public String decompile(int indent) {
        return target == null ? this.name + " = " + this.val.decompile(indent) : this.target.decompile(indent) + "." + this.name + " " + (this.op == null ? "" : this.op.getRep()) + "= " + this.val.decompile(indent);
    }

}
