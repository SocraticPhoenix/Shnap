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
package com.gmail.socraticphoenix.shnap.run.env;

import com.gmail.socraticphoenix.shnap.parse.ShnapLoc;

public class ShnapTraceback {
    private ShnapLoc loc;
    private String desc;
    private boolean detail;
    private boolean meta;

    public ShnapTraceback(ShnapLoc loc, String desc, boolean detail) {
        this.loc = loc;
        this.desc = desc;
        this.detail = detail;
    }

    public static ShnapTraceback detail(ShnapLoc loc, String desc) {
        return new ShnapTraceback(loc, desc, true);
    }

    public static ShnapTraceback meta(ShnapLoc loc, String desc) {
        ShnapTraceback det = new ShnapTraceback(loc, desc, true);
        det.meta = true;
        return det;
    }

    public static ShnapTraceback frame(ShnapLoc loc, String desc) {
        return new ShnapTraceback(loc, desc, false);
    }

    public boolean isMeta() {
        return meta;
    }

    public ShnapLoc getLoc() {
        return this.loc;
    }

    public String getDesc() {
        return this.desc;
    }

    public boolean isDetail() {
        return this.detail;
    }

}
