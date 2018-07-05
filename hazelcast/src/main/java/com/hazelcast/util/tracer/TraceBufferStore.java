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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.currentThread;

public class TraceBufferStore {
    private static final int THREAD_BUFFER_SIZE = 16 * 1024;

    private final ConcurrentMap<Thread, BufferHandle> bufferHandles = new ConcurrentHashMap<Thread, BufferHandle>();
    private static final TraceBufferStore INSTANCE = new TraceBufferStore();

    public static BufferHandle acquireBuffer() {
        BufferHandle bufferHandle = getBufferHandleInternal();
        while (!bufferHandle.acquire()) {
            bufferHandle = getBufferHandleInternal();
            System.out.println("g");
        }

        return bufferHandle;
    }

    private static BufferHandle getBufferHandleInternal() {
        final Thread currentThread = currentThread();
        BufferHandle bufferHandle = INSTANCE.bufferHandles.get(currentThread);
        if (bufferHandle == null) {
            bufferHandle = new BufferHandle(THREAD_BUFFER_SIZE);

            INSTANCE.bufferHandles.put(currentThread, bufferHandle);
        }
        return bufferHandle;
    }

    public Collection<ThreadBufferPair> getAndRemoveAllBuffers() {
        List<ThreadBufferPair> removedBufferHandles = new LinkedList<ThreadBufferPair>();
        for (Thread thread : this.bufferHandles.keySet()) {
            BufferHandle removedHandle = this.bufferHandles.remove(thread);
            removedBufferHandles.add(new ThreadBufferPair(thread, removedHandle));
        }

        return removedBufferHandles;
    }

    public static TraceBufferStore getInstance() {
        return INSTANCE;
    }

    static class ThreadBufferPair {
        private final Thread thread;
        private final BufferHandle bufferHandle;

        ThreadBufferPair(Thread thread, BufferHandle bufferHandle) {
            this.thread = thread;
            this.bufferHandle = bufferHandle;
        }

        Thread getThread() {
            return thread;
        }

        BufferHandle getBufferHandle() {
            return bufferHandle;
        }
    }
}
