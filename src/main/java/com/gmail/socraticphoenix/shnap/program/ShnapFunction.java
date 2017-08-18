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
package com.gmail.socraticphoenix.shnap.program;

import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.env.ShnapTraceback;
import com.gmail.socraticphoenix.shnap.err.ShnapException;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShnapFunction extends ShnapObject {
    private List<ShnapParameter> params;

    private List<ShnapParameter> required;
    private List<ShnapParameter> def;
    private ShnapInstruction body;

    public ShnapFunction(ShnapLoc loc, List<ShnapParameter> parameters, ShnapInstruction body) {
        super(loc, new ShnapContext());
        this.loc = loc;
        this.params = parameters;

        boolean inDefaults = false;
        this.required = new ArrayList<>();
        this.def = new ArrayList<>();
        this.body = body;
        for(ShnapParameter parameter : parameters) {
            if(parameter.getValue() == null) {
                if(inDefaults) {
                    throw ShnapException.syntax(parameter, "Parameter with no default value after parameter(s) with default value(s)");
                } else {
                    this.required.add(parameter);
                }
            } else {
                inDefaults = true;
                this.def.add(parameter);
            }
        }

        this.set("thisFunc", this);
    }

    @Override
    public String defaultToString() {
        return "func[" + this.required.size() + "," + this.def.size() + "]::" + this.identityStr();
    }

    public ShnapFunction init(ShnapObject parent) {
        this.init(parent.context);
        return this;
    }

    public void init(ShnapContext context) {
        this.context = ShnapContext.childOf(context);
        this.set("thisFunc", this);
    }

    public ShnapFunction copyPreInit() {
        return new ShnapFunction(this.loc, this.params, this.body);
    }

    public ShnapExecution invoke(ShnapEnvironment tracer) {
        return this.invoke(Collections.emptyList(), Collections.emptyMap(), tracer);
    }

    public void trace(ShnapEnvironment tracer) {
        tracer.pushTraceback(ShnapTraceback.frame(this.getLocation(), "Invoke function"));
    }

    public ShnapExecution invokeOperator(List<ShnapObject> values, Map<String, ShnapObject> defValues, int order, ShnapEnvironment tracer) {
        ShnapContext functionContext = this.getContext().copy();
        this.trace(tracer);
        functionContext.set("order", ShnapNumberNative.valueOf(order));
        for (int i = 0; i < this.paramsSize(); i++) {
            String name = this.getParam(i).getName();
            ShnapObject val;
            if(defValues.containsKey(name)) {
                val = defValues.get(name);
            } else if (i < values.size()) {
                val = values.get(i);
            } else {
                ShnapInstruction param = this.getParam(i).getValue();
                ShnapExecution ex = param.exec(functionContext, tracer);
                if(ex.isAbnormal()) {
                    return ex;
                }

                val = ex.getValue();
            }

            functionContext.set(name, val);
        }
        ShnapExecution e = this.body.exec(functionContext, tracer);
        if(e.getState() == State.THROWING) {
            return e;
        } else if (e.getState() == State.RETURNING) {
            tracer.popTraceback();
            return ShnapExecution.normal(e.getValue(), tracer, this.getLocation());
        } else {
            tracer.popTraceback();
            return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
        }
    }

    public ShnapExecution invokeWithoutTrace(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        return this.invokePrivate(values, defValues, tracer);
    }

    public ShnapExecution invoke(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        this.trace(tracer);
        return this.invokePrivate(values, defValues, tracer);
    }

    private ShnapExecution invokePrivate(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        ShnapContext functionContext = this.getContext().copy();
        functionContext.set("func", this);
        for (int i = 0; i < this.paramsSize(); i++) {
            String name = this.getParam(i).getName();
            ShnapObject val;
            if(defValues.containsKey(name)) {
                val = defValues.get(name);
            } else if (i < values.size()) {
                val = values.get(i);
            } else {
                ShnapInstruction param = this.getParam(i).getValue();
                ShnapExecution ex = param.exec(functionContext, tracer);
                if(ex.isAbnormal()) {
                    return ex;
                }

                val = ex.getValue();
            }

            functionContext.set(name, val);
        }
        ShnapExecution e = this.body.exec(functionContext, tracer);
        if(e.getState() == State.THROWING) {
            return e;
        } else if (e.getState() == State.RETURNING) {
            tracer.popTraceback();
            return ShnapExecution.normal(e.getValue(), tracer, this.getLocation());
        } else {
            tracer.popTraceback();
            return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
        }
    }

    public int paramsSize() {
        return this.required.size() + this.def.size();
    }

    public ShnapParameter getParam(int index) {
        if(index < this.required.size()) {
            return this.required.get(index);
        } else {
            return this.def.get(index - this.required.size());
        }
    }


    public int paramSizeId() {
        return this.required.size();
    }

    public List<ShnapParameter> getParams() {
        return params;
    }
}
