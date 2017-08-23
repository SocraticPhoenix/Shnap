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

import java.util.LinkedHashMap;
import java.util.Map;

public class StringNode {
    private String value;
    private Map<String, StringNode> children;

    public StringNode(String value, Map<String, StringNode> children) {
        this.value = value;
        this.children = children;
    }

    public StringNode(String value) {
        this(value, new LinkedHashMap<>());
    }

    public String getValue() {
        return this.value;
    }

    public Map<String, StringNode> getChildren() {
        return this.children;
    }

    public DocNode toDocNode() {
        DocNode node = new DocNode(this.value == null ? null : DocParser.parse(this.value));
        for(Map.Entry<String, StringNode> child : this.children.entrySet()) {
            node.getChildren().put(child.getKey(), child.getValue().toDocNode());
        }
        return node;
    }

}
