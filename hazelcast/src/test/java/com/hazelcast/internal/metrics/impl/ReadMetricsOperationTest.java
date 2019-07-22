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

package com.hazelcast.internal.metrics.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.diagnostics.Diagnostics;
import com.hazelcast.internal.metrics.ProbeLevel;
import com.hazelcast.internal.metrics.managementcenter.ReadMetricsOperation;
import com.hazelcast.internal.metrics.managementcenter.ConcurrentArrayRingbuffer.RingbufferSlice;
import com.hazelcast.internal.metrics.managementcenter.Metric;
import com.hazelcast.internal.metrics.managementcenter.MetricsResultSet;
import com.hazelcast.map.IMap;
import com.hazelcast.spi.InternalCompletableFuture;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.impl.operationservice.impl.OperationServiceImpl;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ReadMetricsOperationTest extends HazelcastTestSupport {

    @Test
    public void testMetricsPresent_map() {
        Config config = new Config();
        config.getMetricsConfig().setEnabled(true);
        config.setProperty(Diagnostics.METRICS_LEVEL.getName(), ProbeLevel.INFO.name());
        config.setProperty(Diagnostics.METRICS_DISTRIBUTED_DATASTRUCTURES.getName(), "true");

        HazelcastInstance hzInstance = createHazelcastInstance(config);
        IMap<Object, Object> map = hzInstance.getMap("map");
        map.put("key", "value");

        NodeEngineImpl nodeEngine = getNode(hzInstance).getNodeEngine();
        OperationServiceImpl operationService = nodeEngine.getOperationService();

        AtomicLong nextSequence = new AtomicLong();
        assertTrueEventually(() -> {
            ReadMetricsOperation readMetricsOperation = new ReadMetricsOperation(nextSequence.get());
            InternalCompletableFuture<Object> future = operationService
                    .invokeOnTarget(MetricsService.SERVICE_NAME, readMetricsOperation, nodeEngine.getThisAddress());

            RingbufferSlice<Map.Entry<Long, byte[]>> ringbufferSlice = (RingbufferSlice) future.get();

            MetricsResultSet metricsResultSet = new MetricsResultSet(ringbufferSlice);
            nextSequence.set(metricsResultSet.nextSequence());

            boolean mapMetric = false;
            for (MetricsResultSet.MetricsCollection coll : metricsResultSet.collections()) {
                for (Metric metric : coll) {
                    mapMetric |= metric.key().contains("map[");
                }
            }
            assertTrue(mapMetric);
        });
    }
}
