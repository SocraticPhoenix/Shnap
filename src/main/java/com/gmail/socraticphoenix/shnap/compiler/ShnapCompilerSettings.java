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

package com.gmail.socraticphoenix.shnap.compiler;

import com.gmail.socraticphoenix.shnap.env.ShnapEnvironmentSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShnapCompilerSettings {
    private List<Path> compileDirs = new ArrayList<>();
    private List<Path> additionalFiles = new ArrayList<>();
    private Path outputDir;
    private String archiveName;
    private boolean keepScripts;
    private boolean reloadHome;

    private ShnapEnvironmentSettings environmentSettings;

    public boolean isReloadHome() {
        return this.reloadHome;
    }

    public List<Path> getAdditionalFiles() {
        return this.additionalFiles;
    }

    public ShnapCompilerSettings addAdditionalDirectory(Path file) {
        this.additionalFiles.add(file);
        return this;
    }

    public ShnapCompilerSettings setReloadHome(boolean reloadHome) {
        this.reloadHome = reloadHome;
        return this;
    }

    public List<Path> getCompileDirs() {
        return this.compileDirs;
    }

    public ShnapCompilerSettings addCompileDir(Path path) {
        this.compileDirs.add(path);
        return this;
    }

    public Path getOutputDir() {
        return this.outputDir;
    }

    public ShnapCompilerSettings setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public String getArchiveName() {
        return this.archiveName;
    }

    public ShnapCompilerSettings setArchiveName(String archiveName) {
        this.archiveName = archiveName;
        return this;
    }

    public boolean isKeepScripts() {
        return this.keepScripts;
    }

    public ShnapCompilerSettings setKeepScripts(boolean keepScripts) {
        this.keepScripts = keepScripts;
        return this;
    }

    public ShnapEnvironmentSettings getEnvironmentSettings() {
        return this.environmentSettings;
    }

    public ShnapCompilerSettings setEnvironmentSettings(ShnapEnvironmentSettings environmentSettings) {
        this.environmentSettings = environmentSettings;
        return this;
    }

}
