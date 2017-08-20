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

import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;

import java.util.List;

public class ShnapInstructionSequence extends AbstractShnapLocatable implements ShnapInstruction {
    private List<ShnapInstruction> sequence;

    public ShnapInstructionSequence(ShnapLoc loc, List<ShnapInstruction> sequence) {
        super(loc);
        this.sequence = sequence;
    }

    public List<ShnapInstruction> getSequence() {
        return this.sequence;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        int size = this.sequence.size();
        for (int i = 0; i < size; i++) {
            ShnapInstruction instruction = this.sequence.get(i);
            if (i < size - 1) {
                ShnapExecution execution = instruction.exec(context, tracer);
                if (execution.isAbnormal()) {
                    return execution;
                }
            } else {
                return instruction.exec(context, tracer);
            }
        }

        return ShnapExecution.normal(ShnapObject.getNull(), tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        StringBuilder seq = new StringBuilder().append("{").append(System.lineSeparator());
        for (ShnapInstruction instruction : this.sequence) {
            seq.append(Strings.indent(indent)).append(instruction.decompile(indent + 1)).append(System.lineSeparator());
        }
        return seq.append(Strings.indent(indent - 1)).append("}").toString();
    }

}
