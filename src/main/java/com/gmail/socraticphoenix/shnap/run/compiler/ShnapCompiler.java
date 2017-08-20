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

package com.gmail.socraticphoenix.shnap.run.compiler;

import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptInvalidSyntaxException;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShnapCompiler {
    private ShnapCompilerSettings settings;

    public ShnapCompiler(ShnapCompilerSettings settings) {
        this.settings = settings;
    }

    public void compile() throws IOException, ShnapScriptInvalidSyntaxException {
        ShnapEnvironment environment = new ShnapEnvironment();
        Path home = this.settings.getEnvironmentSettings().getHome();
        if(this.settings.isReloadHome()) {
            environment.reloadHome(home);
        }

        environment.getNormalSearchLocs().addAll(this.settings.getCompileDirs());
        environment.transitionImmediatelyToNormal();
        List<ShnapScript> toCompile = environment.parseAll();

        Path temp = home.resolve("temp_0");
        int k = 1;
        while (Files.exists(temp)) {
            temp = home.resolve("temp_" + k++);
        }
        Files.createDirectories(temp);

        Path tempA = home.resolve("temp_0A");
        int z = 1;
        while (Files.exists(temp)) {
            temp = home.resolve("temp_" + z++ + "A");
        }
        Files.createDirectories(tempA);

        Path dest = this.settings.getOutputDir();
        Files.createDirectories(dest);
        List<Path> created = new ArrayList<>();
        for(ShnapScript script : toCompile) {
            byte[] compiled = script.compile();
            Path targetPath = script.getCompileDstPath(temp);
            if(Files.exists(targetPath)) {
                ShnapCompilerUtil.deleteDirectory(targetPath);
            }
            Files.createDirectories(targetPath.toAbsolutePath().getParent());
            Files.createFile(targetPath);
            created.add(targetPath);
            Files.write(targetPath, compiled);
        }
        for(Path additional : this.settings.getAdditionalFiles()) {
            List<Path> children = Files.walk(additional).collect(Collectors.toList());
            for(Path child : children) {
                Path target = temp.resolve(additional.relativize(child));
                Files.createDirectories(target.toAbsolutePath().getParent());
                Files.copy(child, target);
            }
        }
        Path archive = dest.resolve(this.settings.getArchiveName() + ".sar");
        Files.deleteIfExists(archive);
        Path tempArchive = tempA.resolve("archive.sar");
        ShnapCompilerUtil.zipFile(temp.toFile(), tempArchive.toFile());
        Files.copy(tempArchive, archive, StandardCopyOption.REPLACE_EXISTING);
        if(this.settings.isKeepScripts()) {
            for(Path script : created) {
                Files.copy(script, dest.resolve(temp.relativize(script)));
            }
        }

        ShnapCompilerUtil.deleteDirectory(temp);
        ShnapCompilerUtil.deleteDirectory(tempA);
    }

}
