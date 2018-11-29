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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigNode implements Node {

    private Node parentNode;
    private short nodeType = Node.ELEMENT_NODE;
    private String nodeName;
    private String nodeValue;
    private String textContent;
    private List<Node> children;
    private Set<Attr> attributes;

    public ConfigNode(Node parentNode, String nodeName) {
        this.parentNode = parentNode;
        this.nodeName = nodeName;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public String getNodeValue() throws DOMException {
        return nodeValue;
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        this.nodeValue = nodeValue;
    }

    @Override
    public short getNodeType() {
        return nodeType;
    }

    @Override
    public Node getParentNode() {
        return parentNode;
    }

    @Override
    public NodeList getChildNodes() {
        return ConfigNodeList.of(children);
    }

    public void addChild(Node child) {
        getOrCreateChildren().add(child);
    }

    @Override
    public Node getFirstChild() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getLastChild() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getPreviousSibling() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getNextSibling() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedNodeMap getAttributes() {
        return null;
        //        return getOr;
    }

    @Override
    public Document getOwnerDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChildNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node cloneNode(boolean deep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void normalize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupported(String feature, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespaceURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrefix() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalName() {
        return getNodeName();
    }

    @Override
    public boolean hasAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBaseURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTextContent() throws DOMException {
        return textContent;
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        this.textContent = textContent;
    }

    @Override
    public boolean isSameNode(Node other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lookupNamespaceURI(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqualNode(Node arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getFeature(String feature, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getUserData(String key) {
        throw new UnsupportedOperationException();
    }

    private List<Node> getOrCreateChildren() {
        if (children == null) {
            children = new ArrayList<Node>();
        }

        return children;
    }

    private Set<Attr> getOrCreateAttributes() {
        if (attributes == null) {
            attributes = new HashSet<Attr>();
        }

        return attributes;
    }

    @Override
    public String toString() {
        return "ConfigNode{" +
                "nodeName='" + nodeName + '\'' +
                ", textContent='" + textContent + '\'' +
                '}';
    }
}
