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

import com.hazelcast.nio.Bits;

import java.nio.ByteBuffer;

/**
 * +---------------+--------------+------+
 * |               |              |      |
 * | timestamp (8) | nanotime (4) | data |
 * |               |              |      |
 * +---------------+--------------+------+
 */

public class TraceEventWriter {
    private static final int META_SIZE = Bits.LONG_SIZE_IN_BYTES + Bits.LONG_SIZE_IN_BYTES;

    public static <T> void write(T event, TraceEventTranslator<T> translator) {
        BufferHandle bufferHandle = null;
        int preparedFor = 0;
        int position = 0;
        try {
            bufferHandle = TraceBufferStore.acquireBuffer();
            final int eventSize = translator.getEventSize(event);
            final ByteBuffer byteBuffer = bufferHandle.getRingBuffer().prepare(eventSize + META_SIZE);

            preparedFor = eventSize + META_SIZE;
            position = byteBuffer.position();

            byteBuffer.putLong(System.currentTimeMillis());
            byteBuffer.putLong(System.nanoTime());
            translator.translate(event, null, byteBuffer);
        } catch (Throwable ex) {
            System.out.println(preparedFor);
            System.out.println(position);
            ex.printStackTrace();
        } finally {
            bufferHandle.release();
        }
    }
}
