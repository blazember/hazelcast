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

package com.hazelcast.util.tracer.conv;

import com.hazelcast.nio.Bits;

import java.nio.ByteBuffer;

public class TraceEvent implements Comparable<TraceEvent> {
    private final long timestamp;
    private final long nanotime;
    private final String threadName;
    private final ByteBuffer buffer;
    private final int dataStart;
    private final int dataLength;

    public TraceEvent(long timestamp, long nanotime, String threadName, ByteBuffer buffer, int dataStart,
                      int dataLength) {
        this.timestamp = timestamp;
        this.nanotime = nanotime;
        this.threadName = threadName;
        this.buffer = buffer;
        this.dataStart = dataStart;
        this.dataLength = dataLength;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public byte[] getDataBytes() {
        byte[] dataBytes = new byte[dataLength];
        buffer.position(dataStart);
        buffer.get(dataBytes);
        return dataBytes;
    }

    @Override
    public int compareTo(TraceEvent o) {
        if (this.timestamp <= o.timestamp && this.nanotime < o.nanotime) {
            return -1;
        } else if (this.timestamp == o.timestamp && this.nanotime == o.nanotime) {
            return 0;
        }

        return 1;
    }

    public static TraceEvent createFromBuffer(ByteBuffer buffer, String threadName) {
        // header
        int blockPosition = buffer.position();
        int blockSize = buffer.getInt();
        int dataSize = buffer.getInt();

        // data:header
        long timestamp = buffer.getLong();
        long nanotime = buffer.getLong();

        // data:data
        int dataStart = buffer.position();
        int dataLength = dataSize - Bits.LONG_SIZE_IN_BYTES - Bits.LONG_SIZE_IN_BYTES;
        buffer.position(blockPosition + blockSize);

        return new TraceEvent(timestamp, nanotime, threadName, buffer, dataStart, dataLength);
    }
}
