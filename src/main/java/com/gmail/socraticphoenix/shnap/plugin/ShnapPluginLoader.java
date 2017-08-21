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

package com.gmail.socraticphoenix.shnap.plugin;

import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironmentSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class ShnapPluginLoader {
    private ClassLoader parent;

    private URLClassLoader classLoader;
    private List<Path> paths;
    private List<String> pluginClassNames;

    public ShnapPluginLoader(ClassLoader parent) {
        this.paths = new ArrayList<>();
        this.pluginClassNames = new ArrayList<>();
        this.parent = parent;
    }

    public void addDefaultPath(ShnapEnvironmentSettings settings) {
        this.paths.add(settings.getHome().resolve("plugins"));
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public List<Path> getPaths() {
        return this.paths;
    }

    public void index() throws IOException {
        List<URL> jars = new ArrayList<>();
        for (Path path : this.paths) {
            if (Files.exists(path)) {
                Stream<Path> files = Files.walk(path);
                List<Path> collected = files.collect(Collectors.toList());
                files.close();
                for (Path child : collected) {
                    if (child.toAbsolutePath().toString().endsWith(".jar")) {
                        jars.add(child.toUri().toURL());
                        JarFile file = new JarFile(child.toFile());
                        if (file.getEntry("shnap_plugin.txt") != null) {
                            ZipEntry conf = file.getEntry("shnap_plugin.txt");
                            InputStream stream = file.getInputStream(conf);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                            StringBuilder result = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                result.append(line).append("\n");
                            }

                            this.pluginClassNames.add(result.toString().trim());
                            reader.close();
                        }
                        file.close();
                    }
                }
            }
        }

        this.classLoader = new URLClassLoader(jars.toArray(new URL[jars.size()]), this.parent);
    }

    public void load() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for (String name : this.pluginClassNames) {
            Class cls = Class.forName(name, true, this.classLoader);
            if (Runnable.class.isAssignableFrom(cls)) {
                Runnable plugin = (Runnable) cls.newInstance();
                ShnapPluginRegistry.register(new ShnapPlugin(plugin, name));
            }
        }
    }

}
