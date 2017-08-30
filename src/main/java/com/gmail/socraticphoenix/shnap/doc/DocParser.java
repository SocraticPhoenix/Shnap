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

package com.gmail.socraticphoenix.shnap.doc;

import com.gmail.socraticphoenix.collect.coupling.Switch;
import com.gmail.socraticphoenix.parse.CharacterStream;
import com.gmail.socraticphoenix.parse.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocParser {

    public static Doc parse(String str) {
        while (str.startsWith("\n")) {
            str = Strings.cutFirst(str);
        }

        while (str.endsWith("\n")) {
            str = Strings.cutLast(str);
        }

        String[] lineArr = str.split("\n");
        List<String> lines = Stream.of(lineArr).map(line -> {
            line = line.trim();
            if (line.startsWith("*")) {
                line = Strings.cutFirst(line);
            }
            return line.trim();
        }).collect(Collectors.toList());

        StringBuilder content = new StringBuilder();
        List<DocProperty> properties = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("@") && line.contains(":")) {
                String prop = Strings.cutFirst(line).trim();
                String[] pieces = prop.split(" ", 2);
                if (pieces.length == 1 || !pieces[1].contains(":")) {
                    pieces = prop.split(":", 2);
                    String type = pieces[0].trim();
                    String value = pieces[1].trim();
                    properties.add(new DocProperty(type, null, value));
                } else {
                    String type = pieces[0].trim();
                    pieces = pieces[1].split(":", 2);
                    String name = pieces[0].trim();
                    String value = pieces[1].trim();
                    properties.add(new DocProperty(type, name, value));
                }
            } else if (line.isEmpty()) {
                content.append("<lb />");
            } else {
                content.append(line).append(line.endsWith(" ") ? "" : " ");
            }
        }

        List<Switch<String, DocProperty>> docContent = new ArrayList<>();
        CharacterStream stream = new CharacterStream(content.toString());
        while (stream.hasNext()) {
            String next = stream.nextUntil("{@");
            if (stream.isNext("{@")) {
                stream.next(2);
                int index = stream.index();
                String prop = stream.nextUntil("}").trim();
                if (stream.isNext("}") && prop.startsWith("@") && prop.contains(":")) {
                    stream.next();
                    String[] pieces = prop.split(" ", 2);
                    if (pieces.length == 1 || !pieces[1].contains(":")) {
                        pieces = prop.split(":", 2);
                        String type = pieces[0].trim();
                        String value = pieces[1].trim();
                        docContent.add(Switch.ofB(new DocProperty(type, null, value)));
                    } else {
                        String type = pieces[0].trim();
                        pieces = pieces[1].split(":", 2);
                        String name = pieces[0].trim();
                        String value = pieces[1].trim();
                        docContent.add(Switch.ofB(new DocProperty(type, name, value)));
                    }
                } else {
                    stream.jumpTo(index);
                    docContent.add(Switch.ofA(next + "{@"));
                }
            } else {
                docContent.add(Switch.ofA(next));
            }
        }

        return new Doc(docContent, properties);
    }


}
