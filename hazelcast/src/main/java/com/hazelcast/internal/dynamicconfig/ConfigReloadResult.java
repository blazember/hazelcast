/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.internal.dynamicconfig;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class ConfigReloadResult {
    private List<Object> notReloadableChanges = new LinkedList<>();
    private List<Object> reloadableChanges = new LinkedList<>();

    void addNotReloadableChange(Object config) {
        notReloadableChanges.add(config);
    }

    void addReloadableChange(Object config) {
        reloadableChanges.add(config);
    }

    public List<Object> getNotReloadableChanges() {
        return unmodifiableList(notReloadableChanges);
    }

    public List<Object> getReloadableChanges() {
        return unmodifiableList(reloadableChanges);
    }
}
