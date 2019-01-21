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

package com.hazelcast.test.jitter;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.hazelcast.test.jitter.JitterRule.RESOLUTION_NANOS;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

public class JitterThread extends Thread {

    private final JitterRecorder jitterRecorder;
    private final BlockingQueue<Jitter> jitterQueue;
    private final InfluxDB influxDB;
    private final String machine;


    JitterThread(JitterRecorder jitterRecorder) {
        this.jitterRecorder = jitterRecorder;

        setName("JitterThread");
        setDaemon(true);

        try {
            machine = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        influxDB = InfluxDBFactory.connect("http://lab:8086", "hiccup", "hiccup");
        influxDB.setDatabase("hiccup");
        influxDB.setRetentionPolicy("autogen");

        influxDB.enableBatch(BatchOptions.DEFAULTS
                .bufferLimit(10000)
                .actions(10000)
                .flushDuration(1000)
        );

        jitterQueue = new LinkedBlockingQueue<Jitter>(10000);
        new InfluxReporterThread().start();
    }

    public void run() {
        long beforeNanos = System.nanoTime();
        long shortestHiccup = Long.MAX_VALUE;
        while (true) {
            long beforeMillis = System.currentTimeMillis();
            sleepNanos(RESOLUTION_NANOS);
            long after = System.nanoTime();
            long delta = after - beforeNanos;
            long currentHiccup = delta - RESOLUTION_NANOS;

            // subtract the shortest observed hiccups, as that's an inherit
            // cost of the measuring loop and OS scheduler imprecision
            shortestHiccup = min(shortestHiccup, currentHiccup);
            currentHiccup -= shortestHiccup;

            jitterRecorder.recordPause(beforeMillis, currentHiccup);
            jitterQueue.offer(new Jitter(beforeMillis, currentHiccup));

            beforeNanos = after;
        }
    }

    private void sleepNanos(long duration) {
        parkNanos(duration);
    }

    private static class Jitter {
        private final long timestamp;
        private final long hiccup;

        private Jitter(long timestamp, long hiccup) {
            this.timestamp = timestamp;
            this.hiccup = hiccup;
        }
    }

    private class InfluxReporterThread extends Thread {

        private InfluxReporterThread() {
        }

        public void run() {
            while (true) {
                try {
                    Jitter jitter = jitterQueue.take();

                    influxDB.write(Point.measurement("hiccups")
                                        .time(jitter.timestamp, MILLISECONDS)
                                        .addField("value", jitter.hiccup)
                                        .addField("host", machine)
                                        .build());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
