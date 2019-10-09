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

package com.hazelcast.client.impl.protocol.task.executorservice;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ExecutorServiceSubmitScriptToPartitionCodec;
import com.hazelcast.client.impl.protocol.task.AbstractPartitionMessageTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.executor.impl.DistributedExecutorService;
import com.hazelcast.executor.impl.operations.CallableTaskOperation;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.security.SecurityContext;
import com.hazelcast.spi.impl.operationservice.Operation;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Permission;
import java.util.concurrent.Callable;

import static com.hazelcast.internal.util.ExceptionUtil.rethrow;

public class ExecutorServiceSubmitScriptToPartitionMessageTask
        extends AbstractPartitionMessageTask<ExecutorServiceSubmitScriptToPartitionCodec.RequestParameters> {

    public ExecutorServiceSubmitScriptToPartitionMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected Operation prepareOperation() {
        SecurityContext securityContext = clientEngine.getSecurityContext();
        Callable<Object> callableScript = new CallableScript(parameters.script);
        Data callableData;
        if (securityContext != null) {
            Subject subject = endpoint.getSubject();
            Callable callable = securityContext.createSecureCallable(subject, (Callable<? extends Object>) callableScript);
            callableData = serializationService.toData(callable);
        } else {
            callableData = serializationService.toData(callableScript);
        }
        return new CallableTaskOperation(parameters.name, parameters.uuid, callableData);
    }

    @Override
    protected ExecutorServiceSubmitScriptToPartitionCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return ExecutorServiceSubmitScriptToPartitionCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        Data data = serializationService.toData(response);
        return ExecutorServiceSubmitScriptToPartitionCodec.encodeResponse(data);
    }

    @Override
    public String getServiceName() {
        return DistributedExecutorService.SERVICE_NAME;
    }

    @Override
    public Permission getRequiredPermission() {
        return null;
    }

    @Override
    public String getDistributedObjectName() {
        return parameters.name;
    }

    @Override
    public String getMethodName() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    public static final class CallableScript implements Callable<Object>, DataSerializable, HazelcastInstanceAware {

        private HazelcastInstance hazelcastInstance;
        private String script;

        public CallableScript() {
        }

        public CallableScript(String script) {
            this.script = script;
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hazelcastInstance = hazelcastInstance;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeUTF(script);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            script = in.readUTF();
        }

        @Override
        public Object call() throws Exception {
            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
            Bindings bindings = scriptEngine.createBindings();
            bindings.put("instance", hazelcastInstance);
            try {
                return scriptEngine.eval(script, bindings);
            } catch (ScriptException e) {
                throw rethrow(e);
            }
        }
    }
}
