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

import java.util.List;
import java.util.Optional;

public class ShnapMultiStepStepper implements ShnapStepper {
    private ShnapInstruction instruction;
    private ShnapStepper[] steppers;
    private int current;

    public ShnapMultiStepStepper(ShnapInstruction instruction, List<ShnapStepper> steppers) {
        this(instruction, steppers.toArray(new ShnapStepper[steppers.size()]));
    }

    public ShnapMultiStepStepper(ShnapInstruction instruction, ShnapStepper... steppers) {
        this.instruction = instruction;
        this.steppers = steppers;
    }

    @Override
    public Optional<ShnapExecution> step(ShnapContext context, ShnapEnvironment tracer) {
        ShnapStepper stepper = this.steppers[this.current];

        Optional<ShnapExecution> exec = stepper.step(context, tracer);
        if(exec.isPresent()) {
            this.current++;
            if (this.current == this.steppers.length) {
                return exec;
            }
        }

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
