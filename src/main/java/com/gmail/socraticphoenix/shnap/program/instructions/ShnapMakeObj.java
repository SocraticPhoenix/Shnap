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

import com.gmail.socraticphoenix.shnap.program.AbstractShnapNode;
import com.gmail.socraticphoenix.shnap.program.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;

public class ShnapMakeObj extends AbstractShnapNode implements ShnapInstruction {
    private ShnapInstruction instruction;

    public ShnapMakeObj(ShnapLoc loc, ShnapInstruction instruction) {
        super(loc);
        this.instruction = instruction;
    }

    public ShnapInstruction getInstruction() {
        return this.instruction;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapObject object = new ShnapObject(this.getLocation());
        object.init(context);

        ShnapExecution exe = this.instruction.exec(object.getContext(), tracer);
        if(exe.isAbnormal()) {
            return exe;
        }

        return ShnapExecution.normal(object, tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        return "#" + this.instruction.decompile(indent);
    }

}
