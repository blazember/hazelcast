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

import com.hazelcast.util.collection.OAHashSet;
import com.hazelcast.util.function.Consumer;

import java.util.Arrays;

import static com.hazelcast.nio.Bits.INT_SIZE_IN_BYTES;
import static com.hazelcast.nio.Bits.LONG_SIZE_IN_BYTES;
import static com.hazelcast.util.JVMUtil.REFERENCE_COST_IN_BYTES;

/**
 * Default on-heap implementation of the {@link MerkleTreeStorage} interface.
 * <p>
 * This implementation stores the tree in an <code>int[]</code> and the
 * keys belong to leaves in {@link OAHashSet}s, one set for each leaf.
 *
 * @see ArrayMerkleTree
 */
// TODO make initialCapacity parameterized
class DefaultMerkleTreeStorage implements MerkleTreeStorage {
    private int[] tree;
    private int leafLevel;
    private OAHashSet[] leafKeys;
    private int leafLevelOrder;

    /**
     * Footprint holds the total memory footprint of the Merkle tree and
     * the leaf data blocks.
     * <p/>
     * Note that this field leverages a single-writer and a non-atomic
     * operation is executed on the field. See {@link #adjustFootprintWithLeafKeySetChange}
     */
    private volatile long footprint;

    DefaultMerkleTreeStorage(int depth) {
        final int nodes = MerkleTreeUtil.getNumberOfNodes(depth);
        this.tree = new int[nodes];

        this.leafLevel = depth - 1;
        this.leafLevelOrder = MerkleTreeUtil.getLeftMostNodeOrderOnLevel(depth - 1);

        final int leaves = MerkleTreeUtil.getNodesOnLevel(leafLevel);

        leafKeys = new OAHashSet[leaves];
        for (int i = 0; i < leaves; i++) {
            leafKeys[i] = new OAHashSet<Object>(1);
        }

        initializeFootprint();
    }

    @Override
    public int getNodeHash(int nodeOrder) {
        return tree[nodeOrder];
    }

    @Override
    public void setNodeHash(int nodeOrder, int hash) {
        tree[nodeOrder] = hash;
    }

    @Override
    public void addKeyToLeaf(int leafOrder, int keyHash, Object key) {
        int relativeLeafOrder = leafOrder - leafLevelOrder;
        OAHashSet<Object> leafKeySet = leafKeys[relativeLeafOrder];
        long leafKeysFootprintBefore = leafKeySet.footprint();
        leafKeySet.add(key, keyHash);

        adjustFootprintWithLeafKeySetChange(leafKeySet.footprint(), leafKeysFootprintBefore);
    }

    private void adjustFootprintWithLeafKeySetChange(long currentFootprint, long footprintBeforeUpdate) {
        long footprintDelta = currentFootprint - footprintBeforeUpdate;

        if (footprintDelta != 0) {
            //noinspection NonAtomicOperationOnVolatileField
            footprint += footprintDelta;
        }
    }

    @Override
    public void updateBranch(int leafOrder) {
        int nodeOrder = MerkleTreeUtil.getParentOrder(leafOrder);

        for (int level = leafLevel; level > 0; level--) {
            int leftChildOrder = MerkleTreeUtil.getLeftChildOrder(nodeOrder);
            int rightChildOrder = MerkleTreeUtil.getRightChildOrder(nodeOrder);

            int leftChildHash = getNodeHash(leftChildOrder);
            int rightChildHash = getNodeHash(rightChildOrder);

            int newNodeHash = MerkleTreeUtil.sumHash(leftChildHash, rightChildHash);
            setNodeHash(nodeOrder, newNodeHash);

            nodeOrder = MerkleTreeUtil.getParentOrder(nodeOrder);
        }

    }

    @Override
    public void removeKeyFromLeaf(int leafOrder, int keyHash, Object key) {
        int relativeLeafOrder = leafOrder - leafLevelOrder;
        OAHashSet<Object> leafKeySet = leafKeys[relativeLeafOrder];
        long leafKeysFootprintBefore = leafKeySet.footprint();
        leafKeySet.remove(key, keyHash);

        adjustFootprintWithLeafKeySetChange(leafKeySet.footprint(), leafKeysFootprintBefore);
    }

    @Override
    public void forEachKeyOfLeaf(int leafOrder, Consumer<Object> consumer) {
        int relativeLeafOrder = leafOrder - leafLevelOrder;
        for (Object key : leafKeys[relativeLeafOrder]) {
            consumer.accept(key);
        }
    }

    @Override
    public int getLeafKeyCount(int leafOrder) {
        int relativeLeafOrder = leafOrder - leafLevelOrder;
        return leafKeys[relativeLeafOrder].size();
    }

    @Override
    public void clear() {
        Arrays.fill(tree, 0);
        for (OAHashSet<Object> leafKeysSet : this.leafKeys) {
            leafKeysSet.clear();
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public long footprint() {
        return footprint;
    }

    @SuppressWarnings("checkstyle:trailingcomment")
    private void initializeFootprint() {
        long leafKeysSetsFootprint = 0;
        for (OAHashSet leafKeysSet : leafKeys) {
            leafKeysSetsFootprint += leafKeysSet.footprint();
        }

        footprint = leafKeysSetsFootprint
                + INT_SIZE_IN_BYTES * tree.length
                + REFERENCE_COST_IN_BYTES * leafKeys.length
                + REFERENCE_COST_IN_BYTES // reference to the tree
                + REFERENCE_COST_IN_BYTES // reference to leafKeys array
                + LONG_SIZE_IN_BYTES; // footprint
    }
}
