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
import com.gmail.socraticphoenix.shnap.run.compiler.DangerousSupplier;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.type.object.ShnapScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReferenceFactory {
    private ShnapEnvironment environment;
    private Map<String, ShnapScript> scripts = new LinkedHashMap<>();
    private Stack<String> initTrace = new Stack<>();
    private List<Path> indexedArchives;


    public ReferenceFactory(ShnapEnvironment environment) {
        this.environment = environment;
    }

    public void indexArchives() throws IOException {
        this.indexedArchives = new ArrayList<>();
        for (Path path : environment.getWorkingSearchLocs()) {
            Files.walk(path).forEach(p -> {
                String fmt = format(p);
                if (fmt.equalsIgnoreCase("sar")) {
                    this.indexedArchives.add(p);
                }
            });
        }
    }

    public ShnapScript getModule(ShnapEnvironment tracer, String name) {
        if (this.scripts.containsKey(name)) {
            return this.scripts.get(name);
        } else {
            return this.loadAndInit(tracer, name);
        }
    }

    private Path workingApplicablePath = null;
    private Pair<String, String> workingNameAndFormat = null;

    public ShnapScript load(ShnapEnvironment environment, String name) {
        workingApplicablePath = null;
        workingNameAndFormat = null;

        try {
            for (Path path : environment.getWorkingSearchLocs()) {
                Files.walk(path).forEach(p -> {
                    p = path.relativize(p);
                    if (this.workingApplicablePath == null) {
                        Pair<String, String> nameAndFormat = this.nameAndFormat(p);
                        if (name.equals(nameAndFormat.getA())) {
                            String fmt = nameAndFormat.getB();
                            if (fmt.equalsIgnoreCase("shnap") || fmt.equalsIgnoreCase("cshnap")) {
                                this.workingApplicablePath = path.resolve(p).toAbsolutePath();
                                this.workingNameAndFormat = nameAndFormat;
                            }
                        }
                    }
                });
            }

            DangerousSupplier<InputStream> stream = null;
            String fileName = null;
            String format = null;
            if (this.workingApplicablePath != null) {
                Path path = this.workingApplicablePath;
                stream = () -> Files.newInputStream(path);
                fileName = this.workingApplicablePath.toString();
                format = this.workingNameAndFormat.getB();
            } else {
                archiveSearch:
                for (Path archive : this.indexedArchives) {
                    ZipFile zip = null;
                    try {
                        zip = new ZipFile(archive.toFile());
                        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            Pair<String, String> nameAndFormat = this.nameAndFormat(entry.getName());
                            if (name.equals(nameAndFormat.getA())) {
                                String fmt = nameAndFormat.getB();
                                if (fmt.equalsIgnoreCase("shnap") || fmt.equalsIgnoreCase("cshnap")) {
                                    String entryName = entry.getName();
                                    stream = () -> {
                                        ZipFile theFile = new ZipFile(archive.toFile());
                                        ZipEntry theFileEntry = theFile.getEntry(entryName);
                                        return new DelegateZipClosingStream(theFile.getInputStream(theFileEntry), theFile);
                                    };
                                    fileName = archive.toString() + "::" + entry.getName();
                                    format = fmt;
                                    break archiveSearch;
                                }
                            }
                        }
                    } finally {
                        if(zip != null) {
                            zip.close();
                        }
                    }
                }
            }

            if (stream == null) {
                throw new ShnapScriptAbsentException(name);
            } else {
                ShnapScript script = ShnapScript.read(stream, name, fileName, format);
                scripts.put(name, script);
                return script;
            }
        } catch (IOException e) {
            throw new ShnapScriptLoadingFailedException(e);
        } catch (ShnapParseError e) {
            throw new ShnapScriptInvalidSyntaxException(e);
        }
    }

    public ShnapScript loadAndInit(ShnapEnvironment tracer, String name) {
        if (this.initTrace.contains(name)) {
            throw circularError();
        } else {
            this.initTrace.push(name);
            ShnapScript loaded = this.load(tracer, name);
            loaded.initScript(tracer);
            this.initTrace.pop();
            return loaded;
        }
    }

    public List<String> findAllModules() {
        List<String> res = new ArrayList<>();
        try {
            for (Path path : environment.getWorkingSearchLocs()) {
                Files.walk(path).forEach(p -> {
                    p = path.relativize(p);
                    if (this.workingApplicablePath == null) {
                        Pair<String, String> nameAndFormat = this.nameAndFormat(p);
                        String fmt = nameAndFormat.getB();
                        if (fmt.equalsIgnoreCase("shnap") || fmt.equalsIgnoreCase("cshnap")) {
                            res.add(nameAndFormat.getA());
                        }
                    }
                });
            }
            for (Path archive : this.indexedArchives) {
                ZipFile zip = new ZipFile(archive.toFile());
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Pair<String, String> nameAndFormat = this.nameAndFormat(entry.getName());
                    String fmt = nameAndFormat.getB();
                    if (fmt.equalsIgnoreCase("shnap") || fmt.equalsIgnoreCase("cshnap")) {
                        res.add(nameAndFormat.getA());
                    }
                }
            }
        } catch (IOException e) {
            throw new ShnapScriptLoadingFailedException(e);
        } catch (ShnapParseError e) {
            throw new ShnapScriptInvalidSyntaxException(e);
        }

        return res;
    }

    private ShnapScriptCircularInitException circularError() {
        return new ShnapScriptCircularInitException(String.join("->", this.initTrace.toArray(new CharSequence[0])));
    }

    public String format(Path path) {
        String k = path.toString();
        int last = k.lastIndexOf('.');
        if (last == -1) {
            return "";
        } else {
            return k.substring(last + 1);
        }
    }

    public Pair<String, String> nameAndFormat(Path path) {
        String[] names = new String[path.getNameCount()];
        for (int i = 0; i < path.getNameCount(); i++) {
            names[i] = path.getName(i).getFileName().toString();
        }
        String file = names[names.length - 1];
        int last = file.lastIndexOf('.');
        if (last != -1) {
            String namePiece = file.substring(0, last);
            String fmtPiece = file.substring(last + 1);
            names[names.length - 1] = namePiece;
            return Pair.of(String.join(".", names), fmtPiece);
        } else {
            return Pair.of(String.join(".", names), "");
        }
    }

    public Pair<String, String> nameAndFormat(String path) {
        path = path.replace('/', '.');
        int last = path.lastIndexOf('.');
        if (last == -1) {
            return Pair.of(path, "");
        } else {
            return Pair.of(path.substring(0, last), path.substring(last + 1));
        }
    }

}
