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
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        if (name.indexOf('.') == -1) {
            if (name.startsWith("^")) {
                this.getParentSafely().del(Strings.cutFirst(name));
            } else {
                this.variables.remove(name);
                this.flags.remove(name);
            }
        } else {
            this.pathDel(name.split(Pattern.quote(".")));
        }
    }

    public boolean hasFlag(String name, Flag flag) {
        List<Flag> flags = this.getFlags(name);
        return flags != null && flags.contains(flag);
    }

    public void set(String name, ShnapObject object) {
        this.del(name); //Remove previous value (takes care of flags)

        if (name.startsWith("^")) {
            this.getParentSafely().set(Strings.cutFirst(name), object);
            return;
        }

        if (name.indexOf('.') == -1) {
            if (this.parent != null && this.parent.contains(name)) {
                this.parent.set(name, object);
            } else {
                this.variables.put(name, object);
            }
        } else {
            this.pathSet(name.split(Pattern.quote(".")), object);
        }
    }

    public void setFlag(String name, Flag flag) {
        if(name.startsWith("^")) {
            this.getParentSafely().setFlag(Strings.cutFirst(name), flag);
            return;
        }

        if(name.indexOf('.') == -1) {
            if(this.parent != null && this.parent.contains(name)) {
                this.parent.setFlag(name, flag);
            } else {
                this.flags.computeIfAbsent(name, k -> new ArrayList<>()).add(flag);
            }
        } else {
            this.pathSetFlag(name.split(Pattern.quote(".")), flag);
        }
    }

    public ShnapObject get(String name) {
        if (name.startsWith("^")) {
            if(this.parent == null) {
                return ShnapObject.getVoid();
            } else {
                return this.parent.get(Strings.cutFirst(name));
            }
        }

        if (name.indexOf('.') == -1) {
            ShnapObject obj = this.variables.get(name);
            if (obj == null) {
                if (this.parent != null) {
                    return this.parent.get(name);
                }
                return ShnapObject.getVoid();
            } else {
                return obj;
            }
        } else {
            return this.pathGet(name.split(Pattern.quote(".")));
        }
    }

    public List<Flag> getFlags(String name) {
        if(name.startsWith("^")) {
            if(this.parent == null) {
                return null;
            } else {
                return this.parent.getFlags(Strings.cutFirst(name));
            }
        }

        if(name.indexOf('.') == -1) {
            List<Flag> flag = this.flags.get(name);
            if(flag == null) {
                if(this.parent != null) {
                    return this.parent.getFlags(name);
                }
                return null;
            } else {
                return flag;
            }
        } else {
            return this.pathGetFlags(name.split(Pattern.quote(".")));
        }
    }

    private void pathDel(String[] path) {
        ShnapContext context = this;
        for (int i = 0; i < path.length - 1; i++) {
            if (!context.contains(path[i])) {
                return;
            }

            ShnapObject obj = context.get(path[i]);
            context = obj.getContext();
        }

        context.del(path[path.length - 1]);
    }

    private void pathSet(String[] path, ShnapObject object) {
        ShnapContext context = this;
        for (int i = 0; i < path.length - 1; i++) {
            if (!context.contains(path[i])) {
                ShnapObject obj = new ShnapObject(ShnapLoc.BUILTIN);
                obj.init(context);
                context.set(path[i], object);
            }

            ShnapObject obj = context.get(path[i]);
            context = obj.getContext();
        }

        context.set(path[path.length - 1], object);
    }

    private void pathSetFlag(String[] path, Flag flag) {
        ShnapContext context = this;
        for (int i = 0; i < path.length - 1; i++) {
            if (!context.contains(path[i])) {
                return;
            }

            ShnapObject obj = context.get(path[i]);
            context = obj.getContext();
        }

        context.setFlag(path[path.length - 1], flag);
    }

    private ShnapObject pathGet(String[] path) {
        ShnapContext context = this;
        for (int i = 0; i < path.length - 1; i++) {
            if (!context.contains(path[i])) {
                return ShnapObject.getVoid();
            }

            ShnapObject obj = context.get(path[i]);
            context = obj.getContext();
        }

        return context.get(path[path.length - 1]);
    }

    private List<Flag> pathGetFlags(String[] path) {
        ShnapContext context = this;
        for (int i = 0; i < path.length - 1; i++) {
            if (!context.contains(path[i])) {
                return null;
            }

            ShnapObject obj = context.get(path[i]);
            context = obj.getContext();
        }

        return context.getFlags(path[path.length - 1]);
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
            other.set(name, this.get(name));
        }
        return other;
    }

    public enum Flag {
        DONT_IMPORT("dont_import"),
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
            for(Flag flag : values()) {
                if(flag.rep.equals(rep)) {
                    return flag;
                }
            }

            return null;
        }

    }

}
