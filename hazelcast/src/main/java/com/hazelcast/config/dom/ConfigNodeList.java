/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.config.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class ConfigNodeList implements NodeList {
    private final List<Node> nodes;

    private ConfigNodeList(List<Node> nodes) {
        this.nodes = new ArrayList<Node>(nodes);
    }

    @Override
    public Node item(int index) {
        if (index < 0 || index >= nodes.size()) {
            return null;
        }

        return nodes.get(index);
    }

    @Override
    public int getLength() {
        return nodes.size();
    }

    static NodeList of(List<Node> nodes) {
        return new ConfigNodeList(nodes);
    }
}
