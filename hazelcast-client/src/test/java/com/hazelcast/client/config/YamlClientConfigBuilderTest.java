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
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the usage of {@link YamlClientConfigBuilder}
 */
// tests need to be executed sequentially because of system properties being set/unset
@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class YamlClientConfigBuilderTest extends HazelcastTestSupport {

    private ClientConfig fullClientConfig;
    private ClientConfig defaultClientConfig;

    //
    //        @Before
    //        public void init() throws Exception {
    //            URL schemaResource = yamlConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-full.yaml");
    //            fullClientConfig = new yamlClientConfigBuilder(schemaResource).build();
    //
    //            URL schemaResourceDefault = yamlConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-default.yaml");
    //            defaultClientConfig = new yamlClientConfigBuilder(schemaResourceDefault).build();
    //        }
    //
    @After
    @Before
    public void beforeAndAfter() {
        System.clearProperty("hazelcast.client.config");
    }

    //    @Test(expected = InvalidConfigurationException.class)
    //    public void testInvalidRootElement() {
    //        String yaml = "<hazelcast>"
    //                + "<group>"
    //                + "<name>dev</name>"
    //                + "<password>clusterpass</password>"
    //                + "</group>"
    //                + "</hazelcast>";
    //        buildConfig(yaml);
    //    }
    //
    //    @Test(expected = HazelcastException.class)
    //    public void loadingThroughSystemProperty_nonExistingFile() throws IOException {
    //        File file = File.createTempFile("foo", "bar");
    //        delete(file);
    //        System.setProperty("hazelcast.client.config", file.getAbsolutePath());
    //
    //        new YamlClientConfigBuilder();
    //    }

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
