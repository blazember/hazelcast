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

package com.hazelcast.map;

import com.hazelcast.internal.serialization.impl.HeapData;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.map.impl.VictimCache;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.serialization.SerializationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static java.io.File.separator;

// TODO use memory mapped files instead to give the control over flushing to the OS?
// TODO implement eviction
// TODO limit disk usage
public class DefaultVictimCache implements VictimCache {
    public static final String KEY = "key";
    public static final String VALUE = "value";

    private final boolean enabled;

    private File victimCacheRoot;
    private ILogger logger;
    private SerializationService serializationService;

    public DefaultVictimCache(String mapName, SerializationService serializationService,
                              boolean enabled) {
        this.serializationService = serializationService;
        this.enabled = enabled;
        if (enabled) {
            this.logger = Logger.getLogger(DefaultVictimCache.class);

            String tmpDir = System.getProperty("java.io.tmpdir");
            this.victimCacheRoot = new File(tmpDir + separator + "hz_victim_cache" + separator + mapName);

            // ensuring no garbage is there
            deleteDirectory(this.victimCacheRoot);
            this.victimCacheRoot.mkdirs();

            if (logger.isFineEnabled()) {
                logger.fine("Created victim cache for map " + mapName + " locating at " + victimCacheRoot);
            }
        }
    }

    @Override
    public void cache(int partitionId, Data key, Data value) {
        if (!enabled) {
            return;
        }

        // simulate not caching everything
        if (partitionId % 10 == 0) {
            return;
        }

        File entryRootPath = getEntryRootPath(partitionId, key);
        entryRootPath.mkdirs();
        try {
            File keyPath = new File(entryRootPath, KEY);
            File valuePath = new File(entryRootPath, VALUE);

            FileOutputStream keyFos = new FileOutputStream(keyPath);
            try {
                keyFos.write(key.toByteArray());
            } finally {
                keyFos.close();
            }

            FileOutputStream valueFos = new FileOutputStream(valuePath);
            try {
                valueFos.write(value.toByteArray());
            } finally {
                valueFos.close();
            }
        } catch (Exception ex) {
            logger.warning("Placing entry in the victim cache has failed", ex);
            deleteDirectory(entryRootPath);
        }
    }

    @Override
    public Object load(int partitionId, Data key) {
        if (!enabled) {
            return null;
        }

        File entryRootPath = getEntryRootPath(partitionId, key);

        if (!entryRootPath.exists()) {
            return null;
        }

        File keyPath = new File(entryRootPath, KEY);
        if (!keyPath.exists()) {
            deleteDirectory(entryRootPath);
            return null;
        }

        File valuePath = new File(entryRootPath, VALUE);
        if (!valuePath.exists()) {
            deleteDirectory(entryRootPath);
            return null;
        }

        if (!compareKey(key, keyPath)) {
            return null;
        }

        byte[] valueBytes = null;
        FileInputStream valueFis = null;
        try {
            valueFis = new FileInputStream(valuePath);
            int valueSize = (int) valuePath.length();
            valueBytes = new byte[valueSize];
            int read = valueFis.read(valueBytes);
            if (read != valueSize) {
                return null;
            }
        } catch (Exception ex) {
            logger.warning("Reading key from the victim cache has failed", ex);
        } finally {
            if (valueFis != null) {
                try {
                    valueFis.close();
                } catch (IOException e) {
                    // so what?
                }
            }
        }

        // we loaded the entry, now it will be in the record store, we can remove it from the victim cache
        deleteDirectory(entryRootPath);

        return new HeapData(valueBytes);
    }

    private boolean compareKey(Data key, File valuePath) {
        FileInputStream keyFis = null;
        try {
            keyFis = new FileInputStream(valuePath);
            int keySize = (int) valuePath.length();
            byte[] keyBytes = new byte[keySize];
            int read = keyFis.read(keyBytes);
            if (keySize != read || !Arrays.equals(key.toByteArray(), keyBytes)) {
                return false;
            }
        } catch (Exception ex) {
            logger.warning("Reading key from the victim cache has failed", ex);
        } finally {
            if (keyFis != null) {
                try {
                    keyFis.close();
                } catch (IOException e) {
                    // so what?
                }
            }
        }
        return true;
    }

    @Override
    public void remove(int partitionId, Data key) {
        if (!enabled) {
            return;
        }

        deleteDirectory(getEntryRootPath(partitionId, key));
    }

    @Override
    public void clear(int partitionId) {
        if (!enabled) {
            return;
        }

        deleteDirectory(getPartitionRootPath(partitionId));
    }

    private boolean deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directory.delete();
    }

    private File getPartitionRootPath(int partitionId) {
        return new File(victimCacheRoot, Integer.toString(partitionId));
    }

    private File getEntryRootPath(int partitionId, Data key) {
        return new File(victimCacheRoot, Integer.toString(partitionId) + separator + Long.toHexString(key.hash64()));
    }
}
