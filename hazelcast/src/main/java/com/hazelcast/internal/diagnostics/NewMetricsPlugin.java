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

package com.hazelcast.internal.diagnostics;

import com.hazelcast.internal.metrics.MetricsRegistry;
import com.hazelcast.internal.metrics.renderers.ProbeRenderer;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.properties.HazelcastProperties;

import static com.hazelcast.internal.diagnostics.MetricsPlugin.PERIOD_SECONDS;

public class NewMetricsPlugin extends DiagnosticsPlugin {

    private final ProbeRenderer probeRenderer = new SysOutRendererImpl();
    private final MetricsRegistry metricsRegistry;
    private final long periodMillis;

    public NewMetricsPlugin(NodeEngineImpl nodeEngine) {
        this(nodeEngine.getLogger(NewMetricsPlugin.class), nodeEngine.getMetricsRegistry(), nodeEngine.getProperties());
    }

    public NewMetricsPlugin(ILogger logger, MetricsRegistry metricsRegistry, HazelcastProperties properties) {
        super(logger);
        this.metricsRegistry = metricsRegistry;
        this.periodMillis = properties.getMillis(PERIOD_SECONDS);
    }

    @Override
    public long getPeriodMillis() {
        return periodMillis;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void run(DiagnosticsLogWriter writer) {
        metricsRegistry.render(probeRenderer);
    }

    private static class SysOutRendererImpl implements ProbeRenderer {

        @Override
        public void renderLong(String name, long value) {
            System.out.println("PROBE: " + name + "=" + value);
        }

        @Override
        public void renderDouble(String name, double value) {
            System.out.println("PROBE: " + name + "=" + value);
        }

        @Override
        public void renderException(String name, Exception e) {
            System.out.println("PROBE: " + name + "=" + e.getMessage());
        }

        @Override
        public void renderNoValue(String name) {
            System.out.println("PROBE: " + name + "=" + null);
        }
    }
}
