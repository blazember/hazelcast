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

package com.hazelcast.map;

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastParallelParametersRunnerFactory;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.config.InMemoryFormat.BINARY;
import static com.hazelcast.config.InMemoryFormat.OBJECT;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@UseParametersRunnerFactory(HazelcastParallelParametersRunnerFactory.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class EntryProcessorScriptsTest extends HazelcastTestSupport {

    public static final String MAP_NAME = "EntryProcessorTest";

    @Parameter
    public InMemoryFormat inMemoryFormat;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {BINARY},
                {OBJECT},
                });
    }

    @Override
    public Config getConfig() {
        Config config = super.getConfig();
        MapConfig mapConfig = new MapConfig(MAP_NAME);
        mapConfig.setInMemoryFormat(inMemoryFormat);
        config.addMapConfig(mapConfig);
        return config;
    }

    @Test
    public void testExecuteOnKey() {
        HazelcastInstance instance = createHazelcastInstance(getConfig());
        IMap<String, String> map = instance.getMap(MAP_NAME);
        map.put("key", "value");

        int result = map.executeOnKey("key",
                "var key = entry.getKey();"
                        + "var value = entry.getValue();" +
                        "value = value + \" for project-x\";" +
                        "entry.setValue(value);" +
                        "42");

        String newValue = map.get("key");

        assertEquals(42, result);
        assertEquals("value for project-x", newValue);
    }

    @Test
    public void testExecuteOnEntries() {
        HazelcastInstance instance = createHazelcastInstance(getConfig());
        IMap<String, String> map = instance.getMap(MAP_NAME);
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, "value" + i);
        }

        Map<String, Integer> result = map.executeOnEntries(
                "var key = entry.getKey();"
                        + "var value = entry.getValue();"
                        + "if (value.substring(5) % 2 == 0) {"
                        + "  value = value + \" for project-x\";"
                        + "}"
                        + "entry.setValue(value);"
                        + "42");

        assertEquals("value0 for project-x", map.get("key0"));
        assertEquals("value1", map.get("key1"));
        assertEquals("value2 for project-x", map.get("key2"));
        assertEquals("value3", map.get("key3"));
        assertEquals("value4 for project-x", map.get("key4"));
        assertEquals("value5", map.get("key5"));
        assertEquals("value6 for project-x", map.get("key6"));
        assertEquals("value7", map.get("key7"));
        assertEquals("value8 for project-x", map.get("key8"));
        assertEquals("value9", map.get("key9"));

    }
}
