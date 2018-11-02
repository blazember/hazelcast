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

package com.hazelcast.map.impl.recordstore;

import com.hazelcast.map.impl.VictimCache;
import com.hazelcast.map.impl.record.Record;
import com.hazelcast.nio.serialization.Data;

public class VictimCacheWriterMutationObserver implements RecordStoreMutationObserver<Record> {
    private final VictimCache victimCache;
    private final int partitionId;

    public VictimCacheWriterMutationObserver(VictimCache victimCache, int partitionId) {
        this.victimCache = victimCache;
        this.partitionId = partitionId;
    }

    @Override
    public void onClear() {
        victimCache.clear(partitionId);
    }

    @Override
    public void onPutRecord(Data key, Record record) {
        victimCache.remove(partitionId, record.getKey());
    }

    @Override
    public void onReplicationPutRecord(Data key, Record record) {
        victimCache.remove(partitionId, record.getKey());
    }

    @Override
    public void onUpdateRecord(Data key, Record record, Object newValue) {
        victimCache.remove(partitionId, record.getKey());
    }

    @Override
    public void onRemoveRecord(Data key, Record record) {
        victimCache.remove(partitionId, record.getKey());
    }

    @Override
    public void onEvictRecord(Data key, Record record) {
        victimCache.cache(partitionId, record.getKey(), (Data) record.getValue());
    }

    @Override
    public void onLoadRecord(Data key, Record record) {
        // NOP
    }

    @Override
    public void onDestroy(boolean internal) {
        victimCache.clear(partitionId);
    }

    @Override
    public void onReset() {
        victimCache.clear(partitionId);
    }
}
