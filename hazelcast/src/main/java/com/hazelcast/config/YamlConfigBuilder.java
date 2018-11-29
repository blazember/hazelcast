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

package com.hazelcast.config;

import com.hazelcast.config.dom.YamlDomBuilder;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.util.ExceptionUtil;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.hazelcast.config.ConfigSections.INSTANCE_NAME;
import static com.hazelcast.util.Preconditions.checkNotNull;

/**
 * A YAML {@link ConfigBuilder} implementation.
 *
 * This config builder is compatible with the YAML 1.2 specification and
 * supports the JSON Scheme.
 */
public class YamlConfigBuilder implements ConfigBuilder {

    private static final ILogger LOGGER = Logger.getLogger(YamlConfigBuilder.class);

    private final Set<String> occurrenceSet = new HashSet<String>();
    private final InputStream in;

    private Properties properties = System.getProperties();
    private File configurationFile;
    private URL configurationUrl;
    private Config config;

    //    static {
    //        try {
    //            Config.class.forName("org.snakeyaml.engine.v1.api.Load");
    //        } catch (ClassNotFoundException e) {
    //            LOGGER.severe("The SnakeYAML engine library couldn't be found on the classpath");
    //        }
    //    }
    //

    /**
     * Constructs a YamlConfigBuilder that reads from the provided YAML file.
     *
     * @param YAMLFileName the name of the YAML file that the YamlConfigBuilder reads from
     * @throws FileNotFoundException if the file can't be found
     */
    public YamlConfigBuilder(String YAMLFileName) throws FileNotFoundException {
        this(new FileInputStream(YAMLFileName));
        this.configurationFile = new File(YAMLFileName);
    }

    /**
     * Constructs a YAMLConfigBuilder that reads from the given InputStream.
     *
     * @param inputStream the InputStream containing the YAML configuration
     * @throws IllegalArgumentException if inputStream is {@code null}
     */
    public YamlConfigBuilder(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream can't be null");
        }
        this.in = inputStream;
    }

    /**
     * Constructs a YamlConfigBuilder that reads from the given URL.
     *
     * @param url the given url that the YamlConfigBuilder reads from
     * @throws IOException if URL is invalid
     */
    public YamlConfigBuilder(URL url) throws IOException {
        checkNotNull(url, "URL is null!");
        this.in = url.openStream();
        this.configurationUrl = url;
    }

    /**
     * Constructs a YamlConfigBuilder that tries to find a usable YAML configuration file.
     */
    public YamlConfigBuilder() {
        YamlConfigLocator locator = new YamlConfigLocator();
        this.in = locator.getIn();
        this.configurationFile = locator.getConfigurationFile();
        this.configurationUrl = locator.getConfigurationUrl();
    }

    @Override
    public Config build() {
        return build(new Config());
    }

    Config build(Config config) {
        config.setConfigurationFile(configurationFile);
        config.setConfigurationUrl(configurationUrl);
        try {
            parseAndBuildConfig(config);
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
        return config;
    }

    private void parseAndBuildConfig(Config config) {
        this.config = config;
        LoadSettings loadSettings = new LoadSettingsBuilder().build();
        Load load = new Load(loadSettings);
        Object document = load.loadFromInputStream(in);

        Node root = new YamlDomBuilder().buildDom(document, ConfigSections.HAZELCAST.name().toLowerCase());
        //        Map<String, Object> imdgRoot = getImdgRoot(root);

        //

        DomConfigBuilder configBuilder = new DomConfigBuilder(config);
        try {
            configBuilder.handleConfig(root);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(root);
        //        check
        //
        //        process(root);
        //        if (shouldValidateTheSchema()) {
        //            schemaValidation(root.getOwnerDocument());
        //        }
        //

        //        handleConfig(imdgRoot);
    }

    private Map<String, Object> getImdgRoot(Object root) {
        if (root instanceof Map) {
            Object imdgRoot = ((Map) root).get(ConfigSections.HAZELCAST.name().toLowerCase());
            if (imdgRoot == null) {
                throw new InvalidConfigurationException("No mapping with hazelcast key is found in the provided configuration");
            }

            if (imdgRoot instanceof Map) {
                return (Map<String, Object>) imdgRoot;
            }
        }

        throw new InvalidConfigurationException("The provided configuration couldn't be parsed: no ");
    }

    private void handleConfig(Map<String, Object> imdgRoot) {
        for (Map.Entry<String, Object> entry : imdgRoot.entrySet()) {
            String nodeName = entry.getKey();
            Object node = entry.getKey();

            if (INSTANCE_NAME.isEqual(nodeName)) {
                handleInstanceName(node);
            } else if (ConfigSections.NETWORK.isEqual(nodeName)) {
                handleNetwork(node);
            }
        }
    }

    private void handleInstanceName(Object node) {
        String instanceName = getTextContent(node);
        if (instanceName.isEmpty()) {
            throw new InvalidConfigurationException("Instance name in XML configuration is empty");
        }
        config.setInstanceName(instanceName);
    }

    private void handleNetwork(Object node) {
    }

    private String getTextContent(Object node) {
        if (node == null) {
            return null;
        }

        if (node instanceof String) {
            return (String) node;
        }

        return node.toString();
    }
}
