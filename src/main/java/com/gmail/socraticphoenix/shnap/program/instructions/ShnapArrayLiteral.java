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
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;

import java.util.List;

public class ShnapArrayLiteral extends AbstractShnapLocatable implements ShnapInstruction {
    private List<ShnapInstruction> values;

    public ShnapArrayLiteral(ShnapLoc loc, List<ShnapInstruction> values) {
        super(loc);
        this.values = values;
    }

    public List<ShnapInstruction> getValues() {
        return this.values;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapObject[] arr = new ShnapObject[this.values.size()];
        for (int i = 0; i < this.values.size(); i++) {
            ShnapExecution res = this.values.get(i).exec(context, tracer);
            if(res.isAbnormal()) {
                return res;
            } else {
                arr[i] = res.getValue();
            }
        }
        return ShnapExecution.normal(new ShnapArrayNative(this.getLocation(), arr), tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < this.values.size(); i++) {
            builder.append(this.values.get(i).decompile(indent));
            if(i < this.values.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.append("]").toString();
    }

}
