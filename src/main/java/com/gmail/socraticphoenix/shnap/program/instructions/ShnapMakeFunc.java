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
import com.gmail.socraticphoenix.shnap.program.AbstractShnapNode;
import com.gmail.socraticphoenix.shnap.program.ShnapFunction;
import com.gmail.socraticphoenix.shnap.program.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapParameter;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;

import java.util.List;

public class ShnapMakeFunc extends AbstractShnapNode implements ShnapInstruction {
    private List<ShnapParameter> parameters;
    private List<ShnapInstruction> objInstructions;
    private ShnapInstruction body;

    public ShnapMakeFunc(ShnapLoc loc, List<ShnapParameter> parameters, List<ShnapInstruction> objInstructions, ShnapInstruction body) {
        super(loc);
        this.parameters = parameters;
        this.body = body;
        this.objInstructions = objInstructions;
    }

    public List<ShnapParameter> getParameters() {
        return this.parameters;
    }

    public List<ShnapInstruction> getObjInstructions() {
        return this.objInstructions;
    }

    public ShnapInstruction getBody() {
        return this.body;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapFunction function = new ShnapFunction(this.getLocation(), this.parameters, this.body);
        function.init(context);
        for(ShnapInstruction obj : this.objInstructions) {
            obj.exec(function.getContext(), tracer);
        }
        return ShnapExecution.normal(function, tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        StringBuilder builder = new StringBuilder();
        builder.append("$").append(this.parameters.isEmpty() ? "" : this.decArgs(indent));
        if(this.body instanceof ShnapInstructionSequence) {
            builder.append("{").append(System.lineSeparator());
            for (ShnapInstruction instruction : this.objInstructions) {
                builder.append(Strings.indent(indent)).append("static ").append(instruction.decompile(indent + 1)).append(System.lineSeparator());
            }
            for (ShnapInstruction instruction : ((ShnapInstructionSequence) body).getSequence()) {
                builder.append(Strings.indent(indent)).append(instruction.decompile(indent + 1)).append(System.lineSeparator());
            }
            builder.append(Strings.indent(indent - 1)).append("}").toString();
        } else {
            builder.append("{ <native code> }");
        }
        return builder.toString();
    }

    public String decArgs(int indent) {
        StringBuilder args = new StringBuilder().append("(");
        for (int i = 0; i < this.parameters.size(); i++) {
            args.append(this.parameters.get(i).decompile(indent));
            if(i < this.parameters.size() - 1) {
                args.append(", ");
            }
        }
        return args.append(")").toString();
    }

}
