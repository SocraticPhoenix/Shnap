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

package com.gmail.socraticphoenix.shnap.executor;

import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.env.ShnapScriptAbsentException;
import com.gmail.socraticphoenix.shnap.env.ShnapScriptCircularInitException;
import com.gmail.socraticphoenix.shnap.env.ShnapScriptInvalidSyntaxException;
import com.gmail.socraticphoenix.shnap.env.ShnapScriptLoadingFailedException;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapScript;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;

import java.io.IOException;
import java.util.function.Consumer;

public class ShnapExecutor {
    private ShnapExecutionSettings settings;
    private ShnapEnvironment environment;

    public ShnapExecutor(ShnapExecutionSettings settings) {
        this.settings = settings;
    }

    public ShnapExecution performInitialLoading(Consumer<String> log)  throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        return this.performInitialLoading(log, true);
    }

    public ShnapExecution performInitialLoading()  throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        return this.performInitialLoading(null, false);
    }

    public ShnapExecution performInitialLoading(Consumer<String> theLog, boolean log) throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        this.environment = this.settings.buildEnvironment();
        this.settings.applyHomeSettings(this.environment);

        if (log) {
            theLog.accept("Loading natives..." + System.lineSeparator());
        }
        ShnapExecution nativeLoad = this.environment.loadNatives();
        if (nativeLoad.isAbnormal()) {
            return nativeLoad;
        }
        if (log) {
            theLog.accept("Loaded natives." + System.lineSeparator());
            theLog.accept("Loading builtins..." + System.lineSeparator());
        }
        ShnapExecution builtinLoad = this.environment.loadBuiltins();
        if (builtinLoad.isAbnormal()) {
            return builtinLoad;
        }
        if (log) {
            theLog.accept("Loaded builtins." + System.lineSeparator());
        }
        this.environment.loadNormal();

        return ShnapExecution.normal(ShnapObject.getVoid(), this.environment, ShnapLoc.BUILTIN);
    }

    public ShnapExecution execute(String scriptName) throws ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        ShnapExecution scriptLoad = this.environment.getModuleExecution(scriptName);
        if(scriptLoad.isAbnormal()) {
            return scriptLoad;
        }

        ShnapScript script = (ShnapScript) scriptLoad.getValue();
        return script.runMain(this.environment);
    }

    public ShnapEnvironment getEnvironment() {
        return this.environment;
    }


}
