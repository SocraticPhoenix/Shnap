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
package com.gmail.socraticphoenix.shnap.err;

import com.gmail.socraticphoenix.shnap.program.ShnapNode;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;

public class ShnapException extends RuntimeException {
    private ShnapNode offender;
    private ShnapEnvironment tracer;
    private ShnapContext context;

    public ShnapException(ShnapNode offender, ShnapEnvironment tracer, ShnapContext context, String message) {
        super(message);
        this.offender = offender;
        this.tracer = tracer;
        this.context = context;
    }

    public static ShnapException runtime(ShnapNode offender, ShnapEnvironment tracer, ShnapContext context, String message) {
        return new ShnapException(offender, tracer, context, message);
    }

    public static ShnapException syntax(ShnapNode offender, String message) {
        return new ShnapException(offender, null, null, message);
    }

    public ShnapNode getOffender() {
        return this.offender;
    }

    public ShnapException setOffender(ShnapNode offender) {
        this.offender = offender;
        return this;
    }

    public ShnapEnvironment getTracer() {
        return this.tracer;
    }

    public ShnapException setTracer(ShnapEnvironment tracer) {
        this.tracer = tracer;
        return this;
    }

    public ShnapContext getContext() {
        return this.context;
    }

    public ShnapException setContext(ShnapContext context) {
        this.context = context;
        return this;
    }

}
