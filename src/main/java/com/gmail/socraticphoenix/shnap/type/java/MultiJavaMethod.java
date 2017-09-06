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

package com.gmail.socraticphoenix.shnap.type.java;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapJavaBackedNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiJavaMethod {
    private List<TypedJavaMethod> methods;

    public MultiJavaMethod() {
        this.methods = new ArrayList<>();
    }

    public void add(TypedJavaMethod method) {
        this.methods.add(method);
    }

    public ShnapExecution execute(ShnapJavaBackedNative owner, List<ShnapObject> params, ShnapEnvironment environment) {
        List<Object> resolved = new ArrayList<>();
        List<Class> types = new ArrayList<>();

        for (ShnapObject obj : params) {
            ShnapExecution asJava = obj.asJava(environment);
            if (asJava.isAbnormal()) {
                return asJava;
            } else {
                Object theVal = ((ShnapJavaBackedNative) asJava.getValue()).getJavaBacker();
                resolved.add(theVal);
                types.add(theVal == null ? null : theVal.getClass());
            }
        }

        TypedJavaMethod selected = null;

        for (TypedJavaMethod method : this.methods.stream().filter(m -> m.matches(types)).collect(Collectors.toList())) {
            if (selected == null) {
                selected = method;
            } else if (method.conflictsWith(selected)) {
                selected = selected.resolveConflict(method, types);
            }
        }

        if (selected == null) {
            StringBuilder message = new StringBuilder().append("Failed to invoke java method:").append(System.lineSeparator());

            for (TypedJavaMethod method : this.methods) {
                message.append(" - ").append(method.format()).append(System.lineSeparator());
            }
            return ShnapExecution.throwing(ShnapFactory.makeExceptionObj("shnap.JavaInterfaceError", message.toString(), null), environment, ShnapLoc.BUILTIN);
        }

        return selected.execute(owner, resolved, environment);
    }

}