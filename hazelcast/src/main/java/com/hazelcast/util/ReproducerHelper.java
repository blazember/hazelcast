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

package com.hazelcast.util;

import com.hazelcast.instance.Node;
import com.hazelcast.spi.NodeEngine;

import java.util.concurrent.CountDownLatch;

public class ReproducerHelper {
    // CacheBasicServerTest
    private static final String HZ_INSTANCE = "hzInstance_1";

    // NearCacheTest
    //    private static final String HZ_INSTANCE = "hzInstance_2";

    public static CountDownLatch shutdownStarted = new CountDownLatch(1);
    public static CountDownLatch notActiveExceptionThrown = new CountDownLatch(1);
    public static CountDownLatch destroyExecuted = new CountDownLatch(1);
    public static CountDownLatch errorSetInFuture = new CountDownLatch(1);

    public static boolean onExpectedInstance(Node node) {
        return node.hazelcastInstance.getName().contains(HZ_INSTANCE);
    }

    public static boolean onExpectedInstance(NodeEngine nodeEngine) {
        return nodeEngine.getHazelcastInstance().getName().contains(HZ_INSTANCE);
    }
}
