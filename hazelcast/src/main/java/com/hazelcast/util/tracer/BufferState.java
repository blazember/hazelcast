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

public enum BufferState {
    /**
     * No thread is touching the given buffer. It is ready to be moved
     * to {@link #WRITTEN} state by.
     */
    READY,

    /**
     * The buffer is currently being written
     *
     * The flushing thread should wait for the writer thread to complete
     * before moves it to {@link #FINISHED} state.
     */
    WRITTEN,

    /**
     * The buffer is not anymore available for the writer thread. The
     * buffer has been replaced by a new buffer that the writer thread
     * should write into. The current buffer is being processed.
     */
    FINISHED
}
