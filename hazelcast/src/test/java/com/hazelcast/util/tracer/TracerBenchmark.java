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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 5)
@State(Scope.Benchmark)
public class TracerBenchmark {
    private static final byte[] EVENT = "ENTER".getBytes();

    private TraceEventTranslator<byte[]> stringTranslator = new ByteArrayTranslator();

    @Benchmark
    public void benchmarkVanilla() {
        Blackhole.consumeCPU(100);
    }

    @Benchmark
    public void benchmarkTraced() {
        Blackhole.consumeCPU(100);
        TraceEventWriter.write(EVENT, stringTranslator);
    }

    private static class ByteArrayTranslator implements TraceEventTranslator<byte[]> {

        @Override
        public void translate(byte[] event, MessageDescriptor descriptor, ByteBuffer buffer) {
            buffer.put(event);
        }

        @Override
        public int getEventSize(byte[] event) {
            return event.length;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TracerBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
