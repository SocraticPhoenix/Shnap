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
package com.gmail.socraticphoenix.shnap.program.context;

import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.run.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.type.object.ShnapResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShnapContext {
    private ShnapContext parent;
    private Map<String, ShnapObject> variables;
    private Map<String, List<Flag>> flags;

    public ShnapContext() {
        this.variables = new LinkedHashMap<>();
        this.flags = new LinkedHashMap<>();
    }

    public ShnapContext(ShnapContext parent) {
        this();
        this.parent = parent;
    }

    public void del(String name) {
        if (name.startsWith("^")) {
            this.getParentSafely().del(Strings.cutFirst(name));
        } else if (name.startsWith(":")) {
            name = Strings.cutFirst(name);
            this.variables.remove(name);
            this.flags.remove(name);
        } else {
            if (this.contains(name)) {
                this.variables.remove(name);
                this.flags.remove(name);
            } else if (this.parent != null) {
                this.parent.del(name);
                this.variables.remove(name);
                this.flags.remove(name);
            }
        }
    }

    public boolean hasFlag(String name, Flag flag) {
        List<Flag> flags = this.getFlags(name);
        return flags != null && flags.contains(flag);
    }

    public void setLocally(String name, ShnapObject object) {
        this.variables.remove(name);
        this.flags.remove(name);
        if (name.startsWith("^")) {
            this.getParentSafely().setLocally(Strings.cutFirst(name), object);
            return;
        }

        this.variables.put(name, object);
    }

    public ShnapObject getExactly(String name) {
        if (name.startsWith("^")) {
            if (this.parent == null) {
                return ShnapObject.getVoid();
            } else {
                return this.parent.getExactly(Strings.cutFirst(name));
            }
        }

        ShnapObject obj = this.variables.get(name);
        if (obj == null) {
            if (this.parent != null) {
                return this.parent.getExactly(name);
            }
            return ShnapObject.getVoid();
        } else {
            return obj;
        }
    }

    public void set(String name, ShnapObject object) {
        this.del(name); //Remove previous value (takes care of flags)

        if (name.startsWith("^")) {
            this.getParentSafely().set(Strings.cutFirst(name), object);
            return;
        } else if (name.startsWith(":")) {
            this.variables.put(Strings.cutFirst(name), object);
            return;
        }

        if (this.parent != null && this.parent.contains(name)) {
            this.parent.set(name, object);
        } else {
            this.variables.put(name, object);
        }
    }

    public void setFlag(String name, Flag flag) {
        if (name.startsWith("^")) {
            this.getParentSafely().setFlag(Strings.cutFirst(name), flag);
            return;
        } else if (name.startsWith(":")) {
            name = Strings.cutFirst(name);
            if (this.variables.containsKey(name)) {
                this.flags.computeIfAbsent(name, k -> new ArrayList<>()).add(flag);
            }
            return;
        }

        if (this.parent != null && this.parent.contains(name)) {
            this.parent.setFlag(name, flag);
        } else if (this.variables.containsKey(name)) {
            this.flags.computeIfAbsent(name, k -> new ArrayList<>()).add(flag);

        }
    }

    public ShnapExecution get(String name, ShnapEnvironment environment) {
        if (name.startsWith("^")) {
            if (this.parent == null) {
                return ShnapExecution.normal(ShnapObject.getVoid(), environment, ShnapLoc.BUILTIN);
            } else {
                return this.parent.get(Strings.cutFirst(name), environment);
            }
        } else if (name.startsWith(":")) {
            name = Strings.cutFirst(":");
            ShnapObject obj = this.variables.get(name);
            return obj == null ? ShnapExecution.normal(ShnapObject.getVoid(), environment, ShnapLoc.BUILTIN) : ShnapExecution.normal(obj, environment, ShnapLoc.BUILTIN);
        }

        ShnapObject obj = this.variables.get(name);
        if (obj == null) {
            if (this.parent != null) {
                return this.parent.get(name, environment);
            }
            return ShnapExecution.normal(ShnapObject.getVoid(), environment, ShnapLoc.BUILTIN);
        } else if (obj instanceof ShnapResolver) {
            return obj.resolve(environment);
        } else {
            return ShnapExecution.normal(obj, environment, ShnapLoc.BUILTIN);
        }
    }

    private List<Flag> getFlags(String name) {
        if (name.startsWith("^")) {
            if (this.parent == null) {
                return null;
            } else {
                return this.parent.getFlags(Strings.cutFirst(name));
            }
        } else if (name.startsWith(":")) {
            return this.flags.get(Strings.cutFirst(name));
        }

        List<Flag> flag = this.flags.get(name);
        if (flag == null) {
            if (this.parent != null) {
                return this.parent.getFlags(name);
            }
            return null;
        } else {
            return flag;
        }
    }

    public ShnapContext getParentSafely() {
        if (this.parent == null) {
            this.parent = new ShnapContext();
        }

        return this.parent;
    }

    public boolean directlyContains(String name) {
        return this.variables.containsKey(name);
    }

    public boolean contains(String name) {
        return this.variables.containsKey(name) || (this.parent != null && this.parent.contains(name));
    }

    public Collection<String> names() {
        return this.variables.keySet();
    }

    public static ShnapContext childOf(ShnapContext other) {
        return new ShnapContext(other);
    }

    public ShnapContext copy() {
        ShnapContext other = new ShnapContext(this.parent);
        for (String name : this.names()) {
            other.set(name, this.variables.get(name));
        }
        return other;
    }

    public boolean isChildOf(ShnapContext targetContext) {
        return targetContext == this || this.parent != null && this.parent.isChildOf(targetContext);
    }

    public enum Flag {
        DONT_IMPORT("noimport"),
        PRIVATE("private"),
        FINALIZED("final");

        String rep;

        Flag(String rep) {
            this.rep = rep;
        }

        public String getRep() {
            return rep;
        }

        public static Flag getByRep(String rep) {
            for (Flag flag : values()) {
                if (flag.rep.equals(rep)) {
                    return flag;
                }
            }

            return null;
        }

    }

}
