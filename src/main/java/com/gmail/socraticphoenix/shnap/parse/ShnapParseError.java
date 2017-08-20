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
package com.gmail.socraticphoenix.shnap.parse;

import com.gmail.socraticphoenix.parse.Strings;

import java.io.IOException;

public class ShnapParseError extends Error {
    private ShnapLoc loc;

    public ShnapParseError(ShnapLoc loc, String message) {
        super(message);
        this.loc = loc;
    }

    public ShnapLoc getLoc() {
        return this.loc;
    }

    public String formatSafely() {
        try {
            return format();
        } catch (IOException e) {
            StringBuilder res = new StringBuilder();
            int line = this.loc.getLine();
            int col = this.loc.getCol();
            res.append("(line: ").append(line + 1).append(", col: ").append(col + 1).append(", script: ").append(loc.getScript().getFileName()).append("): ").append(this.getMessage()).append(System.lineSeparator());
            res.append("<unknown source>").append(System.lineSeparator()).append("^").append(System.lineSeparator());
            return res.toString();
        }
    }

    public String format() throws IOException {
        return format(this.loc, this.getMessage(), 3, true, true, true, 0);
    }

    public static String format(ShnapLoc loc, String message, int lineCount, boolean header, boolean file, boolean post, int indent) throws IOException {
        String ind = Strings.indent(indent);
        int line = loc.getLine();
        int col = loc.getCol();

        StringBuilder res = new StringBuilder();
        if (header) {
            res.append(ind).append("(line: ").append(line + 1).append(", col: ").append(col + 1).append(", script: ").append(file ? loc.getScript().getFileName() : loc.getScript().getName()).append("): ").append(message);
            res.append(System.lineSeparator());
        }
        String[] lines = loc.getScript().getContent().split("\n");

        for (int i = 0; i < lines.length; i++) {
            if (Math.abs(i - line) < lineCount) {
                res.append(ind).append(lines[i]).append(System.lineSeparator());
                if (line == i) {
                    res.append(ind);
                    for (int j = 0; j < col; j++) {
                        res.append(" ");
                    }
                    res.append("^");
                    res.append(System.lineSeparator());
                    if (!post) {
                        break;
                    }
                }
            }
        }

        return res.toString();
    }

}
