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
import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapLiteral;

public class ShnapForBlock extends AbstractShnapLocatable implements ShnapInstruction {
    private ShnapInstruction name;
    private String varName;
    private ShnapInstruction val;
    private ShnapInstruction instruction;

    public ShnapForBlock(ShnapLoc loc, ShnapInstruction name, String varName, ShnapInstruction val, ShnapInstruction instruction) {
        super(loc);
        this.name = name == null ? new ShnapLiteral(loc, ShnapObject.getVoid()) : name;
        this.val = val;
        this.instruction = instruction;
        this.varName = varName;
    }

    public ShnapInstruction getName() {
        return this.name;
    }

    public String getVarName() {
        return this.varName;
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
        ShnapExecution e2 = this.val.exec(context, tracer).resolve(tracer);
        if (e2.isAbnormal()) {
            return e2;
        }
        ShnapObject iterator = e2.getValue();

        ShnapExecution iteratorE = iterator.get("iterator", tracer);
        if (!iteratorE.isAbnormal() && iteratorE.getValue() instanceof ShnapFunction) {
            ShnapFunction iterfunc = (ShnapFunction) iteratorE.getValue();
            if(iterfunc.paramSizeId() == 1) {
                ShnapExecution exe = iterfunc.invoke(tracer);
                if(exe.isAbnormal()) {
                    return exe;
                } else {
                    iterator = exe.getValue();
                }
            }
        }

        boolean flag = false;
        ShnapFunction nextFunc = null;
        ShnapFunction hasNextFunc = null;

        ShnapExecution nextE = iterator.get("next", tracer);
        ShnapExecution hasNextE = iterator.get("hasNext", tracer);

        if(nextE.getState() == State.THROWING) {
            return nextE;
        } else if (hasNextE.getState() == State.THROWING) {
            return hasNextE;
        }

        if (!nextE.isAbnormal() && !hasNextE.isAbnormal() && nextE.getValue() instanceof ShnapFunction && hasNextE.getValue() instanceof ShnapFunction) {
            nextFunc = (ShnapFunction) nextE.getValue();
            hasNextFunc = (ShnapFunction) hasNextE.getValue();
            flag = nextFunc.paramSizeId() == 0 && hasNextFunc.paramSizeId() == 0;
        }

        ShnapExecution ret = ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
        if (flag) {
            while (flag) {
                ShnapExecution cond = hasNextFunc.invoke(tracer).resolve(tracer);
                if (cond.isAbnormal()) {
                    return cond;
                } else {
                    boolean hasNext = cond.getValue().isTruthy(tracer);
                    if (!hasNext) {
                        break;
                    }
                }

                ShnapExecution nextExe = nextFunc.invoke(tracer);
                if (nextExe.isAbnormal()) {
                    return nextExe;
                }

                ShnapObject iterElem = nextExe.getValue();
                ShnapContext sub = ShnapContext.childOf(context);
                sub.setLocally(this.varName, iterElem);
                ShnapExecution block = this.instruction.exec(sub, tracer);
                ret = block;

                if (block.getState() == State.RETURNING || block.getState() == State.THROWING) {
                    return block;
                } else if (block.getValue() == ShnapObject.getVoid() || name.isEqualTo(block.getValue(), tracer)) {
                    if (block.getState() == State.BREAKING) {
                        return ShnapExecution.normal(block.getValue(), tracer, this.getLocation());
                    }
                } else if (block.getState() == State.BREAKING || block.getState() == State.CONTINUING) {
                    return block;
                }

                nextE = iterator.get("next", tracer);
                hasNextE = iterator.get("hasNext", tracer);

                if(nextE.getState() == State.THROWING) {
                    return nextE;
                } else if (hasNextE.getState() == State.THROWING) {
                    return hasNextE;
                }

                if (!nextE.isAbnormal() && !hasNextE.isAbnormal() && nextE.getValue() instanceof ShnapFunction && hasNextE.getValue() instanceof ShnapFunction) {
                    nextFunc = (ShnapFunction) nextE.getValue();
                    hasNextFunc = (ShnapFunction) hasNextE.getValue();
                    flag = nextFunc.paramSizeId() == 0 && hasNextFunc.paramSizeId() == 0;
                } else {
                    flag = false;
                }
            }

            return ret;
        } else {
            ShnapContext sub = ShnapContext.childOf(context);
            sub.set(this.varName, iterator);
            return this.instruction.exec(sub, tracer);
        }
    }

    @Override
    public String decompile(int indent) {
        StringBuilder block = new StringBuilder();
        if (!(this.name instanceof ShnapLiteral) || ((ShnapLiteral) this.name).getValue() != ShnapObject.getVoid()) {
            block.append(this.name.decompile(indent)).append(":").append(System.lineSeparator());
            block.append(Strings.indent(indent - 1));
        }
        return block.append("for(").append(this.varName).append(" : ").append(this.val.decompile(indent)).append(") ").append(this.instruction.decompile(indent)).toString();
    }

}
