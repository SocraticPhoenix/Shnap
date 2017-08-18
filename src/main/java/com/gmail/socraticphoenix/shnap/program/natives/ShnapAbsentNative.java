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
package com.gmail.socraticphoenix.shnap.program.natives;

import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;

import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.*;

public class ShnapAbsentNative extends ShnapObject {
    public static final ShnapObject NULL = new ShnapAbsentNative(ShnapLoc.BUILTIN, "null");
    public static final ShnapObject VOID = new ShnapAbsentNative(ShnapLoc.BUILTIN, "void");

    private String name;

    private ShnapAbsentNative(ShnapLoc loc, String name) {
        super(loc);
        this.name = name;
        this.set(ShnapObject.AS_STRING, noArg(instSimple(() -> new ShnapStringNative(ShnapLoc.BUILTIN, this.name))));
        this.set(ShnapObject.AS_NUMBER, noArg(instSimple(() -> ShnapNumberNative.valueOf(0))));
        this.set(ShnapObject.AS_BOOLEAN, noArg(instSimple(() -> ShnapBooleanNative.of(false))));
        this.set(ShnapObject.AS_ARRAY, noArg(instSimple(() -> new ShnapArrayNative(ShnapLoc.BUILTIN, this))));
        this.set("equals", oneArg(inst((ctx, trc) -> {
            ShnapObject object = ctx.get("arg");
            if(object instanceof ShnapAbsentNative && ((ShnapAbsentNative) object).getName().equals(this.name)) {
                return ShnapExecution.normal(ShnapBooleanNative.TRUE, trc, ShnapLoc.BUILTIN);
            } else {
                return ShnapExecution.normal(ShnapBooleanNative.FALSE, trc, ShnapLoc.BUILTIN);
            }
        })));
    }

    public String getName() {
        return this.name;
    }

}
