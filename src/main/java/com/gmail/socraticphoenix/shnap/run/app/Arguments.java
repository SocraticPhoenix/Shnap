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

import java.util.List;
import java.util.Map;

public class Arguments {
    private List<String> arguments;
    private Map<String, String> flags;

    public Arguments(List<String> arguments, Map<String, String> flags) {
        this.arguments = arguments;
        this.flags = flags;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public boolean hasFlag(String flag) {
        return this.flags.containsKey(flag);
    }

    public boolean hasArg(int i) {
        return i >= 0 && i < arguments.size();
    }

    public String getFlag(String flag) {
        return this.flags.get(flag);
    }

    public String getFlag(String flag, String def) {
        return this.flags.getOrDefault(flag, def);
    }

    public String getArg(int i) {
        return this.arguments.get(i);
    }

}
