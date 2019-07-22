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
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.metrics.MetricsRegistry;
import com.hazelcast.internal.metrics.Probe;
import com.hazelcast.internal.metrics.ProbeLevel;
import com.hazelcast.internal.metrics.managementcenter.ConcurrentArrayRingbuffer.RingbufferSlice;
import com.hazelcast.internal.metrics.managementcenter.MetricsResultSet;
import com.hazelcast.internal.metrics.renderers.ProbeRenderer;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.impl.executionservice.impl.ExecutionServiceImpl;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class MetricsServiceTest {

    @Mock
    private Node nodeMock;
    @Mock
    private HazelcastInstance hzMock;
    @Mock
    private NodeEngineImpl nodeEngineMock;
    @Mock
    private LoggingService loggingServiceMock;
    @Mock
    private ILogger loggerMock;
    @Mock
    private ProbeRenderer probeRendererMock;

    private MetricsRegistry metricsRegistry;
    private TestProbeSource testProbeSource;

    @Before
    public void setUp() {
        initMocks(this);

        metricsRegistry = new MetricsRegistryImpl(loggerMock, ProbeLevel.INFO);

        when(nodeMock.getLogger(any(Class.class))).thenReturn(loggerMock);
        when(nodeMock.getLogger(any(String.class))).thenReturn(loggerMock);
        when(nodeEngineMock.getNode()).thenReturn(nodeMock);
        when(nodeEngineMock.getConfig()).thenReturn(new Config());
        when(nodeEngineMock.getLoggingService()).thenReturn(loggingServiceMock);
        when(nodeEngineMock.getLogger(any(Class.class))).thenReturn(loggerMock);
        when(nodeEngineMock.getMetricsRegistry()).thenReturn(metricsRegistry);
        when(nodeEngineMock.getHazelcastInstance()).thenReturn(hzMock);

        ExecutionServiceImpl executionService = new ExecutionServiceImpl(nodeEngineMock);
        when(nodeEngineMock.getExecutionService()).thenReturn(executionService);

        when(loggingServiceMock.getLogger(any(Class.class))).thenReturn(loggerMock);

        testProbeSource = new TestProbeSource();

        metricsRegistry.scanAndRegister(testProbeSource, "test");
    }

    @Test
    public void testUpdatesRenderedInOrder() {
        MetricsService metricsService = new MetricsService(nodeEngineMock, () -> metricsRegistry);
        metricsService.init(nodeEngineMock, new Properties());

        testProbeSource.update(1, 1.5D);
        metricsService.collectMetrics(probeRendererMock);

        testProbeSource.update(2, 5.5D);
        metricsService.collectMetrics(probeRendererMock);

        InOrder inOrder = inOrder(probeRendererMock);
        inOrder.verify(probeRendererMock).renderDouble("test.doubleValue", 1.5D);
        inOrder.verify(probeRendererMock).renderLong("test.longValue", 1);
        inOrder.verify(probeRendererMock).renderDouble("test.doubleValue", 5.5D);
        inOrder.verify(probeRendererMock).renderLong("test.longValue", 2);
    }

    @Test
    public void testMetricsReadInOrder() throws Exception {
        MetricsService metricsService = new MetricsService(nodeEngineMock, () -> metricsRegistry);
        metricsService.init(nodeEngineMock, new Properties());

        testProbeSource.update(1, 1.5D);
        metricsService.collectMetrics();
        
        testProbeSource.update(2, 5.5D);
        metricsService.collectMetrics();

        AtomicLong nextSequence = new AtomicLong();
        MetricConsumer metricConsumerMock = mock(MetricConsumer.class);

        InOrder inOrder = inOrder(metricConsumerMock);
        CompletableFuture<RingbufferSlice<Map.Entry<Long, byte[]>>> future = metricsService.readMetrics(nextSequence.get());
        RingbufferSlice<Map.Entry<Long, byte[]>> ringbufferSlice = future.get();

        MetricsResultSet metricsResultSet = new MetricsResultSet(ringbufferSlice);
        nextSequence.set(metricsResultSet.nextSequence());

        metricsResultSet.collections().forEach(coll -> coll.forEach(metric -> {
            if (metric.key().contains("test.longValue")) {
                metricConsumerMock.consumeLong(metric.value());
            } else if (metric.key().contains("test.doubleValue")) {
                metricConsumerMock.consumeDouble(metric.value() / 10_000D);
            }
        }));

        inOrder.verify(metricConsumerMock).consumeDouble(1.5D);
        inOrder.verify(metricConsumerMock).consumeLong(1);
        inOrder.verify(metricConsumerMock).consumeDouble(5.5D);
        inOrder.verify(metricConsumerMock).consumeLong(2);
        inOrder.verifyNoMoreInteractions();
    }

    private static class TestProbeSource {
        @Probe
        private long longValue;

        @Probe
        private double doubleValue;

        private void update(long longValue, double doubleValue) {
            this.longValue = longValue;
            this.doubleValue = doubleValue;
        }

    }

    private interface MetricConsumer {
        void consumeLong(long value);

        void consumeDouble(double value);
    }
}
