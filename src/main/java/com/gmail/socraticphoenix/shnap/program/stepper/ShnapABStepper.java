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

package com.gmail.socraticphoenix.shnap.program.stepper;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;

import java.util.Optional;
import java.util.function.BiFunction;

public class ShnapABStepper implements ShnapStepper {
    private ShnapStepper stepper;
    private TriFunction<ShnapExecution, ShnapContext, ShnapEnvironment, ShnapExecution> transformer;
    private BiFunction<ShnapContext, ShnapEnvironment, ShnapExecution> def;
    private ShnapInstruction instruction;

    public ShnapABStepper(ShnapInstruction stepper, TriFunction<ShnapExecution, ShnapContext, ShnapEnvironment, ShnapExecution> transformer, BiFunction<ShnapContext, ShnapEnvironment, ShnapExecution> def, ShnapInstruction instruction) {
        //this(stepper == null ? null : stepper.asStepper(), transformer, def, instruction);
    }

    public ShnapABStepper(ShnapStepper stepper, TriFunction<ShnapExecution, ShnapContext, ShnapEnvironment, ShnapExecution> transformer, BiFunction<ShnapContext, ShnapEnvironment, ShnapExecution> def, ShnapInstruction instruction) {
        this.stepper = stepper;
        this.transformer = transformer;
        this.def = def;
        this.instruction = instruction;
    }


    private Optional<ShnapExecution> working = Optional.empty();

    @Override
    public Optional<ShnapExecution> step(ShnapContext context, ShnapEnvironment tracer) {
        if (this.stepper == null) {
            return Optional.of(this.def.apply(context, tracer));
        }

        if (this.working.isPresent()) {
            return Optional.of(this.working.get().mapIfNormal(e -> this.transformer.apply(e, context, tracer)));
        }

        this.working = this.stepper.step(context, tracer);
        return Optional.empty();
    }

    @Override
    public ShnapInstruction instruction() {
        return this.instruction;
    }

    @Override
    public ShnapLoc getLocation() {
        return this.instruction.getLocation();
    }

}
