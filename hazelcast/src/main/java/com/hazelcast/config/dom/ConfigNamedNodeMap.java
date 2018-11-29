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

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigNamedNodeMap implements NamedNodeMap {

    /**
     * Map for accessing
     */
    private final Map<String, Node> nodesMap = new LinkedHashMap<String, Node>();

    @Override
    public Node getNamedItem(String name) {
        return nodesMap.get(name);
    }

    @Override
    public Node setNamedItem(Node arg) throws DOMException {
        return nodesMap.put(arg.getNodeName(), arg);
    }

    @Override
    public Node removeNamedItem(String name) throws DOMException {
        return nodesMap.remove(name);
    }

    @Override
    public Node item(int index) {
        if (index < 0 || index >= nodesMap.size()) {
            return null;
        }

        // TODO avoid iteration
        Iterator<Node> iterator = nodesMap.values().iterator();
        Node node = null;
        for (int i = 0; i < index; i++) {
            node = iterator.next();
        }

        return node;
    }

    @Override
    public int getLength() {
        return nodesMap.size();
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node setNamedItemNS(Node arg) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
        throw new UnsupportedOperationException();
    }
}
