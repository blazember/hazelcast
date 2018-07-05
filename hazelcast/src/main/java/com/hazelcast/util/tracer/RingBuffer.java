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

import java.nio.ByteBuffer;

import static com.hazelcast.nio.Bits.INT_SIZE_IN_BYTES;

/**
 * +-------------------+------------------+------+
 * |                   |                  |      |
 * | size of entry (4) | size of data (4) | data |
 * |                   |                  |      |
 * +-------------------+------------------+------+
 */
public class RingBuffer {
    static final int META_SIZE = INT_SIZE_IN_BYTES + INT_SIZE_IN_BYTES;
    private static final int MAX_DATA_SIZE = Integer.MAX_VALUE - META_SIZE;
    static final int LEFTOVER_PAD_THRESHOLD = META_SIZE + 8;

    private final ByteBuffer buffer;

    private int lastPreparedPosition = -1;

    public RingBuffer(int bufferSize) {
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Prepares and positions the buffer so that the writer can write
     * the requested bytes into it.
     *
     * @param dataSize the size of the data in bytes that the writer wants
     *                 to write, without the metadata
     * @return the prepared {@link ByteBuffer}
     */
    // TODO: it is not safe to return the ByteBuffer here, RB should offer the same methods that BB does
    public ByteBuffer prepare(int dataSize) {
        if (dataSize > MAX_DATA_SIZE) {
            throw new IllegalArgumentException(
                    "Requested dataSize exceeds maximum data dataSize. dataSize=" + dataSize + " maximum data dataSize="
                            + MAX_DATA_SIZE);
        }

        if (dataSize > buffer.capacity()) {
            throw new IllegalArgumentException(
                    "Buffer is smaller than requested dataSize. dataSize=" + dataSize + " buffer.capacity=" + buffer.capacity());
        }

        // first write
        if (lastPreparedPosition < 0) {
            buffer.position(0);
        } else {
            // seek to the next position to write
            int currentPosition = lastPreparedPosition + buffer.getInt(lastPreparedPosition) >= buffer.capacity()
                    ? 0
                    : lastPreparedPosition + buffer.getInt(lastPreparedPosition);
            buffer.position(currentPosition);
        }

        // if there is not enough place at the end of the buffer
        if (dataSize + META_SIZE > buffer.capacity() - buffer.position()) {
            buffer.position(0);
        }

        lastPreparedPosition = buffer.position();

        prepareAtPosition(dataSize);

        return buffer;
    }

    private void prepareAtPosition(int requestedDataSize) {
        final int fullEntrySize = META_SIZE + requestedDataSize;
        int sizeOnPosition = buffer.getInt(buffer.position());

        // written first time, we know here that we have enough bytes in the buffer to store the entry
        if (sizeOnPosition == 0) {
            buffer.putInt(fullEntrySize);
            buffer.putInt(requestedDataSize);

            return;
        }

        // if the leftover from the previous message is smaller
        // we need to merge slots
        if (sizeOnPosition < fullEntrySize) {
            int mergedSize = sizeOnPosition;
            while (mergedSize < fullEntrySize) {
                int newIndex = buffer.position() + mergedSize;
                int sizeOnNewIndex = buffer.getInt(newIndex);

                // we never wrote to this position, behind this the buffer is empty
                if (sizeOnNewIndex == 0) {
                    sizeOnNewIndex = buffer.capacity() - newIndex;
                }

                mergedSize += sizeOnNewIndex;
            }
            buffer.putInt(buffer.position(), mergedSize);
            sizeOnPosition = mergedSize;
        }

        if (sizeOnPosition >= fullEntrySize) {
            final int leftoverSize = sizeOnPosition - fullEntrySize;

            // we can insert an empty slot after the written
            if (leftoverSize > LEFTOVER_PAD_THRESHOLD) {
                final int preparedPosition = buffer.position();

                // write an empty slot after the prepared
                buffer.position(preparedPosition + fullEntrySize);
                buffer.putInt(leftoverSize);
                buffer.putInt(0);

                // write the size of the prepared slot
                buffer.position(preparedPosition);
                buffer.putInt(fullEntrySize);
                buffer.putInt(requestedDataSize);

                return;
            }
            // we need to pad the current slot
            else {
                final int paddedEntrySize = fullEntrySize + leftoverSize;
                buffer.putInt(paddedEntrySize);
                buffer.putInt(requestedDataSize);

                return;
            }
        }
    }

}
