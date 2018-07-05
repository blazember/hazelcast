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

import java.util.concurrent.atomic.AtomicReference;

import static com.hazelcast.util.tracer.BufferState.FINISHED;
import static com.hazelcast.util.tracer.BufferState.READY;
import static com.hazelcast.util.tracer.BufferState.WRITTEN;

public class BufferHandle {
    private final RingBuffer ringBuffer;
    private final AtomicReference<BufferState> state;

    public BufferHandle(int size) {
        this.ringBuffer = new RingBuffer(size);
        this.state = new AtomicReference<BufferState>(READY);
    }

    public RingBuffer getRingBuffer() {
        return ringBuffer;
    }

    public AtomicReference<BufferState> getState() {
        return state;
    }

    public boolean acquire() {
        return state.compareAndSet(READY, WRITTEN);
    }

    public boolean release() {
        return state.compareAndSet(WRITTEN, READY);
    }

    boolean finish() {
        return state.compareAndSet(READY, FINISHED);
    }

}
