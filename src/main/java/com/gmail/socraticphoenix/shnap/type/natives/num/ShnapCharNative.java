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
package com.gmail.socraticphoenix.shnap.type.natives.num;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapNativeTypeDescriptor;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapNativeTypeRegistry;
import com.gmail.socraticphoenix.shnap.type.natives.ShnapStringNative;
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;
import com.gmail.socraticphoenix.shnap.util.ShnapFactory;

import java.math.BigInteger;

import static com.gmail.socraticphoenix.shnap.util.ShnapFactory.instSimple;

public class ShnapCharNative extends ShnapObject implements ShnapNumberNative {
    private Integer value;

    public ShnapCharNative(ShnapLoc loc, Integer value) {
        super(loc);
        this.value = value;
        ShnapNumberNative.implementFunctions(this, this);
        this.set(ShnapObject.AS_STRING, ShnapFactory.noArg(instSimple(() -> new ShnapStringNative(this.getLocation(), new String(new int[]{this.value}, 0, 1)))));
    }

    @Override
    public Integer getNumber() {
        return this.value;
    }

    @Override
    public ShnapObject copyWith(Number n) {
        return new ShnapCharNative(this.getLocation(), n.intValue());
    }

    @Override
    public int castingPrecedence(Number result) {
        if(result instanceof BigInteger) {
            BigInteger i = (BigInteger) result;
            try {
                i.intValueExact();
                return 100;
            } catch (ArithmeticException ignore) {

            }
        }

        if (result instanceof Integer) {
            return 100;
        }


        return 0;
    }

    @Override
    public Object getJavaBacker() {
        return this.value;
    }

    @Override
    public ShnapNativeTypeDescriptor descriptor() {
        return ShnapNativeTypeRegistry.Descriptor.CHAR;
    }

}
