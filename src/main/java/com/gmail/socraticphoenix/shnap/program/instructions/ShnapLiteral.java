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
package com.gmail.socraticphoenix.shnap.program.instructions;

import com.gmail.socraticphoenix.parse.Strings;
import com.gmail.socraticphoenix.shnap.program.AbstractShnapNode;
import com.gmail.socraticphoenix.shnap.program.ShnapInstruction;
import com.gmail.socraticphoenix.shnap.program.ShnapLoc;
import com.gmail.socraticphoenix.shnap.program.ShnapObject;
import com.gmail.socraticphoenix.shnap.program.context.ShnapContext;
import com.gmail.socraticphoenix.shnap.program.context.ShnapExecution;
import com.gmail.socraticphoenix.shnap.env.ShnapEnvironment;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapBooleanNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapCharNative;
import com.gmail.socraticphoenix.shnap.program.natives.num.ShnapNumberNative;
import com.gmail.socraticphoenix.shnap.program.natives.ShnapStringNative;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ShnapLiteral extends AbstractShnapNode implements ShnapInstruction {
    private ShnapObject value;

    public ShnapLiteral(ShnapLoc loc, ShnapObject value) {
        super(loc);
        this.value = value;
    }

    public ShnapObject getValue() {
        return this.value;
    }

    @Override
    public ShnapExecution exec(ShnapContext context, ShnapEnvironment tracer) {
        return ShnapExecution.normal(this.value, tracer, this.getLocation());
    }

    @Override
    public String decompile(int indent) {
        if (this.value == ShnapObject.getVoid()) {
            return "void";
        } else if (this.value == ShnapObject.getNull()) {
            return "null";
        } else if (this.value instanceof ShnapBooleanNative) {
            ShnapBooleanNative self = (ShnapBooleanNative) this.value;
            return self.getValue() ? "true" : "false";
        } else if (this.value instanceof ShnapCharNative) {
            int codepoint = ((ShnapCharNative) this.value).getNumber().intValue();
            char[] chars = Character.toChars(codepoint);
            if(chars.length == 1) {
                return "'" + Strings.escape(new String(chars)) + "'";
            } else {
                return "'\\u" + Integer.toString(codepoint, 16) + "'";
            }
        } else if (this.value instanceof ShnapNumberNative) {
            Number n = ((ShnapNumberNative) this.value).getNumber();
            if(n instanceof BigInteger) {
                return String.valueOf(n) + "i";
            } else if (n instanceof BigDecimal) {
                return String.valueOf(n) + "d";
            } else {
                return String.valueOf(n);
            }
        } else if (this.value instanceof ShnapStringNative) {
            return "\"" + Strings.escape(((ShnapStringNative) this.value).getValue()) + "\"";
        }
        return "void";
    }

}
