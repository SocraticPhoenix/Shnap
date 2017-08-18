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
package com.gmail.socraticphoenix.shnap.program.natives.num;

import com.gmail.socraticphoenix.shnap.program.ShnapFactory;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.natives.ShnapStringNative;

import java.math.BigInteger;

import static com.gmail.socraticphoenix.shnap.program.ShnapFactory.instSimple;

public class ShnapBooleanNative extends ShnapObject implements ShnapNumberNative {
    public static final ShnapBooleanNative TRUE = new ShnapBooleanNative(ShnapLoc.BUILTIN, true);
    public static final ShnapBooleanNative FALSE = new ShnapBooleanNative(ShnapLoc.BUILTIN, false);

    private boolean value;

    private ShnapBooleanNative(ShnapLoc loc, boolean value) {
        super(loc);
        this.value = value;
        ShnapNumberNative.implementFunctions(this, this);
        this.set(ShnapObject.AS_STRING, ShnapFactory.noArg(instSimple(() -> new ShnapStringNative(ShnapLoc.BUILTIN, String.valueOf(this.value)))));
    }

    @Override
    public Number getNumber() {
        return this.value ? BigInteger.ONE : BigInteger.ZERO;
    }

    @Override
    public ShnapObject copyWith(Number n) {
        if(n.doubleValue() == 0) {
            return FALSE;
        } else if (n.doubleValue() == 1) {
            return TRUE;
        }

        return ShnapNumberNative.valueOf(n);
    }

    public boolean getValue() {
        return this.value;
    }

    public static ShnapBooleanNative of(boolean b) {
        return b ? ShnapBooleanNative.TRUE : ShnapBooleanNative.FALSE;
    }

    public static ShnapBooleanNative of(ShnapLoc loc, boolean b) {
        return new ShnapBooleanNative(loc, b);
    }

}
