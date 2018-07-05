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

import static org.junit.Assert.assertEquals;

public class RingBufferTest {

    @Test
    public void testPrepareEmptyBuffer() {
        RingBuffer ringBuffer = new RingBuffer(128);
        ByteBuffer preparedBuffer = ringBuffer.prepare(10);

        assertEquals(RingBuffer.META_SIZE, preparedBuffer.position());
        assertEquals(10 + RingBuffer.META_SIZE, preparedBuffer.getInt(0));
        assertEquals(10, preparedBuffer.getInt(4));

        preparedBuffer = ringBuffer.prepare(12);
        assertEquals(RingBuffer.META_SIZE + 10 + RingBuffer.META_SIZE, preparedBuffer.position());
        assertEquals(12 + RingBuffer.META_SIZE, preparedBuffer.getInt(18));
        assertEquals(12, preparedBuffer.getInt(22));
    }

    @Test
    public void testPrepareInsertsEmptySlot() {
        RingBuffer ringBuffer = new RingBuffer(128);
        ringBuffer.prepare(100);

        ByteBuffer preparedBuffer = ringBuffer.prepare(60);
        assertEquals(RingBuffer.META_SIZE, preparedBuffer.position());
        assertEquals(60 + RingBuffer.META_SIZE, preparedBuffer.getInt(0));
        assertEquals(60, preparedBuffer.getInt(4));
        assertEquals(40, preparedBuffer.getInt(68));
        assertEquals(0, preparedBuffer.getInt(72));
    }

    @Test
    public void testPreparePadsSlot() {
        RingBuffer ringBuffer = new RingBuffer(128);
        ringBuffer.prepare(100);
        ByteBuffer preparedBuffer = ringBuffer.prepare(100 - RingBuffer.LEFTOVER_PAD_THRESHOLD);

        assertEquals(RingBuffer.META_SIZE, preparedBuffer.position());
        assertEquals(100 + RingBuffer.META_SIZE, preparedBuffer.getInt(0));
        assertEquals(100 - RingBuffer.LEFTOVER_PAD_THRESHOLD, preparedBuffer.getInt(4));
    }

    @Test
    public void testPrepareMergesSlots() {
        RingBuffer ringBuffer = new RingBuffer(128);
        ringBuffer.prepare(40);
        ringBuffer.prepare(70);
        ByteBuffer preparedBuffer = ringBuffer.prepare(100);

        assertEquals(RingBuffer.META_SIZE, preparedBuffer.position());
        assertEquals(100 + RingBuffer.META_SIZE, preparedBuffer.getInt(0));
        assertEquals(100, preparedBuffer.getInt(4));
        assertEquals(48 + 78 - 108, preparedBuffer.getInt(108));
        assertEquals(0, preparedBuffer.getInt(112));
    }
}
