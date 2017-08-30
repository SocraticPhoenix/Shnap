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
import com.gmail.socraticphoenix.pio.ByteStream;
import com.gmail.socraticphoenix.pio.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Doc {
    private List<Switch<String, DocProperty>> content;
    private List<DocProperty> properties;

    public Doc(List<Switch<String, DocProperty>> content, List<DocProperty> properties) {
        this.content = content;
        this.properties = properties;
    }

    public List<Switch<String, DocProperty>> getContent() {
        return this.content;
    }

    public List<DocProperty> getProperties() {
        return this.properties;
    }

    public void write(ByteStream stream) throws IOException {
        stream.putInt(this.content.size());
        for(Switch<String, DocProperty> part : this.content) {
            if(part.containsA()) {
                stream.put((byte) 0);
                Bytes.writeString(stream, part.getA().get());
            } else {
                stream.put((byte) 1);
                part.getB().get().write(stream);
            }
        }

        stream.putInt(this.properties.size());
        for(DocProperty property : this.properties) {
            property.write(stream);
        }
    }

    public static Doc read(ByteStream stream) throws IOException {
        List<Switch<String, DocProperty>> content = new ArrayList<>();
        int len = stream.getInt();
        for (int i = 0; i < len; i++) {
            byte id = stream.get();
            if (id == 0) {
                content.add(Switch.ofA(Bytes.readString(stream)));
            } else {
                content.add(Switch.ofB(DocProperty.read(stream)));
            }
        }

        int propertySize = stream.getInt();
        List<DocProperty> properties = new ArrayList<>();
        for (int i = 0; i < propertySize; i++) {
            properties.add(DocProperty.read(stream));
        }
        return new Doc(content, properties);
    }

}
