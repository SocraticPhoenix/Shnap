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
package com.gmail.socraticphoenix.shnap.program.context;

import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.env.ShnapTraceback;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;

import java.util.function.Consumer;
import java.util.function.Function;

public class ShnapExecution {
    private ShnapObject value;
    private State state;

    public ShnapExecution(ShnapObject value, State state, ShnapEnvironment trc, ShnapLoc loc) {
        this.value = value;
        this.state = state;
        if(state.isAbnormal()) {
            trc.pushTraceback(ShnapTraceback.meta(loc, "abnormal state: " + state));
        }
    }

    public ShnapExecution ifAbnormal(Consumer<ShnapExecution> action) {
        if(this.isAbnormal()) {
            action.accept(this);
        }
        return this;
    }

    public ShnapExecution ifNormal(Consumer<ShnapExecution> action) {
        if(!this.isAbnormal()) {
            action.accept(this);
        }
        return this;
    }

    public ShnapExecution mapIfAbnormal(Function<ShnapExecution, ShnapExecution> action) {
        if(this.isAbnormal()) {
            return action.apply(this);
        }
        return this;
    }

    public ShnapExecution mapIfNormal(Function<ShnapExecution, ShnapExecution> action) {
        if(!this.isAbnormal()) {
            return action.apply(this);
        }
        return this;
    }

    public static ShnapExecution normal(ShnapObject object, ShnapEnvironment trc, ShnapLoc loc) {
        return new ShnapExecution(object, State.NORMAL, trc, loc);
    }

    public static ShnapExecution breaking(ShnapObject object, ShnapEnvironment trc, ShnapLoc loc) {
        return new ShnapExecution(object, State.BREAKING, trc, loc);
    }

    public static ShnapExecution continuing(ShnapObject object, ShnapEnvironment trc, ShnapLoc loc) {
        return new ShnapExecution(object, State.CONTINUING, trc, loc);
    }

    public static ShnapExecution returning(ShnapObject object, ShnapEnvironment trc, ShnapLoc loc) {
        return new ShnapExecution(object, State.RETURNING, trc, loc);
    }

    public static ShnapExecution throwing(ShnapObject object, ShnapEnvironment trc, ShnapLoc loc) {
        return new ShnapExecution(object, State.THROWING, trc, loc);
    }

    public ShnapObject getValue() {
        return this.value;
    }

    public State getState() {
        return this.state;
    }

    public boolean isAbnormal() {
        return getState().isAbnormal();
    }

    public enum State {
        NORMAL("normal"),
        BREAKING("break"),
        CONTINUING("continue"),
        RETURNING("return"),
        THROWING("throw");

        String rep;

        State(String rep) {
            this.rep = rep;
        }

        public boolean isAbnormal() {
            return this != NORMAL;
        }

        public String getRep() {
            return this.rep;
        }

    }

}
