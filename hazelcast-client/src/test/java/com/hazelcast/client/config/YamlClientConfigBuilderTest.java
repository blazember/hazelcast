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

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.config.SocketInterceptorConfig;
import com.hazelcast.config.YamlConfigBuilderTest;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import static com.hazelcast.nio.IOUtil.delete;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the usage of {@link YamlClientConfigBuilder}
 */
// tests need to be executed sequentially because of system properties being set/unset
@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class YamlClientConfigBuilderTest extends HazelcastTestSupport {

    private ClientConfig fullClientConfig;
    private ClientConfig defaultClientConfig;

    @Before
    public void init() throws Exception {
        URL schemaResource = YamlConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-full.yaml");
        fullClientConfig = new YamlClientConfigBuilder(schemaResource).build();

        //        URL schemaResourceDefault = YamlConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-default.yaml");
        //        defaultClientConfig = new YamlClientConfigBuilder(schemaResourceDefault).build();
    }

    @After
    @Before
    public void beforeAndAfter() {
        System.clearProperty("hazelcast.client.config");
    }

    @Test(expected = InvalidConfigurationException.class)
    public void testInvalidRootElement() {
        String yaml = ""
                + "hazelcast:\n"
                + "  group:\n"
                + "    name: dev\n"
                + "    password: clusterpass";
        buildConfig(yaml);
    }

    @Test(expected = HazelcastException.class)
    public void loadingThroughSystemProperty_nonExistingFile() throws IOException {
        File file = File.createTempFile("foo", "bar");
        delete(file);
        System.setProperty("hazelcast.client.config", file.getAbsolutePath());

        new YamlClientConfigBuilder();
    }

    @Test
    public void loadingThroughSystemProperty_existingFile() throws IOException {
        String yaml = ""
                + "hazelcast-client:\n"
                + "  group:\n"
                + "    name: foobar\n"
                + "    password: dev-pass";

        File file = File.createTempFile("foo", "bar");
        file.deleteOnExit();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println(yaml);
        writer.close();

        System.setProperty("hazelcast.client.config", file.getAbsolutePath());

        YamlClientConfigBuilder configBuilder = new YamlClientConfigBuilder();
        ClientConfig config = configBuilder.build();
        assertEquals("foobar", config.getGroupConfig().getName());
    }

    @Test(expected = HazelcastException.class)
    public void loadingThroughSystemProperty_nonExistingClasspathResource() throws IOException {
        System.setProperty("hazelcast.client.config", "classpath:idontexist");
        new YamlClientConfigBuilder();
    }

    @Test
    public void loadingThroughSystemProperty_existingClasspathResource() throws IOException {
        System.setProperty("hazelcast.client.config", "classpath:test-hazelcast-client.yaml");

        YamlClientConfigBuilder configBuilder = new YamlClientConfigBuilder();
        ClientConfig config = configBuilder.build();
        assertEquals("foobar", config.getGroupConfig().getName());
        assertEquals("com.hazelcast.nio.ssl.BasicSSLContextFactory",
                config.getNetworkConfig().getSSLConfig().getFactoryClassName());
        assertEquals(128, config.getNetworkConfig().getSocketOptions().getBufferSize());
        assertFalse(config.getNetworkConfig().getSocketOptions().isKeepAlive());
        assertFalse(config.getNetworkConfig().getSocketOptions().isTcpNoDelay());
        assertEquals(3, config.getNetworkConfig().getSocketOptions().getLingerSeconds());
    }

    @Test
    public void testGroupConfig() {
        final GroupConfig groupConfig = fullClientConfig.getGroupConfig();
        assertEquals("dev", groupConfig.getName());
        assertEquals("dev-pass", groupConfig.getPassword());
    }

    @Test
    public void testProperties() {
        assertEquals(6, fullClientConfig.getProperties().size());
        assertEquals("60000", fullClientConfig.getProperty("hazelcast.client.heartbeat.timeout"));
    }

    @Test
    public void testAttributes() {
        Map<String, String> attributes = fullClientConfig.getAttributes();
        assertEquals(2, attributes.size());
        assertEquals("bar", attributes.get("foo"));
        assertEquals("admin", attributes.get("role"));
    }

    @Test
    public void testInstanceName() {
        assertEquals("CLIENT_NAME", fullClientConfig.getInstanceName());
    }

    @Test
    public void testNetworkConfig() {
        final ClientNetworkConfig networkConfig = fullClientConfig.getNetworkConfig();
        assertEquals(2, networkConfig.getConnectionAttemptLimit());
        assertEquals(2, networkConfig.getAddresses().size());
        assertContains(networkConfig.getAddresses(), "127.0.0.1");
        assertContains(networkConfig.getAddresses(), "127.0.0.2");

        Collection<String> allowedPorts = networkConfig.getOutboundPortDefinitions();
        assertEquals(2, allowedPorts.size());
        assertTrue(allowedPorts.contains("34600"));
        assertTrue(allowedPorts.contains("34700-34710"));

        assertTrue(networkConfig.isSmartRouting());
        assertTrue(networkConfig.isRedoOperation());

        final SocketInterceptorConfig socketInterceptorConfig = networkConfig.getSocketInterceptorConfig();
        assertTrue(socketInterceptorConfig.isEnabled());
        assertEquals("com.hazelcast.examples.MySocketInterceptor", socketInterceptorConfig.getClassName());
        assertEquals("bar", socketInterceptorConfig.getProperty("foo"));

        AwsConfig awsConfig = networkConfig.getAwsConfig();
        assertTrue(awsConfig.isEnabled());
        assertEquals("TEST_ACCESS_KEY", awsConfig.getProperty("access-key"));
        assertEquals("TEST_SECRET_KEY", awsConfig.getProperty("secret-key"));
        assertEquals("us-east-1", awsConfig.getProperty("region"));
        assertEquals("ec2.amazonaws.com", awsConfig.getProperty("host-header"));
        assertEquals("type", awsConfig.getProperty("tag-key"));
        assertEquals("hz-nodes", awsConfig.getProperty("tag-value"));
        assertEquals("11", awsConfig.getProperty("connection-timeout-seconds"));
        assertFalse(networkConfig.getGcpConfig().isEnabled());
        assertFalse(networkConfig.getAzureConfig().isEnabled());
        assertFalse(networkConfig.getKubernetesConfig().isEnabled());
        assertFalse(networkConfig.getEurekaConfig().isEnabled());
    }


    static ClientConfig buildConfig(String yaml, Properties properties) {
        ByteArrayInputStream bis = new ByteArrayInputStream(yaml.getBytes());
        YamlClientConfigBuilder configBuilder = new YamlClientConfigBuilder(bis);
        //        configBuilder.setProperties(properties);
        return configBuilder.build();
    }

    static ClientConfig buildConfig(String yaml, String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        return buildConfig(yaml, properties);
    }

    public static ClientConfig buildConfig(String yaml) {
        return buildConfig(yaml, null);
    }

}
