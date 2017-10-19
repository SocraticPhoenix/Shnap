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
package com.gmail.socraticphoenix.shnap.type.object;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapParameter;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution.State;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.run.env.ShnapTraceback;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ShnapFunction extends ShnapObject {
    protected List<ShnapParameter> params;

    protected List<ShnapParameter> required;
    protected List<ShnapParameter> def;
    protected Set<String> names;
    protected ShnapInstruction body;

    private boolean hasVarArgs;

    public ShnapFunction(ShnapLoc loc, List<ShnapParameter> parameters, ShnapInstruction body) {
        super(loc, new ShnapContext(), "function");
        this.loc = loc;
        this.params = parameters;
        this.names = new HashSet<>();

        this.required = new ArrayList<>();
        this.def = new ArrayList<>();
        this.body = body;
        for (ShnapParameter parameter : parameters) {
            if (parameter.getValue() == null) {
                this.required.add(parameter);
            } else {
                this.def.add(parameter);
            }

            if (parameter.isVariable()) {
                this.hasVarArgs = true;
            }

            this.names.add(parameter.getName());
        }
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
        this.context.setLocally("thisFunc", this);
        this.context.setFlag("thisFunc", ShnapContext.Flag.DONT_IMPORT);
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

    public ShnapExecution invokeOperator(List<ShnapObject> values, int order, ShnapEnvironment tracer) {
        if (order != 0) {
            values.add(ShnapNumberNative.valueOf(order));
        }
        return this.invokePrivate(values, Collections.emptyMap(), tracer);
    }

    public ShnapObject copyWith(ShnapContext context) {
        ShnapFunction func = new ShnapFunction(this.loc, this.params, this.body);
        func.init(context);
        return func;
    }

    public ShnapExecution invokeWithoutTrace(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        return this.invokePrivate(values, defValues, tracer);
    }

    public ShnapExecution invoke(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        this.trace(tracer);
        return this.invokePrivate(values, defValues, tracer);
    }

    protected ShnapExecution invokePrivate(List<ShnapObject> values, Map<String, ShnapObject> defValues, ShnapEnvironment tracer) {
        try {
            ShnapContext functionContext = this.getContext().copy();
            functionContext.setLocally("thisFunc", this);
            if (values.size() + defValues.size() > this.paramsSize() && !this.hasVarArgs) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParameterSizeError", "Expected at most " + this.paramsSize() + " params, but got " + (values.size() + defValues.size()), null, "shnap.ParameterError", "shnap.InvocationError"), tracer, this.getLocation());
            } else if (values.size() + defValues.size() + (this.hasVarArgs ? 1 : 0) < this.paramSizeId()) {
                return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParameterSizeError", "Expected at least " + this.paramsSize() + " params, but got " + (values.size() + defValues.size()), null, "shnap.ParameterError", "shnap.InvocationError"), tracer, this.getLocation());
            }

            for (Map.Entry<String, ShnapObject> def : defValues.entrySet()) {
                String name = def.getKey();
                if (this.names.contains(name)) {
                    functionContext.setLocally(name, def.getValue());
                } else {
                    return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParameterMismatchError", "Unexpected parameter for " + name, null, "shnap.ParameterError", "shnap.InvocationError"), tracer, this.getLocation());
                }
            }

            for (ShnapParameter def : this.def) {
                if (!defValues.containsKey(def.getName())) {
                    ShnapExecution res = def.getValue().exec(context, tracer);
                    if (res.isAbnormal()) {
                        return res;
                    }
                    functionContext.setLocally(def.getName(), res.getValue());
                }
            }

            boolean iter = false;
            for (int i = 0; i < this.paramsSize() && i < values.size(); i++) {
                ShnapParameter parameter = this.params.get(i);
                String name = parameter.getName();
                if (parameter.isVariable()) {
                    iter = true;
                    int len = values.size() - i;
                    ShnapObject[] arr = new ShnapObject[len];
                    for (int j = 0; j < len; j++) {
                        arr[j] = values.get(j + i);
                    }
                    functionContext.setLocally(name, new ShnapArrayNative(parameter.getLocation(), arr));
                    break;
                } else if (defValues.containsKey(name)) {
                    return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.ParameterMismatchError", "Unexpected duplicate parameter for " + name, null, "shnap.ParameterError", "shnap.InvocationError"), tracer, this.getLocation());
                } else {
                    functionContext.setLocally(name, values.get(i));
                }
            }

            if (!iter && this.hasVarArgs) {
                Optional<ShnapParameter> param = this.required.stream().filter(ShnapParameter::isVariable).findFirst();
                param.ifPresent(shnapParameter -> functionContext.setLocally(shnapParameter.getName(), new ShnapArrayNative(this.getLocation(), 0)));
            }

            ShnapExecution e = this.body.exec(functionContext, tracer);
            if (e.getState() == State.THROWING) {
                return e;
            } else if (e.getState() == State.RETURNING) {
                tracer.popTraceback();
                return ShnapExecution.normal(e.getValue(), tracer, this.getLocation());
            } else {
                tracer.popTraceback();
                return ShnapExecution.normal(ShnapObject.getVoid(), tracer, this.getLocation());
            }
        } catch (StackOverflowError e) {
            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.StackOverflowError", "Recursed too deeply",null), tracer, this.getLocation());
        } catch (Throwable other) {
            return ShnapExecution.throwing(ShnapFactory.mimicJavaException(other), tracer, this.getLocation());
        }
    }

    private boolean containsParam(int i) {
        return i >= 0 && i < this.paramsSize();
    }

    public int paramsSize() {
        return this.required.size() + this.def.size();
    }

    public int defSize() {
        return this.def.size();
    }

    public ShnapParameter getParam(int index) {
        if (index < this.required.size()) {
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
