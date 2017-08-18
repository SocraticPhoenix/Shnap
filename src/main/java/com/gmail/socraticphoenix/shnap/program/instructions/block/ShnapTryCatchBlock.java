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

import com.gmail.socraticphoenix.shnap.program.AbstractShnapNode;
import com.gmail.socraticphoenix.shnap.program.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapNoOp;

public class ShnapTryCatchBlock extends AbstractShnapNode implements ShnapInstruction {
    private ShnapInstruction tryBlock;
    private ShnapInstruction catchBlock;
    private String catchName;

    public ShnapTryCatchBlock(ShnapLoc loc, ShnapInstruction tryBlock, ShnapInstruction catchBlock, String catchName) {
        super(loc);
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.catchName = catchName;
    }

    public ShnapInstruction getTryBlock() {
        return this.tryBlock;
    }

    public ShnapInstruction getCatchBlock() {
        return this.catchBlock;
    }

    public String getCatchName() {
        return this.catchName;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapExecution ex = this.tryBlock.exec(ShnapContext.childOf(context), tracer);
        if(ex.getState() == State.THROWING) {
            ShnapContext throwContext = ShnapContext.childOf(context);
            throwContext.set(this.catchName, ex.getValue());
            return this.catchBlock.exec(throwContext, tracer);
        } else {
            return ex;
        }
    }

    @Override
    public String decompile(int indent) {
        StringBuilder builder = new StringBuilder().append("try ").append(this.tryBlock.decompile(indent));
        if(!(this.catchBlock instanceof ShnapNoOp)) {
            builder.append(" catch ").append(this.catchBlock.decompile(indent));
        }
        return builder.toString();
    }

}
