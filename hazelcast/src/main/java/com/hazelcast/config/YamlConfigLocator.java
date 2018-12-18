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

import com.hazelcast.core.HazelcastException;

/**
 * Support class for the {@link com.hazelcast.config.YamlConfigBuilder} that locates the YAML configuration:
 * <ol>
 * <li>system property</li>
 * <li>working directory</li>
 * <li>classpath</li>
 * <li>default</li>
 * </ol>
 */
public class YamlConfigLocator extends AbstractConfigLocator {

    /**
     * Constructs a YamlConfigLocator that tries to find a usable YAML configuration file.
     *
     * @throws HazelcastException if there was a problem locating the config-file
     */
    public YamlConfigLocator() {
        try {
            if (loadFromSystemProperty("hazelcast.config")) {
                return;
            }

            if (loadFromWorkingDirectory("hazelcast.yaml")) {
                return;
            }

            if (loadConfigurationFromClasspath("hazelcast.yaml")) {
                return;
            }

            loadDefaultConfigurationFromClasspath("hazelcast-default.yaml");
        } catch (RuntimeException e) {
            throw new HazelcastException(e);
        }
    }
}
