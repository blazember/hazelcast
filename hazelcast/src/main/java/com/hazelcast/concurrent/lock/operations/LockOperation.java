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

package com.hazelcast.concurrent.lock.operations;

import com.hazelcast.concurrent.lock.LockDataSerializerHook;
import com.hazelcast.concurrent.lock.LockStoreImpl;
import com.hazelcast.concurrent.lock.LockWaitNotifyKey;
import com.hazelcast.core.OperationTimeoutException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.BackupAwareOperation;
import com.hazelcast.spi.BlockingOperation;
import com.hazelcast.spi.ObjectNamespace;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.WaitNotifyKey;
import com.hazelcast.spi.impl.MutatingOperation;

public class LockOperation extends AbstractLockOperation implements BlockingOperation, BackupAwareOperation, MutatingOperation {

    public LockOperation() {
    }

    public LockOperation(ObjectNamespace namespace, Data key, long threadId, long leaseTime, long timeout) {
        super(namespace, key, threadId, leaseTime, timeout);
    }

    public LockOperation(ObjectNamespace namespace, Data key, long threadId, long leaseTime, long timeout, long referenceId) {
        super(namespace, key, threadId, leaseTime, timeout);
        setReferenceCallId(referenceId);
    }

    @Override
    public void run() throws Exception {
        ILogger logger = getLogger();
        Object keyObj = getNodeEngine().getSerializationService().toObject(key);
        logger.info("***** Acquiring lock " + namespace.getObjectName()
                + " for " + getCallerAddress() + " - " + getCallerUuid() + ", thread ID: " + threadId + " key: " + keyObj);
        final boolean lockResult = getLockStore().lock(key, getCallerUuid(), threadId, getReferenceCallId(), leaseTime);
        response = lockResult;

        if (logger.isFinestEnabled()) {
            if (lockResult) {
                logger.info("Acquired lock " + namespace.getObjectName()
                        + " for " + getCallerAddress() + " - " + getCallerUuid() + ", thread ID: " + threadId + " key: "
                        + keyObj);
            } else {
                logger.finest("Could not acquire lock " + namespace.getObjectName()
                        + " as owned by " + getLockStore().getOwnerInfo(key) + " key: " + keyObj);
            }
        }
    }

    @Override
    public Operation getBackupOperation() {
        LockBackupOperation operation = new LockBackupOperation(namespace, key, threadId, leaseTime, getCallerUuid());
        operation.setReferenceCallId(getReferenceCallId());
        return operation;
    }

    @Override
    public boolean shouldBackup() {
        return Boolean.TRUE.equals(response);
    }

    @Override
    public final WaitNotifyKey getWaitKey() {
        return new LockWaitNotifyKey(namespace, key);
    }

    @Override
    public final boolean shouldWait() {
        LockStoreImpl lockStore = getLockStore();
        return getWaitTimeout() != 0 && !lockStore.canAcquireLock(key, getCallerUuid(), threadId);
    }

    @Override
    public final void onWaitExpire() {
        Object response;
        long timeout = getWaitTimeout();
        if (timeout < 0 || timeout == Long.MAX_VALUE) {
            response = new OperationTimeoutException();
        } else {
            response = Boolean.FALSE;
        }
        sendResponse(response);
    }

    @Override
    public int getId() {
        return LockDataSerializerHook.LOCK;
    }
}
