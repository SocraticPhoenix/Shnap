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
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;

public class ShnapFlag extends AbstractShnapLocatable implements ShnapInstruction {
    private ShnapInstruction target;
    private String name;
    private ShnapContext.Flag flag;

    public ShnapFlag(ShnapLoc loc, ShnapInstruction target, String name, ShnapContext.Flag flag) {
        super(loc);
        this.target = target;
        this.name = name;
        this.flag = flag;
    }

    public ShnapInstruction getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public ShnapContext.Flag getFlag() {
        return flag;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapContext targetContext = context;
        if(this.target != null) {
            ShnapExecution e = this.target.exec(context, tracer);
            if(e.isAbnormal()) {
                return e;
            } else {
                ShnapObject object = e.getValue();
                targetContext = object.getContext();
            }
        }

        targetContext.setFlag(this.name, this.flag);

        return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        return this.flag.getRep() + " " + (this.target == null ? "" : this.target.decompile(indent) + ".") + this.name;
    }
}
