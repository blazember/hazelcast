/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.config;

import org.w3c.dom.Node;

import static com.hazelcast.config.DomConfigHelper.childElements;

public class YamlClientDomConfigProcessor extends ClientDomConfigProcessor {
    YamlClientDomConfigProcessor(boolean domLevel3, ClientConfig clientConfig) {
        super(domLevel3, clientConfig);
    }

    @Override
    protected void handleAttributeNode(Node n) {
        String attributeName = n.getNodeName();
        String value = n.getNodeValue();
        clientConfig.setAttribute(attributeName, value);
    }

    @Override
    protected void handleClusterMembers(Node node, ClientNetworkConfig clientNetworkConfig) {
        for (Node child : childElements(node)) {
            clientNetworkConfig.addAddress(getTextContent(child));
        }
    }

    @Override
    protected void handleOutboundPorts(Node child, ClientNetworkConfig clientNetworkConfig) {
        for (Node n : childElements(child)) {
            String value = getTextContent(n);
            clientNetworkConfig.addOutboundPortDefinition(value);
        }
    }

}
