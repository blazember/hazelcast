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
import com.hazelcast.config.MetricsConfig;
import com.hazelcast.internal.diagnostics.Diagnostics;
import com.hazelcast.internal.metrics.MetricsPublisher;
import com.hazelcast.internal.metrics.MetricsRegistry;
import com.hazelcast.internal.metrics.ProbeLevel;
import com.hazelcast.internal.metrics.managementcenter.ConcurrentArrayRingbuffer;
import com.hazelcast.internal.metrics.managementcenter.ConcurrentArrayRingbuffer.RingbufferSlice;
import com.hazelcast.internal.metrics.managementcenter.ManagementCenterPublisher;
import com.hazelcast.internal.metrics.renderers.ProbeRenderer;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.ExecutionService;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.impl.operationservice.LiveOperations;
import com.hazelcast.spi.impl.operationservice.LiveOperationsTracker;
import com.hazelcast.spi.impl.operationservice.Operation;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

/**
 * Service collecting the Metrics periodically and publishes them via
 * {@link MetricsPublisher}s.
 *
 * @since 4.0
 */
public class MetricsService implements ManagedService, LiveOperationsTracker {
    public static final String SERVICE_NAME = "hz:impl:metricsService";

    private final NodeEngineImpl nodeEngine;
    private final ILogger logger;
    private MetricsConfig config;
    private final LiveOperationRegistry liveOperationRegistry;
    // Holds futures for pending read metrics operations
    private final ConcurrentMap<CompletableFuture<RingbufferSlice<Map.Entry<Long, byte[]>>>, Long>
            pendingReads = new ConcurrentHashMap<>();
    private final ProbeRenderer probeRenderer = new PublisherProbeRenderer();

    /**
     * Ringbuffer which stores a bounded history of metrics. For each round of collection,
     * the metrics are compressed into a blob and stored along with the timestamp,
     * with the format (timestamp, byte[])
     */
    private ConcurrentArrayRingbuffer<Map.Entry<Long, byte[]>> metricsJournal;
    private volatile ScheduledFuture<?> scheduledFuture;

    private List<MetricsPublisher> publishers;

    private Supplier<MetricsRegistry> metricsRegistrySupplier;

    // pauses the collection service for testing
    private volatile boolean paused;

    public MetricsService(NodeEngine nodeEngine) {
        this(nodeEngine, ((NodeEngineImpl) nodeEngine)::getMetricsRegistry);
    }

    public MetricsService(NodeEngine nodeEngine, Supplier<MetricsRegistry> metricsRegistrySupplier) {
        this.nodeEngine = (NodeEngineImpl) nodeEngine;
        this.logger = nodeEngine.getLogger(getClass());
        this.config = nodeEngine.getConfig().getMetricsConfig();
        this.liveOperationRegistry = new LiveOperationRegistry();
        this.metricsRegistrySupplier = metricsRegistrySupplier;
    }

    @Override
    public void init(NodeEngine nodeEngine, Properties properties) {
        this.publishers = getPublishers();

        if (publishers.isEmpty()) {
            return;
        }

        logger.info("Configuring metrics collection, collection interval=" + config.getCollectionIntervalSeconds()
                + " seconds, retention=" + config.getRetentionSeconds() + " seconds, publishers="
                + publishers.stream().map(MetricsPublisher::name).collect(joining(", ", "[", "]")));

        ExecutionService executionService = nodeEngine.getExecutionService();
        scheduledFuture = executionService.scheduleWithRepetition("MetricsPublisher", this::collectMetrics, 1,
                config.getCollectionIntervalSeconds(), TimeUnit.SECONDS);
    }

    // visible for testing
    void collectMetrics() {
        collectMetrics(probeRenderer);
    }

    // visible for testing
    void collectMetrics(ProbeRenderer probeRenderer) {
        if (paused) {
            logger.fine("Metrics not collected, service is paused.");
            return;
        }

        metricsRegistrySupplier.get().render(probeRenderer);
        //            this.nodeEngine.getMetricsRegistry().render(renderer);
        for (MetricsPublisher publisher : publishers) {
            try {
                publisher.whenComplete();
            } catch (Exception e) {
                logger.severe("Error completing publication for publisher " + publisher, e);
            }
        }
    }

    public LiveOperationRegistry getLiveOperationRegistry() {
        return liveOperationRegistry;
    }

    @Override
    public void populate(LiveOperations liveOperations) {
        liveOperationRegistry.populate(liveOperations);
    }

    /**
     * Read metrics from the journal from the given sequence
     */
    public CompletableFuture<RingbufferSlice<Map.Entry<Long, byte[]>>> readMetrics(long startSequence) {
        if (!config.isEnabled()) {
            throw new IllegalArgumentException("Metrics collection is not enabled");
        }
        CompletableFuture<RingbufferSlice<Map.Entry<Long, byte[]>>> future = new CompletableFuture<>();
        future.whenComplete(withTryCatch(logger, (s, e) -> pendingReads.remove(future)));
        pendingReads.put(future, startSequence);

        tryCompleteRead(future, startSequence);

        return future;
    }

    private void tryCompleteRead(CompletableFuture<RingbufferSlice<Map.Entry<Long, byte[]>>> future, long sequence) {
        try {
            RingbufferSlice<Map.Entry<Long, byte[]>> slice = metricsJournal.copyFrom(sequence);
            if (!slice.isEmpty()) {
                future.complete(slice);
            }
        } catch (Exception e) {
            logger.severe("Error reading from metrics journal, sequence: " + sequence, e);
            future.completeExceptionally(e);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void shutdown(boolean terminate) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }

        for (MetricsPublisher publisher : publishers) {
            try {
                publisher.shutdown();
            } catch (Exception e) {
                logger.warning("Error shutting down metrics publisher " + publisher.name(), e);
            }
        }
    }

    // apply MetricsConfig to HZ properties
    // these properties need to be set here so that the metrics get applied from startup
    public static void applyMetricsConfig(Config hzConfig, MetricsConfig metricsConfig) {
        if (metricsConfig.isEnabled()) {
            hzConfig.setProperty(Diagnostics.METRICS_LEVEL.getName(), ProbeLevel.INFO.name());
            if (metricsConfig.isMetricsForDataStructuresEnabled()) {
                hzConfig.setProperty(Diagnostics.METRICS_DISTRIBUTED_DATASTRUCTURES.getName(), "true");
            }
        }
    }

    private List<MetricsPublisher> getPublishers() {
        List<MetricsPublisher> publishers = new ArrayList<>();
        if (config.isEnabled()) {
            int journalSize = Math.max(
                    1, (int) Math.ceil((double) config.getRetentionSeconds() / config.getCollectionIntervalSeconds())
            );
            metricsJournal = new ConcurrentArrayRingbuffer<>(journalSize);
            ManagementCenterPublisher publisher = new ManagementCenterPublisher(this.nodeEngine.getLoggingService(),
                    (blob, ts) -> {
                        metricsJournal.add(entry(ts, blob));
                        pendingReads.forEach(this::tryCompleteRead);
                    }
            );
            publishers.add(publisher);
        }

        // TODO add JmxPublisher
        //        if (config.isJmxEnabled()) {
        //            publishers.add(new JmxPublisher(nodeEngine.getHazelcastInstance().getName(), "com.hazelcast"));
        //        }

        return publishers;
    }

    /**
     * Pause collection of metrics for testing
     */
    void pauseCollection() {
        this.paused = true;
    }

    /**
     * Resume collection of metrics for testing
     */
    void resumeCollection() {
        this.paused = false;
    }

    /**
     * A probe renderer which renders the metrics to all the given publishers.
     */
    private class PublisherProbeRenderer implements ProbeRenderer {
        @Override
        public void renderLong(String name, long value) {
            for (MetricsPublisher publisher : publishers) {
                try {
                    publisher.publishLong(name, value);
                } catch (Exception e) {
                    logError(name, value, publisher, e);
                }
            }
        }

        @Override
        public void renderDouble(String name, double value) {
            for (MetricsPublisher publisher : publishers) {
                try {
                    publisher.publishDouble(name, value);
                } catch (Exception e) {
                    logError(name, value, publisher, e);
                }
            }
        }

        @Override
        public void renderException(String name, Exception e) {
            logger.warning("Error when rendering '" + name + '\'', e);
        }

        @Override
        public void renderNoValue(String name) {
            // noop
        }

        private void logError(String name, Object value, MetricsPublisher publisher, Exception e) {
            logger.fine("Error publishing metric to: " + publisher.name() + ", metric=" + name + ", value=" + value, e);
        }
    }

    // =================================================================
    // TODO move out to utility class
    // =================================================================

    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    /**
     * Utility to make sure exceptions inside
     * {@link java.util.concurrent.CompletionStage#whenComplete(BiConsumer)} are not swallowed.
     * Exceptions will be caught and logged using the supplied logger.
     */
    @Nonnull
    public static <T> BiConsumer<T, ? super Throwable> withTryCatch(
            @Nonnull ILogger logger, @Nonnull BiConsumer<T, ? super Throwable> consumer
    ) {
        return withTryCatch(logger, "Exception during callback", consumer);
    }

    /**
     * Utility to make sure exceptions inside
     * {@link java.util.concurrent.CompletionStage#whenComplete(BiConsumer)} are not swallowed.
     * Exceptions will be caught and logged using the supplied logger and message.
     */
    @Nonnull
    public static <T> BiConsumer<T, ? super Throwable> withTryCatch(
            @Nonnull ILogger logger, @Nonnull String message, @Nonnull BiConsumer<T, ? super Throwable> consumer
    ) {
        return (r, t) -> {
            try {
                consumer.accept(r, t);
            } catch (Throwable e) {
                logger.severe(message, e);
            }
        };
    }

    public static class LiveOperationRegistry {
        // memberAddress -> callId -> operation
        private final ConcurrentHashMap<Address, Map<Long, Operation>> liveOperations = new ConcurrentHashMap<>();

        public void register(Operation operation) {
            Map<Long, Operation> callIds = liveOperations.computeIfAbsent(operation.getCallerAddress(),
                    (key) -> new ConcurrentHashMap<>());
            if (callIds.putIfAbsent(operation.getCallId(), operation) != null) {
                throw new IllegalStateException("Duplicate operation during registration of operation=" + operation);
            }
        }

        public void deregister(Operation operation) {
            Map<Long, Operation> operations = liveOperations.get(operation.getCallerAddress());

            if (operations == null) {
                throw new IllegalStateException("Missing address during de-registration of operation=" + operation);
            }

            if (operations.remove(operation.getCallId()) == null) {
                throw new IllegalStateException("Missing operation during de-registration of operation=" + operation);
            }
        }

        public void populate(LiveOperations liveOperations) {
            this.liveOperations.forEach((key, value) -> value.keySet().forEach(callId -> liveOperations.add(key, callId)));
        }

    }
}
