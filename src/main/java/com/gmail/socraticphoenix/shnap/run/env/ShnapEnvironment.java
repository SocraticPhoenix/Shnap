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
package com.gmail.socraticphoenix.shnap.run.env;

import com.gmail.socraticphoenix.collect.coupling.Pair;
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.resources.ShnapResourceManager;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerUtil;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.literalObj;
import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.makeExceptionObj;

public class ShnapEnvironment {
    private List<Path> nativeSearchLocs = new ArrayList<>();
    private List<Pair<Path, Boolean>> preNormalSearchLocs = new ArrayList<>();
    private List<Path> normalSearchLocs = new ArrayList<>();

    private List<ShnapScript> nativeScripts = new ArrayList<>();
    private List<ShnapScript> builtinScripts = new ArrayList<>();

    private Stack<ShnapTraceback> tracebackStack = new Stack<>();
    private List<Path> workingSearchLocs;
    private ReferenceFactory referenceFactory;
    private State state;

    private ShnapObject arguments;

    private boolean metaEnabled;

    public ShnapEnvironment() {
        this.referenceFactory = new ReferenceFactory(this);
        this.state = State.NATIVES;
        this.arguments = new ShnapArrayNative(ShnapLoc.BUILTIN, 0);
    }

    public boolean isMetaEnabled() {
        return metaEnabled;
    }

    public void setMetaEnabled(boolean metaEnabled) {
        this.metaEnabled = metaEnabled;
    }

    public ShnapObject getArguments() {
        return arguments;
    }

    public void setArguments(ShnapObject arguments) {
        this.arguments = arguments;
    }

    public ReferenceFactory getReferenceFactory() {
        return this.referenceFactory;
    }

    public void reloadHome(Path home) throws IOException {
        Files.createDirectories(home);
        ShnapResourceManager.exportResource("shnap.zip", home.resolve("shnap.zip").toFile());
        ShnapCompilerUtil.unZipFile(home.resolve("shnap.zip").toFile(), home.toFile());
        Files.deleteIfExists(home.resolve("shnap.zip"));
    }

    public void addAndSetupDefaultPaths(Path home, boolean reloadHome) throws IOException {
        if (!Files.exists(home) || reloadHome) {
            this.reloadHome(home);
        }

        this.preNormalSearchLocs.add(Pair.of(home.resolve("builtins"), true));
        this.preNormalSearchLocs.add(Pair.of(home.resolve("prelib"), false));

        this.nativeSearchLocs.add(home.resolve("natives"));
        this.normalSearchLocs.add(home.resolve("lib"));
    }

    public ShnapExecution loadNatives() throws IOException {
        this.checkState(State.NATIVES);
        this.workingSearchLocs = this.nativeSearchLocs;
        this.referenceFactory.indexArchives();
        for (String module : this.referenceFactory.findAllModules()) {
            ShnapExecution exec = this.getModuleExecution(module);
            if (exec.isAbnormal()) {
                return exec;
            }
            this.nativeScripts.add((ShnapScript) exec.getValue());
        }
        this.transitionState(State.NATIVES, State.BUILTINS);
        return ShnapExecution.normal(ShnapObject.getVoid(), this, ShnapLoc.BUILTIN);
    }

    public ShnapExecution loadBuiltins() throws IOException {
        this.checkState(State.BUILTINS);
        List<Pair<String, Boolean>> preNormalModules = new ArrayList<>();
        List<Path> builtins = this.preNormalSearchLocs.stream().filter(Pair::getB).map(Pair::getA).collect(Collectors.toList());
        List<Path> preLibs = this.preNormalSearchLocs.stream().filter(p -> !p.getB()).map(Pair::getA).collect(Collectors.toList());
        this.workingSearchLocs = builtins;
        this.referenceFactory.indexArchives();
        preNormalModules.addAll(this.referenceFactory.findAllModules().stream().map(s -> Pair.of(s, true)).collect(Collectors.toList()));
        this.workingSearchLocs = preLibs;
        this.referenceFactory.indexArchives();
        preNormalModules.addAll(this.referenceFactory.findAllModules().stream().map(s -> Pair.of(s, false)).collect(Collectors.toList()));
        this.workingSearchLocs = new ArrayList<>();
        this.workingSearchLocs.addAll(builtins);
        this.workingSearchLocs.addAll(preLibs);
        this.referenceFactory.indexArchives();
        for (Pair<String, Boolean> module : preNormalModules) {
            ShnapExecution exec = this.getModuleExecution(module.getA());
            if (exec.isAbnormal()) {
                return exec;
            }
            ShnapScript script = (ShnapScript) exec.getValue();
            if (module.getB()) {
                this.builtinScripts.add(script);
            }
        }
        this.transitionState(State.BUILTINS, State.PRE_NORMAL);
        return ShnapExecution.normal(ShnapObject.getVoid(), this, ShnapLoc.BUILTIN);
    }

    public void loadNormal() throws IOException {
        this.checkState(State.PRE_NORMAL);
        this.workingSearchLocs = this.normalSearchLocs;
        this.referenceFactory.indexArchives();
        this.transitionState(State.PRE_NORMAL, State.NORMAL);
    }

    public ShnapExecution getModuleExecution(String name) {
        try {
            ShnapScript script = this.getModule(name);
            if (script.getInitExec().isAbnormal()) {
                return script.getInitExec();
            }
            return ShnapExecution.normal(script, this, script.getLocation());
        } catch (ShnapScriptAbsentException e) {
            return ShnapExecution.throwing(makeExceptionObj("shnap.ImportError", e.getMessage(), literalObj("shnap.load.AbsetScript")), this, ShnapLoc.BUILTIN);
        } catch (ShnapScriptCircularInitException e) {
            return ShnapExecution.throwing(makeExceptionObj("shnap.ImportError", e.getMessage(), literalObj("shnap.load.CircularInit")), this, ShnapLoc.BUILTIN);
        } catch (ShnapScriptInvalidSyntaxException e) {
            return ShnapExecution.throwing(makeExceptionObj("shnap.ImportError", e.getMessage(), literalObj("shnap.load.InvalidSyntax")), this, ShnapLoc.BUILTIN);
        } catch (ShnapScriptLoadingFailedException e) {
            return ShnapExecution.throwing(ShnapFactory.mimicJavaException("shnap.ImportError", "Failed to load script internally", e), this, ShnapLoc.BUILTIN);
        }
    }

    public void transitionImmediatelyToNormal() {
        this.transitionState(State.NATIVES, State.NORMAL);
    }

    public List<ShnapScript> parseAll() throws IOException {
        try {
            this.workingSearchLocs = this.normalSearchLocs;
            this.referenceFactory.indexArchives();
            List<ShnapScript> scripts = new ArrayList<>();
            for (String module : this.referenceFactory.findAllModules()) {
                scripts.add(this.referenceFactory.load(this, module));
            }
            return scripts;
        } catch (ShnapParseError e) {
            throw new ShnapScriptInvalidSyntaxException(e);
        }
    }

    public ShnapScript getModule(String name) {
        return this.referenceFactory.getModule(this, name);
    }

    public void applyDefaults(ShnapContext target) {
        switch (this.state) {
            case BUILTINS:
                for (ShnapScript nativeScript : this.nativeScripts) {
                    nativeScript.importBuiltinsTo(target);
                }
                break;
            case NORMAL:
                for (ShnapScript nativeScript : this.nativeScripts) {
                    nativeScript.importBuiltinsTo(target);
                }
                for (ShnapScript builtin : this.builtinScripts) {
                    builtin.importBuiltinsTo(target);
                }
                break;
        }
    }

    public void applyDefaults(ShnapScript target) {
        this.applyDefaults(target.getContext());
    }

    public List<Pair<Path, Boolean>> getPreNormalSearchLocs() {
        return this.preNormalSearchLocs;
    }

    public List<Path> getNativeSearchLocs() {
        return this.nativeSearchLocs;
    }

    public List<Path> getNormalSearchLocs() {
        return this.normalSearchLocs;
    }

    public List<ShnapScript> getNativeScripts() {
        return this.nativeScripts;
    }

    public List<ShnapScript> getBuiltinScripts() {
        return this.builtinScripts;
    }

    public List<Path> getWorkingSearchLocs() {
        return this.workingSearchLocs;
    }

    public void pushTraceback(ShnapTraceback traceback) {
        if (!traceback.isMeta() || this.isMetaEnabled()) {
            this.tracebackStack.push(traceback);
        }
    }

    public void popTraceback() {
        while (this.tracebackStack.peek().isDetail()) {
            this.tracebackStack.pop();
        }
        this.tracebackStack.pop();
    }

    public List<ShnapTraceback> getTracebacks() {
        return new ArrayList<>(this.tracebackStack);
    }

    public String formatTraceback() {
        int lineCount = 1;

        StringBuilder builder = new StringBuilder();
        for (ShnapTraceback traceback : this.getTracebacks()) {
            ShnapLoc loc = traceback.getLoc();
            if (!traceback.isMeta() || this.isMetaEnabled()) {
                if (traceback.isDetail()) {
                    builder.append(Strings.indent(1));
                    if (traceback.isMeta()) {
                        builder.append(Strings.indent(1));
                    }
                }
                if(loc != ShnapLoc.BUILTIN) {
                    builder.append("(line: ").append(loc.getLine() + 1).append(", col: ").append(loc.getCol() + 1).append(", script: ").append(loc.getScript().getName()).append(") ").append(traceback.getDesc()).append(System.lineSeparator());
                } else {
                    builder.append("(").append(loc.getScript().getName()).append(") ").append(traceback.getDesc()).append(System.lineSeparator());
                }
                if (this.isMetaEnabled()) {
                    int indent = (traceback.isMeta() ? 1 : 0) + (traceback.isDetail() ? 1 : 0);
                    String error = null;
                    try {
                        error = ShnapParseError.format(loc, traceback.getDesc(), lineCount, false, true, false, indent);
                    } catch (IOException e) {
                        error = Strings.indent(indent) + "<unknown source>" + System.lineSeparator();
                    }
                    builder.append(error);
                }
            }
        }


        return builder.toString();
    }

    public void checkState(State state) {
        if (this.state != state) {
            throw new IllegalStateException(state.name());
        }
    }

    public void transitionState(State from, State to) {
        checkState(from);
        this.state = to;
    }

    public Stack<ShnapTraceback> getTracebackStack() {
        return tracebackStack;
    }

    public void notifyAbnormalState(Consumer<String> log, ShnapExecution ex) {
        log.accept("Warning: abnormal state: " + ex.getState() + System.lineSeparator());
        if(!this.getTracebacks().isEmpty()) {
            log.accept("Traceback:" + System.lineSeparator());
            log.accept(this.formatTraceback());
        }
        log.accept(System.lineSeparator() + "State value:" + System.lineSeparator());
        ShnapExecution stringAttempt = ex.getValue().asString(this);
        if (stringAttempt.isAbnormal()) {
            log.accept("void" + System.lineSeparator());
        } else {
            log.accept(((ShnapStringNative) stringAttempt.getValue()).getValue() + System.lineSeparator());
        }
    }

    private enum State {
        NATIVES,
        BUILTINS,
        PRE_NORMAL,
        NORMAL
    }

}
