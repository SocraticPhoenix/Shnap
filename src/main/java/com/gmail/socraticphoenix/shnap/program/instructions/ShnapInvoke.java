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
import com.gmail.socraticphoenix.shnap.run.env.ShnapTraceback;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapLocatable;
import com.gmail.socraticphoenix.shnap.type.object.ShnapFunction;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShnapInvoke extends AbstractShnapLocatable implements ShnapInstruction {
    private ShnapInstruction target;
    private List<ShnapInstruction> args;
    private Map<String, ShnapInstruction> defArgs;
    private List<ShnapInstruction> defArgsAsList;

    public ShnapInvoke(ShnapLoc loc, ShnapInstruction target, List<ShnapInstruction> args, Map<String, ShnapInstruction> defArgs) {
        super(loc);
        this.target = target;
        this.args = args;
        this.defArgs = defArgs;
        this.defArgsAsList = new ArrayList<>(defArgs.values());
    }

    public ShnapInstruction getTarget() {
        return this.target;
    }

    public List<ShnapInstruction> getArgs() {
        return this.args;
    }

    public Map<String, ShnapInstruction> getDefArgs() {
        return this.defArgs;
    }

    public List<ShnapInstruction> getDefArgsAsList() {
        return this.defArgsAsList;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        ShnapExecution execution = this.target.exec(context, tracer);
        if(execution.getState().isAbnormal()) {
            return execution;
        }
        return execution.getValue().resolve(tracer).mapIfNormal(e -> {
            ShnapObject object = e.getValue();
            if(object instanceof ShnapFunction) {
                ShnapFunction function = (ShnapFunction) object;
                List<ShnapObject> values = new ArrayList<>();
                for(ShnapInstruction arg : this.args) {
                    ShnapExecution argExec = arg.exec(context, tracer).mapIfNormal(exe -> exe.getValue().resolve(tracer));
                    if(argExec.isAbnormal()) {
                        return argExec;
                    } else {
                        values.add(argExec.getValue());
                    }
                }
                Map<String, ShnapObject> defValues = new LinkedHashMap<>();
                for(Map.Entry<String, ShnapInstruction> defArg : this.defArgs.entrySet()) {
                    ShnapExecution argExec = defArg.getValue().exec(context, tracer).mapIfNormal(exe -> exe.getValue().resolve(tracer));
                    if(argExec.isAbnormal()) {
                        return argExec;
                    } else {
                        defValues.put(defArg.getKey(), argExec.getValue());
                    }
                }
                tracer.pushTraceback(ShnapTraceback.frame(this.getLocation(), "Invoke function"));
                ShnapExecution res = function.invokeWithoutTrace(values, defValues, tracer);

                if(res.isAbnormal()) {
                    return res;
                }

                return res;
            } else {
                return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
            }
        });
    }

    @Override
    public String decompile(int indent) {
        return this.target.decompile(indent) + this.decArgs(indent);
    }

    private String decArgs(int indent) {
        StringBuilder args = new StringBuilder().append("(");
        for (int i = 0; i < this.args.size(); i++) {
            args.append(this.args.get(i).decompile(indent));
            if(i < this.args.size() - 1 || !this.defArgs.isEmpty()) {
                args.append(", ");
            }
        }

        int k = 0;
        for(Map.Entry<String, ShnapInstruction> defArg : this.defArgs.entrySet()) {
            args.append(defArg.getKey()).append("=").append(defArg.getValue().decompile(indent));
            if(k < this.defArgs.size() - 1) {
                args.append(", ");
            }
            k++;
        }

        return args.append(")").toString();
    }

}
