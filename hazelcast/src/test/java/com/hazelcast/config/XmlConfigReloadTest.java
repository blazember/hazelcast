/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.dynamicconfig.ConfigReloadResult;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

import static com.hazelcast.test.Accessors.getNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class XmlConfigReloadTest extends HazelcastTestSupport {
    static final String HAZELCAST_START_TAG = "<hazelcast xmlns=\"http://www.hazelcast.com/schema/config\">\n";
    static final String HAZELCAST_END_TAG = "</hazelcast>\n";

    @Test
    public void test() {
        String xml = HAZELCAST_START_TAG
                + "    <map name=\"foo\">\n"
                + "        <in-memory-format>BINARY</in-memory-format>"
                + "    </map>\n"
                + HAZELCAST_END_TAG;

        Config config = buildConfig(xml);
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        HazelcastInstance hz1 = factory.newHazelcastInstance(config);
        HazelcastInstance hz2 = factory.newHazelcastInstance(config);

        String xmlReload = HAZELCAST_START_TAG
                + "    <map name=\"foo\">\n"
                + "        <in-memory-format>BINARY</in-memory-format>"
                + "    </map>\n"
                + "    <map name=\"bar\">\n"
                + "        <in-memory-format>OBJECT</in-memory-format>"
                + "    </map>\n"
                + HAZELCAST_END_TAG;

        Config configReload = buildConfig(xmlReload);
        ConfigReloadResult reloadResult = getNode(hz1).reloadConfig(configReload);

        assertTrueEventually(()->{
            assertTrue(hz1.getConfig().getMapConfigs().containsKey("bar"));
            assertTrue(hz2.getConfig().getMapConfigs().containsKey("bar"));

            MapConfig hz1BarMapConfig = hz1.getConfig().getMapConfig("bar");
            assertEquals(InMemoryFormat.OBJECT, hz1BarMapConfig.getInMemoryFormat());

            MapConfig hz2BarMapConfig = hz2.getConfig().getMapConfig("bar");
            assertEquals(InMemoryFormat.OBJECT, hz2BarMapConfig.getInMemoryFormat());
        });
    }

    protected Config buildConfig(String xml) {
        ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes());
        XmlConfigBuilder configBuilder = new XmlConfigBuilder(bis);
        return configBuilder.build();
    }

}
