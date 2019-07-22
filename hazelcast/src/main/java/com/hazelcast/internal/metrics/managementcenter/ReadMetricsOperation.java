/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.internal.metrics.managementcenter;

import com.hazelcast.internal.metrics.impl.MetricsService;
import com.hazelcast.internal.metrics.managementcenter.ConcurrentArrayRingbuffer.RingbufferSlice;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.spi.impl.operationservice.ReadonlyOperation;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import static com.hazelcast.internal.metrics.impl.MetricsService.withTryCatch;
import static com.hazelcast.util.ExceptionUtil.peel;

public class ReadMetricsOperation extends Operation implements ReadonlyOperation {

    private long offset;

    public ReadMetricsOperation(long offset) {
        this.offset = offset;
    }

    @Override
    public void beforeRun() {
        MetricsService service = getService();
        service.getLiveOperationRegistry().register(this);
    }

    @Override
    public void run() {
        ILogger logger = getNodeEngine().getLogger(getClass());
        MetricsService service = getService();
        CompletableFuture<RingbufferSlice<Entry<Long, byte[]>>> future = service.readMetrics(offset);
        future.whenComplete(withTryCatch(logger, (slice, error) -> doSendResponse(error != null ? peel(error) : slice)));
    }

    @Override
    public boolean returnsResponse() {
        return false;
    }

    @Override
    public Object getResponse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServiceName() {
        return MetricsService.SERVICE_NAME;
    }

    private void doSendResponse(Object value) {
        try {
            sendResponse(value);
        } finally {
            final MetricsService service = getService();
            service.getLiveOperationRegistry().deregister(this);
        }
    }
}
