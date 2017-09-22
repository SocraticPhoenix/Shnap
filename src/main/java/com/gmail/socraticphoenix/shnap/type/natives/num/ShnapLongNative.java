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
import com.gmail.socraticphoenix.shnap.type.object.ShnapObject;

public class ShnapLongNative extends ShnapObject implements ShnapNumberNative {
    private Long aLong;

    public ShnapLongNative(ShnapLoc loc, Long aLong) {
        super(loc);
        this.aLong = aLong;
        ShnapNumberNative.implementFunctions(this, this);
    }

    @Override
    public Number getNumber() {
        return this.aLong;
    }

    @Override
    public ShnapObject copyWith(Number n) {
        return new ShnapLongNative(this.loc, n.longValue());
    }

    @Override
    public int castingPrecedence(Number result) {
        return 20;
    }

    @Override
    public Object getJavaBacker() {
        return this.aLong;
    }

    @Override
    public ShnapNativeTypeDescriptor descriptor() {
        return ShnapNativeTypeRegistry.Descriptor.LONG;
    }

}
