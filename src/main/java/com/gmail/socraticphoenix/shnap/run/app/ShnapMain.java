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
package com.gmail.socraticphoenix.shnap.run.app;

import com.gmail.socraticphoenix.collect.coupling.Switch;
import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.parse.ShnapParseError;
import com.gmail.socraticphoenix.shnap.plugin.ShnapPluginLoader;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompiler;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapCompilerSettings;
import com.gmail.socraticphoenix.shnap.run.compiler.ShnapDefaultHandlers;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironmentSettings;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptAbsentException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptCircularInitException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptInvalidSyntaxException;
import com.gmail.socraticphoenix.shnap.run.env.ShnapScriptLoadingFailedException;
import com.gmail.socraticphoenix.shnap.run.executor.ShnapExecutionSettings;
import com.gmail.socraticphoenix.shnap.run.executor.ShnapExecutor;
import com.gmail.socraticphoenix.shnap.run.shell.ShnapShell;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapDefaultNatives;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

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
                .flag("plugins", "A semi-colon separated list of files to use when searching for plugins", validMultiPath)
                .flag("compile", "The directory to compile", validPath)
                .flag("keepScripts", "If present, scripts and .sar files will be compiled", s -> null)
                .flag("archive", "The name of the archive", s -> null)
                .flag("exec", "A name of a script to execute", validScript)
                .flag("reloadHome", "If present, update the standard libraries", s -> null)
                .flag("noSource", "If present, tracebacks will not display source code (only line & column numbers)", s -> null)
                .flag("shell", "If present, arg, exec and compile flags will be ignored and the Shnap shell will start", s -> null);

        Switch<Arguments, String> as = parser.parse(a);
        boolean needsHelp = false;
        if (as.containsB()) {
            System.err.println(as.getB().get());
            needsHelp = true;
        } else {
            Arguments args = as.getA().get();

            List<Path> natives = readPaths(args.getFlag("natives", ""));
            List<Path> prelib = readPaths(args.getFlag("prelib", ""));
            List<Path> builtin = readPaths(args.getFlag("builtin", ""));
            List<Path> normal = readPaths(args.getFlag("path", ""));
            List<Path> plugins = readPaths(args.getFlag("plugins", ""));

            ShnapEnvironmentSettings environmentSettings = new ShnapEnvironmentSettings().setHomeToDefault();

            ShnapExecutionSettings settings = new ShnapExecutionSettings()
                    .setEnvironmentSettings(environmentSettings)
                    .addDefaultPaths()
                    .setDebug(!args.hasFlag("noSource"))
                    .setReloadHome(args.hasFlag("reloadHome"));
            settings.getNatives().addAll(natives);
            settings.getPrelib().addAll(prelib);
            settings.getBuiltin().addAll(builtin);
            settings.getNormal().addAll(normal);

            if (!args.hasFlag("compile")) {
                if (args.hasFlag("shell")) {
                    ShnapShell shell = new ShnapShell(settings);
                    Scanner scanner = new Scanner(System.in);
                    try {
                        shell.performPreLoading(plugins);
                        if(!loadPlugins(environmentSettings, plugins)) {
                            return;
                        }

                        ShnapExecution ex = shell.performInitialLoading(System.out::print);
                        if(ex.isAbnormal()) {
                            System.err.println("Failed to load libraries...");
                            shell.getEnvironment().notifyAbnormalState(System.err::print, ex);
                            System.exit(1);
                            return;
                        }
                        System.out.println("---------------------------------");
                        System.out.println("Welcome to the Shnap Shell!");
                        System.out.println("- Type in statements to see their execution value");
                        System.out.println("- End a line with ! to store it");
                        System.out.println("- Stored lines will be executed with the first line not ending in !");
                        System.out.println("---------------------------------");
                        shell.setupScript();
                        while (true) {
                            System.out.print("> ");
                            String line = scanner.nextLine();
                            if (line.endsWith("!")) {
                                shell.store(Strings.cutLast(line));
                            } else {
                                try {
                                    ShnapExecution execution = shell.execute(line);
                                    if (execution.getState() == ShnapExecution.State.RETURNING) {
                                        return;
                                    } else if (execution.getState().isAbnormal()) {
                                        shell.getEnvironment().notifyAbnormalState(System.out::print, execution);
                                    } else {
                                        ShnapExecution val = execution.getValue().asString(shell.getEnvironment());
                                        String res;
                                        if (val.isAbnormal()) {
                                            res = execution.getValue().defaultToString();
                                        } else {
                                            res = ((ShnapStringNative) val.getValue()).getValue();
                                        }
                                        System.out.println("Execution value: " + res);
                                    }
                                } catch (ShnapParseError e) {
                                    System.out.println(e.formatSafely());
                                }
                            }
                        }
                    }  catch (IOException | ShnapScriptLoadingFailedException | ShnapScriptAbsentException | ShnapScriptCircularInitException e) {
                        System.err.println("Failed to load library scripts");
                        e.printStackTrace();
                        System.exit(1);
                        return;
                    } catch (ShnapScriptInvalidSyntaxException e) {
                        System.err.println("Failed to load library scripts");
                        System.err.println(e.getCause().formatSafely());
                        System.exit(1);
                        return;
                    }
                } else {
                    if(!args.hasFlag("arg")) {
                        System.err.println("Required arg flag");
                        needsHelp = true;
                    } else {
                        Path toAdd;
                        String scriptName;
                        if (args.hasFlag("exec")) {
                            toAdd = Paths.get(args.getFlag("arg"));
                            scriptName = args.getFlag("exec");
                        } else {
                            toAdd = Paths.get(System.getProperty("user.dir"));
                            scriptName = args.getFlag("arg");
                        }

                        settings.addNormalPath(toAdd);
                        for(String arg : args.getArguments()) {
                            settings.addArgument(arg);
                        }

                        ShnapExecutor executor = new ShnapExecutor(settings);
                        try {
                            executor.performPreLoading(plugins);
                            if(!loadPlugins(environmentSettings, plugins)) {
                                return;
                            }

                            ShnapExecution ex = executor.performInitialLoading();
                            if(ex.isAbnormal()) {
                                System.err.println("Failed to load libraries...");
                                executor.getEnvironment().notifyAbnormalState(System.err::print, ex);
                                System.exit(1);
                                return;
                            }
                            ShnapExecution execution = executor.execute(scriptName);
                            if(execution.isAbnormal()) {
                                executor.getEnvironment().notifyAbnormalState(System.err::print, execution);
                            }
                        } catch (IOException | ShnapScriptLoadingFailedException | ShnapScriptAbsentException | ShnapScriptCircularInitException e) {
                            System.err.println("Failed to load scripts");
                            e.printStackTrace(System.err);
                            System.exit(1);
                            return;
                        } catch (ShnapScriptInvalidSyntaxException e) {
                            System.err.println(e.getCause().formatSafely());
                            System.exit(1);
                            return;
                        }
                    }
                }
            } else {
                ShnapCompilerSettings compilerSettings = new ShnapCompilerSettings()
                        .setEnvironmentSettings(environmentSettings)
                        .setKeepScripts(args.hasFlag("keepScripts"))
                        .setReloadHome(args.hasFlag("reloadHome"))
                        .setArchiveName(args.getFlag("archive", "archive"))
                        .setOutputDir(Paths.get(args.getFlag("arg")))
                        .addCompileDir(Paths.get(args.getFlag("compile")));

                ShnapCompiler compiler = new ShnapCompiler(compilerSettings);
                try {
                    compiler.compile();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                    return;
                } catch (ShnapScriptInvalidSyntaxException e) {
                    System.err.println(e.getCause().formatSafely());
                    System.exit(1);
                    return;
                }

            }

        }


        if (needsHelp) {
            System.err.println(parser.help());
            System.exit(1);
        }
    }

    private static boolean loadPlugins(ShnapEnvironmentSettings settings, List<Path> plugins) {
        ShnapPluginLoader loader = new ShnapPluginLoader(ShnapMain.class.getClassLoader());
        loader.addDefaultPath(settings);
        loader.getPaths().addAll(plugins);

        try {
            loader.index();
        } catch (IOException e) {
            System.err.println("Failed to index plugins!");
            e.printStackTrace();
            System.exit(1);
            return false;
        }

        try {
            loader.load();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            System.err.println("Failed to load plugins!");
            e.printStackTrace();
            System.exit(1);
            return false;
        }

        return true;
    }

    private static List<Path> readPaths(String pth) {
        if (pth.trim().equals("")) {
            return Collections.emptyList();
        }
        String[] pieces = pth.split(";");
        List<Path> res = new ArrayList<>();
        for (String k : pieces) {
            Path p = Paths.get(k);
            if (Files.exists(p)) {
                res.add(p);
            }
        }
        return res;
    }

}
