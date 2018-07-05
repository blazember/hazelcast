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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.PriorityQueue;

public class TraceForHumanBeings {
    private final PriorityQueue<TraceEvent> queue = new PriorityQueue<TraceEvent>();

    private void readDir() {
        File directory = new File(System.getProperty("user.home") + "/trace");

        File[] fList = directory.listFiles();
        for (File file : fList) {
            readFile(System.getProperty("user.home") + "/trace/", file.getName());
        }
    }

    private void readFile(String path, String threadName) {
        RandomAccessFile file = null;
        FileChannel channel = null;
        try {
            file = new RandomAccessFile(path + threadName, "rw");
            channel = file.getChannel();

            long fileSize = file.length();
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) fileSize);
            channel.read(byteBuffer);

            byteBuffer.position(0);

            processBuffer(byteBuffer, threadName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processBuffer(ByteBuffer byteBuffer, String threadName) {
        while (byteBuffer.position() < byteBuffer.capacity() && byteBuffer.getInt(byteBuffer.position()) != 0) {
            int size = byteBuffer.getInt(byteBuffer.position());
            if (size != 0) {
                TraceEvent traceEvent = TraceEvent.createFromBuffer(byteBuffer, threadName);
                queue.offer(traceEvent);
            }
        }
    }

    private void writeHumanReadable(String path) {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(path, "rw");

            while (!queue.isEmpty()) {
                TraceEvent traceEvent = queue.poll();
                Date timestamp = new Date(traceEvent.getTimestamp());
                StringBuffer sb = new StringBuffer();
                sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(timestamp));
                sb.append(" - ");
                sb.append(traceEvent.getThreadName());
                sb.append(" - ");
                sb.append(new String(traceEvent.getDataBytes()));
                file.writeBytes(sb.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
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

    public static void main(String[] args) {
        TraceForHumanBeings traceForHumanBeings = new TraceForHumanBeings();
        traceForHumanBeings.readDir();
        traceForHumanBeings.writeHumanReadable(System.getProperty("user.home") + "/trace/trace_for_human_beings.log");
    }
}
