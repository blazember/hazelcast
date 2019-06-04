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

package com.hazelcast.test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

class TestMetrics {
    private final AtomicReference<Metrics> metricsRef = new AtomicReference<>(new Metrics());

    static final TestMetrics INSTANCE = new TestMetrics();

    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final GarbageCollectorMXBean minorGcMXBean;
    private final GarbageCollectorMXBean majorGcMXBean;
    private final MetricsPersister metricsPersister;
    private final OperatingSystemMXBean operatingSystemMXBean;

    private TestMetrics() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        minorGcMXBean = ManagementFactory.getGarbageCollectorMXBeans().get(0);
        majorGcMXBean = ManagementFactory.getGarbageCollectorMXBeans().get(1);

        newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::recordSnapshot, 0, 1, TimeUnit.SECONDS);

        //        new MetricsUpdaterThread().start();
        metricsPersister = new MetricsPersister();
        metricsPersister.start();
    }

    void ensureRunning() {
        // NOP
    }

    void recordMetrics(String edition, String testClassName, String testName, long executionTime, long startTime) {
        recordSnapshot();

        // well, this is fine for this purpose... :)
        Metrics oldMetrics = metricsRef.get();
        metricsRef.getAndSet(new Metrics(oldMetrics));

        ManagementFactory.getThreadMXBean().resetPeakThreadCount();
        metricsPersister.metricsQueue.offer(new MetricsTask(edition, testClassName, testName, oldMetrics, executionTime,
                startTime));
    }

    private void recordSnapshot() {
        // threads
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        long startedThreadCount = threadMXBean.getTotalStartedThreadCount();
        Metrics metrics = metricsRef.get();
        if (peakThreadCount > metrics.peakAliveThreads) {
            metrics.peakAliveThreads = peakThreadCount;
        }
        metrics.startedThreads += startedThreadCount - metrics.startedThreads;

        // memory
        long usedHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
        long usedNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        if (usedHeap > metrics.peakHeapUsage) {
            metrics.peakHeapUsage = usedHeap;
        }
        if (usedNonHeap > metrics.peakNonHeapUsage) {
            metrics.peakNonHeapUsage = usedNonHeap;
        }

        double load = operatingSystemMXBean.getSystemLoadAverage();
        if (load > metrics.peakLoad1) {
            metrics.peakLoad1 = load;
        }

        // GC
        long minorGcTime = minorGcMXBean.getCollectionTime();
        long minorGcCount = minorGcMXBean.getCollectionCount();
        long majorGcTime = majorGcMXBean.getCollectionTime();
        long majorGcCount = majorGcMXBean.getCollectionCount();
        metrics.totalMinorGc += minorGcCount - metrics.totalMinorGc;
        metrics.totalMinorGcTime += minorGcTime - metrics.totalMinorGcTime;
        metrics.totalMajorGc += majorGcCount - metrics.totalMajorGc;
        metrics.totalMajorGcTime += majorGcTime - metrics.totalMajorGcTime;
    }

    class Metrics {
        private int peakAliveThreads;
        private int startedThreads;
        private double peakLoad1;
        private long totalMinorGcTime;
        private long totalMinorGc;
        private long totalMajorGcTime;
        private long totalMajorGc;
        private long peakHeapUsage;
        private long peakNonHeapUsage;

        private int startedThreadsAtCreation;
        private long totalMinorGcTimeAtCreation;
        private long totalMinorGcAtCreation;
        private long totalMajorGcTimeAtCreation;
        private long totalMajorGcAtCreation;

        private Metrics() {
        }

        private Metrics(Metrics oldMetrics) {
            startedThreadsAtCreation = oldMetrics.startedThreads;
            totalMinorGcTimeAtCreation = oldMetrics.totalMinorGcTime;
            totalMinorGcAtCreation = oldMetrics.totalMinorGc;
            totalMajorGcTimeAtCreation = oldMetrics.totalMajorGcTime;
            totalMajorGcAtCreation = oldMetrics.totalMajorGc;
        }

        public double getPeakLoad1() {
            return peakLoad1;
        }

        public int getPeakAliveThreads() {
            return peakAliveThreads;
        }

        public int getStartedThreads() {
            return startedThreads - startedThreadsAtCreation;
        }

        public long getTotalMinorGc() {
            return totalMinorGc > 0 ? totalMinorGc - totalMinorGcAtCreation : 0;
        }

        public long getTotalMinorGcTime() {
            return totalMinorGcTime > 0 ? totalMinorGcTime - totalMinorGcTimeAtCreation : 0;
        }

        public long getTotalMajorGc() {
            return totalMajorGc > 0 ? totalMajorGc - totalMajorGcAtCreation : 0;
        }

        public long getTotalMajorGcTime() {
            return totalMajorGcTime > 0 ? totalMajorGcTime - totalMajorGcTimeAtCreation : 0;
        }

        public long getPeakHeapUsage() {
            return peakHeapUsage;
        }

        public long getPeakNonHeapUsage() {
            return peakNonHeapUsage;
        }

        @Override
        public String toString() {
            return "Metrics{"
                    + "peakAliveThreads=" + getPeakAliveThreads()
                    + ", startedThreads=" + getStartedThreads()
                    + ", totalMinorGcTime=" + getTotalMinorGcTime()
                    + ", totalMinorGc=" + getTotalMinorGc()
                    + ", totalMajorGcTime=" + getTotalMajorGcTime()
                    + ", totalMajorGc=" + getTotalMajorGc()
                    + ", peakHeapUsage=" + getPeakHeapUsage()
                    + ", peakNonHeapUsage=" + getPeakNonHeapUsage()
                    + '}';
        }
    }

    private static class MetricsTask {
        private final String edition;
        private final String testClassName;
        private final String testName;
        private final Metrics metrics;
        private final long executionTime;
        private final long startTime;

        private MetricsTask(String edition, String testClassName, String testName, Metrics metrics, long executionTime,
                            long startTime) {
            this.edition = edition;
            this.testClassName = testClassName;
            this.testName = testName;
            this.metrics = metrics;
            this.executionTime = executionTime;
            this.startTime = startTime;
        }
    }

    private static class MetricsPersister extends Thread {
        private final String SQL = "{ ? = call record_metrics(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        private final BlockingQueue<MetricsTask> metricsQueue = new LinkedBlockingQueue<>();
        private Connection pgConn;

        private MetricsPersister() {
            try {
                pgConn = DriverManager.getConnection("jdbc:postgresql://192.168.2.11/hz_test", "hz_test", "hz_test");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                CallableStatement statement = pgConn.prepareCall(SQL);
                statement.registerOutParameter(1, Types.INTEGER);

                while (true) {
                    MetricsTask metricsTask = metricsQueue.take();

                    statement.setString(2, metricsTask.edition);
                    statement.setString(3, metricsTask.testClassName);
                    statement.setString(4, metricsTask.testName);
                    statement.setDouble(5, metricsTask.metrics.getPeakLoad1());
                    statement.setInt(6, metricsTask.metrics.getPeakAliveThreads());
                    statement.setInt(7, getStartedThreads(metricsTask));
                    statement.setLong(8, metricsTask.metrics.getTotalMinorGc());
                    statement.setLong(9, metricsTask.metrics.getTotalMinorGcTime());
                    statement.setLong(10, metricsTask.metrics.getTotalMajorGc());
                    statement.setLong(11, metricsTask.metrics.getTotalMajorGcTime());
                    statement.setLong(12, metricsTask.metrics.getPeakHeapUsage());
                    statement.setLong(13, metricsTask.metrics.getPeakNonHeapUsage());
                    statement.setLong(14, metricsTask.executionTime);
                    statement.setLong(15, metricsTask.startTime);

                    try {
                        statement.execute();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public int getStartedThreads(MetricsTask metricsTask) {
            return metricsTask.metrics.peakAliveThreads != 0 ? metricsTask.metrics.getStartedThreads() : 0;
        }
    }
}
