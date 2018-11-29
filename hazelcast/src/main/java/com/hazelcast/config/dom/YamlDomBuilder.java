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

import java.util.List;
import java.util.Map;

/**
 * Builds a DOM tree of the YAML nodes as loaded by the SnakeYaml
 * provided library.
 */
public class YamlDomBuilder {

    public Node buildDom(Object document, String rootName) {
        if (document == null) {
            throw new IllegalArgumentException("The provided document is null");
        }

        if (!(document instanceof Map)) {
            throw new IllegalArgumentException("The provided document is not compatible with the YAML->DOM builder");
        }

        Object rootNode = ((Map) document).get(rootName);

        if (rootNode == null) {
            throw new IllegalArgumentException("The required " + rootName + " root node couldn't be found in the document root");
        }

        return buildNode(null, rootName, rootNode);
    }

    private Node buildNode(Node parent, String nodeName, Object yamlNode) {
        ConfigNode node = new ConfigNode(parent, nodeName);

        if (yamlNode instanceof Map) {
            buildChildren(node, (Map<String, Object>) yamlNode);
        } else if (yamlNode instanceof List) {
            System.out.println("List: " + nodeName + " -> " + yamlNode.toString());
        } else {
            buildAttribute(node, yamlNode);
        }

        return node;
    }

    private void buildChildren(ConfigNode parentNode, Map<String, Object> mapNode) {
        for (Map.Entry<String, Object> entry : mapNode.entrySet()) {
            String childNodeName = entry.getKey();
            Object childNodeValue = entry.getValue();

            Node child = buildNode(parentNode, childNodeName, childNodeValue);
            parentNode.addChild(child);
        }
    }

    private void buildChildren(ConfigNode parentNode, List<Object> listNode) {
        for (Object child : listNode) {

            //            Node child = buildNode(parentNode, childNodeName, childNodeValue);
            //            parentNode.addChild(child);
        }
    }

    private void buildAttribute(ConfigNode parentNode, Object value) {
        parentNode.setTextContent(value.toString());
    }

}
