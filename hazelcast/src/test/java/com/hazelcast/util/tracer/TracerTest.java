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

package com.hazelcast.util.tracer;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

public class TracerTest {
    private static final StringTranslator stringTranslator = new StringTranslator();

    @Test
    public void test() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
        executorService.scheduleAtFixedRate(new Writer(), 0, 1, MILLISECONDS);
        executorService.scheduleAtFixedRate(new Writer(), 0, 1, MILLISECONDS);
        executorService.scheduleAtFixedRate(new Writer(), 0, 1, MILLISECONDS);

        parkNanos(SECONDS.toNanos(3));

        executorService.shutdownNow();
        Collection<TraceBufferStore.ThreadBufferPair> allBuffers = TraceBufferStore.getInstance().getAndRemoveAllBuffers();
        for (TraceBufferStore.ThreadBufferPair pair : allBuffers) {
            BufferHandle bufferHandle = pair.getBufferHandle();
            while (!bufferHandle.finish()) {
                System.out.println("huh? " + bufferHandle.getState());
            }

            ByteBuffer buffer = bufferHandle.getRingBuffer().getBuffer();
            BufferPersister.persist(buffer, pair.getThread().getName());
        }
    }

    private static class Writer implements Runnable {

        @Override
        public void run() {
            TraceEventWriter.write("ENTER", stringTranslator);

            int anInt = ThreadLocalRandom.current().nextInt(1, 100);
            LockSupport.parkNanos(MILLISECONDS.toNanos(3));

            // do something
            TraceEventWriter.write("EXIT", stringTranslator);
        }
    }

    private static class StringTranslator implements TraceEventTranslator<String> {

        @Override
        public void translate(String event, MessageDescriptor descriptor, ByteBuffer buffer) {
            byte[] bytes = event.getBytes();
            buffer.put(bytes);
        }

        @Override
        public int getEventSize(String event) {
            return event.getBytes().length;
        }
    }
}
