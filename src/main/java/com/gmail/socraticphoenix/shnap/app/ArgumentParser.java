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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ArgumentParser {
    private List<ArgDef> args;
    private Map<String, ArgDef> flags;

    public ArgumentParser() {
        this.args = new ArrayList<>();
        this.flags = new LinkedHashMap<>();
    }

    public ArgumentParser flag(String name, String help, Function<String, String> verify) {
        this.flags.put(name, new ArgDef(name, help, verify));
        return this;
    }

    public ArgumentParser arg(String name, String help, Function<String, String> verify) {
        this.args.add(new ArgDef(name, help, verify));
        return this;
    }

    public String help() {
        StringBuilder builder = new StringBuilder();
        builder.append("Flags:").append(System.lineSeparator());
        for (ArgDef arg : flags.values()) {
            builder.append("    -").append(arg.getName()).append(": ").append(arg.getHelp()).append(System.lineSeparator());
        }
        builder.append("    -startScriptArgs: forces the remaining arguments to be used for the script, rather than the interpreter").append(System.lineSeparator());
        return builder.toString();
    }

    public Switch<Arguments, String> parse(String... args) {
        if(args.length < this.args.size()) {
            return Switch.ofB("Expected at least " + this.args.size() + " argument(s), but got " + args.length);
        }

        List<String> resArgs = new ArrayList<>();
        Map<String, String> resFlags = new LinkedHashMap<>();

        int i;
        for (i = 0; i < this.args.size(); i++) {
            ArgDef next = this.args.get(i);
            String ver = next.getVerify().apply(args[i]);
            if(ver == null) {
                resArgs.add(args[i]);
            } else {
                return Switch.ofB("Unexpected argument for " + next.getName() + ": " + ver);
            }
        }

        for (; i < args.length; i++) {
            String next = args[i];
            if(!next.startsWith("-")) {
                resArgs.add(next);
            } else {
                next = next.replaceFirst("-", "");
                String[] pieces = next.split("=", 2);
                String name = pieces[0];
                String value = pieces.length > 1 ? pieces[1] : "";

                if(name.equals("startScriptArgs")) {
                    i++;
                    break;
                }

                if(this.flags.containsKey(name) && !resFlags.containsKey(name)) {
                    String ver = this.flags.get(name).getVerify().apply(value);
                    if(ver == null) {
                        resFlags.put(name, value);
                    } else {
                        return Switch.ofB("Unexpected flag value -" + next + ": " + ver);
                    }
                } else {
                    break;
                }
            }
        }

        for (; i < args.length; i++) {
            resArgs.add(args[i]);
        }

        return Switch.ofA(new Arguments(resArgs, resFlags));
    }


    public static class ArgDef {
        private String name;
        private String help;
        private Function<String, String> verify;

        public ArgDef(String name, String help, Function<String, String> verify) {
            this.name = name;
            this.help = help;
            this.verify = verify;
        }

        public String getName() {
            return this.name;
        }

        public String getHelp() {
            return this.help;
        }

        public Function<String, String> getVerify() {
            return this.verify;
        }
    }

}
