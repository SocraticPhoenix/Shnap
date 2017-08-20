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

package com.gmail.socraticphoenix.shnap.run.executor;

import com.gmail.socraticphoenix.collect.coupling.Pair;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironmentSettings;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class


ShnapExecutionSettings {
    private List<Path> natives = new ArrayList<>();
    private List<Path> builtin = new ArrayList<>();
    private List<Path> prelib = new ArrayList<>();
    private List<Path> normal = new ArrayList<>();
    private List<ShnapObject> arguments = new ArrayList<>();

    private boolean reloadHome;
    private boolean debug;

    private ShnapEnvironmentSettings environmentSettings;

    public ShnapExecutionSettings addDefaultPaths(Path home) {
        this.builtin.add(home.resolve("builtins"));
        this.prelib.add(home.resolve("prelib"));
        this.natives.add(home.resolve("natives"));
        this.normal.add(home.resolve("lib"));
        return this;
    }

    public ShnapObject buildArguments() {
        ShnapObject[] args = this.arguments.toArray(new ShnapObject[this.arguments.size()]);
        return new ShnapArrayNative(ShnapLoc.BUILTIN, args);
    }

    public ShnapEnvironment buildEnvironment() {
        ShnapEnvironment environment = new ShnapEnvironment();
        environment.getNativeSearchLocs().addAll(this.natives);
        environment.getNormalSearchLocs().addAll(this.normal);
        environment.getPreNormalSearchLocs().addAll(this.builtin.stream().map(p -> Pair.of(p, true)).collect(Collectors.toList()));
        environment.getPreNormalSearchLocs().addAll(this.prelib.stream().map(p -> Pair.of(p, false)).collect(Collectors.toList()));
        environment.setMetaEnabled(this.debug);
        environment.setArguments(this.buildArguments());
        return environment;
    }

    public void applyHomeSettings(ShnapEnvironment environment) throws IOException {
        if(this.environmentSettings == null) {
            throw new IllegalStateException("call to applyHomeSettings(ShnapEnvironment) with null environment settings");
        }
        if(this.reloadHome || !Files.exists(this.environmentSettings.getHome())) {
            environment.reloadHome(this.environmentSettings.getHome());
        }
    }

    public ShnapExecutionSettings addArgument(ShnapObject object) {
        this.arguments.add(object);
        return this;
    }

    public List<ShnapObject> getArguments() {
        return this.arguments;
    }

    public ShnapExecutionSettings addArgument(String argument) {
        return this.addArgument(new ShnapStringNative(ShnapLoc.BUILTIN, argument));
    }

    public ShnapExecutionSettings addDefaultPaths() {
        if(this.environmentSettings == null) {
            throw new IllegalStateException("call to addDefaultPaths() without path arg and with null environment settings");
        }

        return this.addDefaultPaths(this.environmentSettings.getHome());
    }

    public ShnapExecutionSettings addNativePath(Path path) {
        return this.addPath(this.natives, path);
    }

    public ShnapExecutionSettings addBuiltinPath(Path path) {
        return this.addPath(this.builtin, path);
    }

    public ShnapExecutionSettings addPrelibPath(Path path) {
        return this.addPath(this.prelib, path);
    }

    public ShnapExecutionSettings addNormalPath(Path path) {
        return this.addPath(this.normal, path);
    }

    public List<Path> getNatives() {
        return this.natives;
    }

    public List<Path> getBuiltin() {
        return this.builtin;
    }

    public List<Path> getPrelib() {
        return this.prelib;
    }

    public List<Path> getNormal() {
        return this.normal;
    }

    public boolean isReloadHome() {
        return this.reloadHome;
    }

    public ShnapExecutionSettings setReloadHome(boolean reloadHome) {
        this.reloadHome = reloadHome;
        return this;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public ShnapExecutionSettings setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public ShnapEnvironmentSettings getEnvironmentSettings() {
        return this.environmentSettings;
    }

    public ShnapExecutionSettings setEnvironmentSettings(ShnapEnvironmentSettings environmentSettings) {
        this.environmentSettings = environmentSettings;
        return this;
    }

    private ShnapExecutionSettings addPath(List<Path> lst, Path path) {
        lst.add(path);
        return this;
    }
}
