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

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

class BufferPersister {
    static void persist(ByteBuffer buffer, String threadName) {
        RandomAccessFile file = null;
        FileChannel channel = null;
        try {
            String fileName = System.getProperty("user.home") + "/trace/" + threadName;
            file = new RandomAccessFile(fileName, "rw");
            channel = file.getChannel();
            buffer.position(0);

            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIfNotNull(channel);
            closeIfNotNull(file);
        }
    }

    private static void closeIfNotNull(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ok, we couldn't close it so what
            }
        }
    }
}
