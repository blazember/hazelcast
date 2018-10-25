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

package com.hazelcast.wan.merkletree;

import com.hazelcast.util.function.Consumer;

/**
 * Defines the interface of a storage used by the {@link ArrayMerkleTree}
 */
interface MerkleTreeStorage {

    /**
     * Returns the hash of the provided node in the tree
     *
     * @param nodeOrder The order of the node
     * @return the hash of the node
     */
    int getNodeHash(int nodeOrder);

    /**
     * Sets the hash of the provided node to the provided hash
     *
     * @param nodeOrder The node
     * @param hash      The new hash of the node
     */
    void setNodeHash(int nodeOrder, int hash);

    /**
     * Adds a key to a leaf
     *
     * @param leafOrder The leaf
     * @param keyHash   The hash of the key
     * @param key       The key to add to the leaf
     */
    void addKeyToLeaf(int leafOrder, int keyHash, Object key);

    /**
     * Removes a key from a leaf
     *
     * @param leafOrder The leaf
     * @param keyHash   The hash of the key
     * @param key       The key to remove from the leaf
     */
    void removeKeyFromLeaf(int leafOrder, int keyHash, Object key);

    /**
     * Performs the given action for each key of the provided
     * until all elements have been processed or the action throws an
     * exception.
     *
     * @param leafOrder The leaf
     * @param consumer  The action which is called for each key
     */
    void forEachKeyOfLeaf(int leafOrder, Consumer<Object> consumer);

    /**
     * Returns the number of the keys under the provided leaf.
     *
     * @param leafOrder The leaf for which the key count is queried
     * @return the number of the keys under the specified node
     */
    int getLeafKeyCount(int leafOrder);

    /**
     * Synchronously calculates the hash for all the parent nodes of the
     * node referenced by {@code leafOrder} up to the root node.
     *
     * @param leafOrder The order of node
     */
    void updateBranch(int leafOrder);

    /**
     * Clears the storage
     */
    void clear();

    /**
     * Disposes the structures the storage using
     */
    void dispose();

    /**
     * Returns the memory footprint of the storage
     *
     * @return the memory footprint
     */
    long footprint();
}
