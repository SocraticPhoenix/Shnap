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
package com.gmail.socraticphoenix.shnap.app;

import com.gmail.socraticphoenix.collect.coupling.Switch;
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.compiler.ShnapCompiler;
import com.gmail.socraticphoenix.shnap.compiler.ShnapDefaultHandlers;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.env.ShnapScriptInvalidSyntaxException;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.parse.ShnapParser;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.ShnapScript;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.instructions.ShnapInstructionSequence;
import com.gmail.socraticphoenix.shnap.program.natives.ShnapArrayNative;
import com.gmail.socraticphoenix.shnap.program.natives.ShnapDefaultNatives;
import com.gmail.socraticphoenix.shnap.program.natives.ShnapStringNative;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShnapMain {

    public static void main(String[] a) {
        ShnapDefaultHandlers.registerDefaults();
        ShnapDefaultNatives.registerDefaults();

        Function<String, String> validPath = s -> {
            try {
                Paths.get(s);
                return null;
            } catch (InvalidPathException e) {
                return "Invalid path: " + s;
            }
        };

        Function<String, String> validExistingPath = s -> {
            try {
                if (!Files.exists(Paths.get(s))) {
                    return "Invalid path: " + s;
                }
                return null;
            } catch (InvalidPathException e) {
                return "Invalid path: " + s;
            }
        };

        Function<String, String> validMultiPath = s -> {
            String[] parts = s.split(";");
            for (String piece : parts) {
                String ver = validExistingPath.apply(piece);
                if (ver != null) {
                    return ver;
                }
            }
            return null;
        };

        Function<String, String> validScript = s -> {
            if (s.isEmpty()) {
                return "Invalid script name: " + s;
            } else {
                char[] chars = s.toCharArray();
                if (!Character.isJavaIdentifierStart(chars[0])) {
                    return "Invalid script name: " + s;
                }
                for (int i = 1; i < chars.length; i++) {
                    char c = chars[i];
                    if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                        return "Invalid script name: " + s;
                    }
                }

                return null;
            }
        };

        ArgumentParser parser = new ArgumentParser()
                .flag("arg", "The file(s)/script(s) to execute/compile", s -> {
                    String vs = validScript.apply(s);
                    String vf = validPath.apply(s);
                    if (vs == null || vf == null) {
                        return null;
                    } else {
                        return vs + " AND " + vf;
                    }
                })
                .flag("natives", "A semi-colon separated list of files to use as native builtins", validMultiPath)
                .flag("prelib", "A semi-colon separated list of files to use as prelibs", validMultiPath)
                .flag("builtin", "A semi-colon separated list of files to use as builtins", validMultiPath)
                .flag("path", "A semi-colon separated list of files to use when searching for imports and scripts", validMultiPath)
                .flag("compile", "The directory to compile", validPath)
                .flag("keepScripts", "If present, scripts and .sar files will be compiled", s -> null)
                .flag("archive", "The name of the archive", s -> null)
                .flag("exec", "A name of a script to execute", validScript)
                .flag("reloadHome", "If present, update the standard libraries", s -> null)
                .flag("debug", "If present, tracebacks will be considerably more detailed", s -> null)
                .flag("shell", "If present, arg, exec and compile flags will be ignored and the Shnap shell will start", s -> null);

        Switch<Arguments, String> as = parser.parse(a);
        boolean needsHelp = false;
        if (as.containsB()) {
            System.out.println(as.getB().get());
            needsHelp = true;
        } else {
            Arguments args = as.getA().get();

            List<String> scriptArgsList = new ArrayList<>();
            scriptArgsList.addAll(args.getArguments());
            ShnapObject[] scriptArgsArr = new ShnapObject[scriptArgsList.size()];
            for (int i = 0; i < scriptArgsList.size(); i++) {
                scriptArgsArr[i] = new ShnapStringNative(ShnapLoc.BUILTIN, scriptArgsList.get(i));
            }
            ShnapArrayNative scriptArgs = new ShnapArrayNative(ShnapLoc.BUILTIN, scriptArgsArr);

            String file = args.getFlag("arg");
            int mode = 0;
            String target;
            if (args.hasFlag("exec")) {
                mode = 1;
                target = args.getFlag("exec");
                if (file == null) {
                    needsHelp = true;
                    mode = -1;
                    System.out.println("required -arg flag");
                }
            } else if (args.hasFlag("compile")) {
                mode = 3;
                target = args.getFlag("compile");
                if (file == null) {
                    needsHelp = true;
                    mode = -1;
                    System.out.println("required -arg flag");
                }
            } else if (args.hasFlag("shell")) {
                mode = 4;
                target = null;
            } else {
                mode = 2;
                target = file;
                if (file == null) {
                    needsHelp = true;
                    mode = -1;
                    System.out.println("required -arg flag");
                }
            }

            ShnapEnvironment environment = new ShnapEnvironment();
            environment.setArguments(scriptArgs);
            environment.setMetaEnabled(args.hasFlag("debug"));
            try {
                environment.addAndSetupDefaultPaths(args.hasFlag("reloadHome"));
            } catch (IOException e) {
                System.err.println("Failed to load/setup SHNAP_HOME");
                e.printStackTrace();
                return;
            }

            switch (mode) {//1 = exec flag, 2 = exec arg, 3 = compile
                case 1: {
                    try {
                        String vf = validExistingPath.apply(file);
                        if (vf == null) {
                            environment.getNormalSearchLocs().add(Paths.get(file));
                            ShnapExecution nativeLoad = environment.loadNatives();
                            if (nativeLoad.isAbnormal()) {
                                notifyAbnormalState(nativeLoad, environment);
                                System.exit(1);
                                return;
                            }
                            ShnapExecution builtinsLoad = environment.loadBuiltins();
                            if (builtinsLoad.isAbnormal()) {
                                notifyAbnormalState(builtinsLoad, environment);
                                System.exit(1);
                                return;
                            }
                            environment.loadNormal();

                            ShnapScript script = environment.getModule(target);
                            ShnapExecution ex = script.runMain(environment);
                            if (ex.isAbnormal()) {
                                notifyAbnormalState(ex, environment);
                                System.exit(1);
                                return;
                            }
                        } else {
                            System.out.println(vf);
                            needsHelp = true;
                        }
                    } catch (ShnapScriptInvalidSyntaxException e) {
                        System.out.println(e.getCause().formatSafely());
                    } catch (IOException e) {
                        System.out.println("Failed to compile");
                        e.printStackTrace(System.out);
                        needsHelp = true;
                    }
                    break;
                }
                case 2: {
                    try {
                        environment.getNormalSearchLocs().add(Paths.get(System.getProperty("user.dir")));
                        ShnapExecution nativeLoad = environment.loadNatives();
                        if (nativeLoad.isAbnormal()) {
                            notifyAbnormalState(nativeLoad, environment);
                            System.exit(1);
                            return;
                        }
                        ShnapExecution builtinsLoad = environment.loadBuiltins();
                        if (builtinsLoad.isAbnormal()) {
                            notifyAbnormalState(builtinsLoad, environment);
                            System.exit(1);
                            return;
                        }
                        environment.loadNormal();

                        ShnapScript script = environment.getModule(target);
                        ShnapExecution ex = script.runMain(environment);
                        if (ex.isAbnormal()) {
                            notifyAbnormalState(ex, environment);
                        }
                    } catch (ShnapScriptInvalidSyntaxException e) {
                        System.out.println(e.getCause().formatSafely());
                    } catch (IOException e) {
                        System.out.println("Failed to compile");
                        e.printStackTrace(System.out);
                        needsHelp = true;
                    }
                    break;
                }
                case 3: {
                    try {
                        String vf = validExistingPath.apply(target);

                        if (vf == null) {
                            environment.getNormalSearchLocs().clear();
                            environment.getNormalSearchLocs().add(Paths.get(target));
                            environment.transitionImmediatelyToNormal();
                            List<ShnapScript> parse = environment.parseAll();
                            Path dst = Paths.get(file);
                            Files.createDirectories(dst);
                            List<Path> created = new ArrayList<>();
                            for (ShnapScript script : parse) {
                                byte[] compiled = script.compile();
                                Path targetPath = script.getCompileDstPath(dst);
                                if (!Files.exists(targetPath)) {
                                    Files.createDirectories(targetPath.toAbsolutePath().getParent());
                                    Files.createFile(targetPath);
                                }
                                created.add(targetPath);
                                Files.write(targetPath, compiled, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                            }
                            Path arch = dst.resolve(args.getFlag("archive", "archive") + ".sar");
                            Files.deleteIfExists(arch);
                            Path temp = environment.getShnapHome().resolve("temp");
                            int k = 0;
                            while (Files.exists(temp)) {
                                temp = environment.getShnapHome().resolve("temp" + k++);
                            }
                            Files.createDirectories(temp);
                            Path archTemp = temp.resolve("archive.sar");
                            ShnapCompiler.zipFile(dst.toFile(), archTemp.toFile());
                            if (!args.hasFlag("keepScripts")) {
                                for (Path cre : created) {
                                    Files.deleteIfExists(cre);
                                }
                                for (Path pdr : Files.list(dst).collect(Collectors.toList())) {
                                    if (Files.isDirectory(pdr)) {
                                        Files.deleteIfExists(pdr);
                                    }
                                }
                            }
                            Files.copy(archTemp, arch);
                            ShnapCompiler.deleteDirectory(temp.toFile());
                        } else {
                            System.out.println(vf);
                            needsHelp = true;
                        }


                    } catch (ShnapScriptInvalidSyntaxException e) {
                        System.out.println("Failed to compile");
                        System.out.println(e.getCause().formatSafely());
                        System.exit(1);
                        return;
                    } catch (IOException e) {
                        System.out.println("Failed to compile");
                        e.printStackTrace(System.out);
                        System.exit(1);
                        return;
                    }
                    break;
                }
                case 4: {
                    try {
                        System.out.println("Starting shell...");
                        System.out.println("Loading natives...");
                        ShnapExecution nativeLoad = environment.loadNatives();
                        if (nativeLoad.isAbnormal()) {
                            notifyAbnormalState(nativeLoad, environment);
                            System.exit(1);
                            return;
                        }
                        System.out.println("Loaded natives.");
                        System.out.println("Loading builtins...");
                        ShnapExecution builtinsLoad = environment.loadBuiltins();
                        if (builtinsLoad.isAbnormal()) {
                            notifyAbnormalState(builtinsLoad, environment);
                            System.exit(1);
                            return;
                        }
                        System.out.println("Loaded builtins");
                        System.out.println("Shell started...");
                        System.out.println("----------------------------------");
                        System.out.println("Welcome to the Shnap shell!");
                        System.out.println("Type in statements to evaluate them");
                        System.out.println("Type 'return' to exit");
                        System.out.println("Prefix a line with & to store it");
                        System.out.println("Stored lines will be parsed and executed with the next line not prefixed with &");
                        System.out.println("----------------------------------");
                        environment.loadNormal();

                        ShnapScript script = new ShnapScript("shell", "shell");
                        environment.applyDefaults(script);

                        StringBuilder content = new StringBuilder();
                        Scanner scanner = new Scanner(System.in);
                        while (true) {
                            System.out.print("> ");
                            String line = scanner.nextLine();

                            if (line.startsWith("&")) {
                                content.append(Strings.cutFirst(line)).append("\n");
                            } else {
                                content.append(line);
                                String contentStr = content.toString();
                                script.setContent(() -> contentStr);
                                content = new StringBuilder();
                                ShnapParser shnapParser = new ShnapParser(contentStr, script);
                                try {
                                    ShnapInstructionSequence seq = shnapParser.parseAll();
                                    environment.getTracebackStack().clear();

                                    ShnapExecution execution = seq.exec(script.getContext(), environment);
                                    if(execution.getState() == ShnapExecution.State.RETURNING) {
                                        System.out.println("Shell terminated");
                                        return;
                                    } else if (execution.isAbnormal()) {
                                        notifyAbnormalState(execution, environment);
                                    } else {
                                        System.out.print("Execution Value: ");
                                        ShnapExecution str = execution.getValue().asString(environment);
                                        if(str.isAbnormal()) {
                                            System.out.println();
                                            System.out.println("Failed to convert to string: ");
                                            notifyAbnormalState(execution, environment);
                                        } else {
                                            System.out.println(((ShnapStringNative) str.getValue()).getValue());
                                        }
                                    }
                                } catch (ShnapScriptInvalidSyntaxException e) {
                                    System.out.println(e.getCause().formatSafely());
                                } catch (ShnapParseError e) {
                                    System.out.println(e.formatSafely());
                                }


                            }
                        }


                    } catch (IOException e) {
                        System.out.println("IOException occured");
                        e.printStackTrace(System.out);
                        needsHelp = true;
                    }
                    break;
                }
            }
        }


        if (needsHelp) {
            System.out.println(parser.help());
            System.exit(1);
        }
    }

    private static void notifyAbnormalState(ShnapExecution ex, ShnapEnvironment environment) {
        System.out.println("Warning: abnormal state: " + ex.getState());
        if(!environment.getTracebacks().isEmpty()) {
            System.out.println("Traceback:");
            System.out.print(environment.formatTraceback());
        }
        System.out.println("State value:");
        ShnapExecution stringAttempt = ex.getValue().asString(environment);
        if (stringAttempt.isAbnormal()) {
            System.out.println("void");
        } else {
            System.out.println(((ShnapStringNative) stringAttempt.getValue()).getValue());
        }
    }

}
