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

package com.gmail.socraticphoenix.shnap.run.shell;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.parse.ShnapParser;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.run.compiler.DangerousSupplier;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptAbsentException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptCircularInitException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptInvalidSyntaxException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptLoadingFailedException;
import com.gmail.socraticphoenix.shnap.run.executor.ShnapExecutionSettings;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ShnapShell {
    private ShnapExecutionSettings settings;
    private ShnapEnvironment environment;

    public ShnapShell(ShnapExecutionSettings settings) {
        this.settings = settings;
    }

    public ShnapExecution performInitialLoading(Consumer<String> log)  throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        return this.performInitialLoading(log, true);
    }

    public ShnapExecution performInitialLoading()  throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
        return this.performInitialLoading(null, false);
    }

    public void performPreLoading(List<Path> other) throws IOException {
        this.environment = this.settings.buildEnvironment();
        this.settings.applyHomeSettings(this.environment, other);
    }

    public ShnapExecution performInitialLoading(Consumer<String> theLog, boolean log) throws IOException, ShnapScriptAbsentException, ShnapScriptCircularInitException, ShnapScriptInvalidSyntaxException, ShnapScriptLoadingFailedException {
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

    private StringBuilder fullContent = new StringBuilder();
    private StringBuilder content = new StringBuilder();
    private ShnapScript script = new ShnapScript("shell", "shell"); //Dummy script to represent the shell
    private ShnapLoc start = new ShnapLoc(0, 0, script);

    public void setupScript() {
        this.environment.applyDefaults(this.script);
    }

    public ShnapShell store(String line) {
        this.content.append(line).append("\n");
        this.fullContent.append(line).append("\n");
        return this;
    }

    public ShnapExecution execute(String line) throws ShnapParseError {
        this.content.append(line).append("\n");
        this.fullContent.append(line).append("\n");
        String content = this.content.toString();
        this.content = new StringBuilder();
        DangerousSupplier<String> contentGetter = () -> fullContent.toString();
        this.script.setContent(contentGetter);
        ShnapParser parser = new ShnapParser(content, start);
        ShnapInstruction all = parser.parseAll();
        int[][] pts = parser.getPts();
        int[] last = pts[pts.length - 1];
        this.start = new ShnapLoc(last[0], last[1], this.script);

        ShnapExecution exec = all.exec(this.script.getContext(), this.environment);
        if(exec.isAbnormal()) {
            return exec;
        } else {
            this.environment.getTracebackStack().clear();
            return exec;
        }
    }

    public ShnapEnvironment getEnvironment() {
        return this.environment;
    }

}
