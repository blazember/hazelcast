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

package com.hazelcast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

public class BindExceptionReproducerTest {

    private static final int TIMEOUT_SECS = 2;
    private static final int PARALLEL_BINDERS = 2;
    private ExecutorService threadPool;

    @Before
    public void setUp() {
        threadPool = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        threadPool.shutdownNow();
    }

    @Test
    public void test() throws InterruptedException {
        // CAS over this to avoid false positives
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        CountDownLatch errorLatch = new CountDownLatch(1);

        for (int i = 0; i < PARALLEL_BINDERS; i++) {
            threadPool.submit(new Binder(atomicBoolean, errorLatch));
        }

        assertFalse(errorLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
    }

    private static class Binder implements Runnable {
        private final AtomicBoolean atomicBoolean;
        private final CountDownLatch errorLatch;

        private Binder(AtomicBoolean atomicBoolean, CountDownLatch errorLatch) {
            this.atomicBoolean = atomicBoolean;
            this.errorLatch = errorLatch;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (atomicBoolean.compareAndSet(false, true)) {
                        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                        ServerSocket serverSocket = serverSocketChannel.socket();
                        serverSocket.setReuseAddress(true);
                        serverSocket.setSoTimeout(1000);
                        serverSocket.bind(new InetSocketAddress("0.0.0.0", 5701));
                        // 
                        serverSocketChannel.close();
                        atomicBoolean.set(false);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errorLatch.countDown();
            }
        }
    }
}
