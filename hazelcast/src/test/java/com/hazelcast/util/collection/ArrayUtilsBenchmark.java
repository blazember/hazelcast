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

package com.hazelcast.util.collection;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//@BenchmarkMode({Mode.SampleTime})
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 5)
@State(Scope.Benchmark)
public class ArrayUtilsBenchmark {
    //    @Param({"10000000"})
    @Param({"10", "100", "1000", "10000", "100000", "1000000", "10000000"})
    private int arraySize;

    private int[] array;

    private Object[] arrayObj;

    @Setup
    public void setup() {
        array = new int[arraySize];
        arrayObj = new Object[arraySize];
    }

    @Benchmark
    public void fill_obj_jdk() {
        Arrays.fill(arrayObj, "a");
    }

    @Benchmark
    public void fill_obj_hz() {
        ArrayUtils.fill3(arrayObj, "a");
    }

    @Benchmark
    public void fill_obj_hz2() {
        ArrayUtils.fill(arrayObj, "a");
    }

    @Benchmark
    public void fill_int_jdk() {
        Arrays.fill(array, 0);
    }

    @Benchmark
    public void fill_int_hz() {
        ArrayUtils.fill4(array, 0);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ArrayUtilsBenchmark.class.getSimpleName())
                //                .addProfiler(LinuxPerfProfiler.class)
                .build();

        new Runner(opt).run();
    }
}
