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

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import com.hazelcast.util.function.Consumer;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class ArrayMerkleTreeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDepthBelowMinDepthThrows() {
        new ArrayMerkleTree(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDepthAboveMaxDepthThrows() {
        new ArrayMerkleTree(28);
    }

    @Test
    public void testDepth() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        assertEquals(3, merkleTree.depth());
    }

    @Test
    public void testFootprint() {
        MerkleTree merkleTree1 = new ArrayMerkleTree(3, 10);
        MerkleTree merkleTree2 = new ArrayMerkleTree(3, 100);

        assertTrue(merkleTree2.footprint() > merkleTree1.footprint());
    }

    @Test
    public void testUpdateAdd() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        merkleTree.updateAdd(1, 1);
        merkleTree.updateAdd(3, 3);

        int expectedHash = 0;
        expectedHash = MerkleTreeUtil.addHash(expectedHash, 1);
        expectedHash = MerkleTreeUtil.addHash(expectedHash, 3);

        int nodeHash = merkleTree.getNodeHash(6);

        assertEquals(expectedHash, nodeHash);
    }

    @Test
    public void testUpdateAddUpdateBranch() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        // keys under leaf 4
        int keyOf1 = keyInLeaf(4);
        int keyOf2 = keyInLeaf(4, keyOf1);
        int keyOf4 = keyInLeaf(4, keyOf2);

        // keys under leaf 5
        int keyOfMinus1 = keyInLeaf(5);
        int keyOfMinus2 = keyInLeaf(5, keyOfMinus1);
        int keyOfMinus3 = keyInLeaf(5, keyOfMinus2);

        merkleTree.updateAdd(keyOf4, 4);
        merkleTree.updateAdd(keyOf2, 2);
        merkleTree.updateAdd(keyOf1, 1);
        merkleTree.updateAdd(0, 0);
        merkleTree.updateAdd(keyOfMinus1, -1);
        merkleTree.updateAdd(keyOfMinus2, -2);
        merkleTree.updateAdd(keyOfMinus3, -3);

        int expectedHashNode5 = 0;
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -1);
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -2);
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -3);

        int expectedHashNode4 = 0;
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 1);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 2);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 4);

        int expectedHashNode1 = MerkleTreeUtil.sumHash(0, expectedHashNode4);
        int expectedHashNode2 = MerkleTreeUtil.sumHash(0, expectedHashNode5);
        int expectedHashNode0 = MerkleTreeUtil.sumHash(expectedHashNode1, expectedHashNode2);

        assertEquals(expectedHashNode0, merkleTree.getNodeHash(0));
        assertEquals(expectedHashNode1, merkleTree.getNodeHash(1));
        assertEquals(expectedHashNode2, merkleTree.getNodeHash(2));
        assertEquals(0, merkleTree.getNodeHash(3));
        assertEquals(expectedHashNode4, merkleTree.getNodeHash(4));
        assertEquals(expectedHashNode5, merkleTree.getNodeHash(5));
        assertEquals(0, merkleTree.getNodeHash(6));
    }

    @Test
    public void testUpdateReplaceUpdateBranch() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        // keys under leaf 4
        int keyOf1 = keyInLeaf(4);
        int keyOf2 = keyInLeaf(4, keyOf1);
        int keyOf4 = keyInLeaf(4, keyOf2);

        // keys under leaf 5
        int keyOfMinus1 = keyInLeaf(5);
        int keyOfMinus2 = keyInLeaf(5, keyOfMinus1);
        int keyOfMinus3 = keyInLeaf(5, keyOfMinus2);

        merkleTree.updateAdd(keyOf4, 4);
        merkleTree.updateAdd(keyOf2, 2);
        merkleTree.updateAdd(keyOf1, 1);
        merkleTree.updateAdd(0, 0);
        merkleTree.updateAdd(keyOfMinus1, -1);
        merkleTree.updateAdd(keyOfMinus2, -2);
        merkleTree.updateAdd(keyOfMinus3, -3);

        merkleTree.updateReplace(keyOfMinus3, -3, -5);

        int expectedHashNode5 = 0;
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -1);
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -2);
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -5);

        int expectedHashNode4 = 0;
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 1);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 2);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 4);

        int expectedHashNode1 = MerkleTreeUtil.sumHash(0, expectedHashNode4);
        int expectedHashNode2 = MerkleTreeUtil.sumHash(0, expectedHashNode5);
        int expectedHashNode0 = MerkleTreeUtil.sumHash(expectedHashNode1, expectedHashNode2);

        assertEquals(expectedHashNode0, merkleTree.getNodeHash(0));
        assertEquals(expectedHashNode1, merkleTree.getNodeHash(1));
        assertEquals(expectedHashNode2, merkleTree.getNodeHash(2));
        assertEquals(0, merkleTree.getNodeHash(3));
        assertEquals(expectedHashNode4, merkleTree.getNodeHash(4));
        assertEquals(expectedHashNode5, merkleTree.getNodeHash(5));
        assertEquals(0, merkleTree.getNodeHash(6));
    }

    @Test
    public void testUpdateRemoveUpdateBranch() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        // keys under leaf 4
        int keyOf1 = keyInLeaf(4);
        int keyOf2 = keyInLeaf(4, keyOf1);
        int keyOf4 = keyInLeaf(4, keyOf2);

        // keys under leaf 5
        int keyOfMinus1 = keyInLeaf(5);
        int keyOfMinus2 = keyInLeaf(5, keyOfMinus1);
        int keyOfMinus3 = keyInLeaf(5, keyOfMinus2);

        merkleTree.updateAdd(keyOf4, 4);
        merkleTree.updateAdd(keyOf2, 2);
        merkleTree.updateAdd(keyOf1, 1);
        merkleTree.updateAdd(0, 0);
        merkleTree.updateAdd(keyOfMinus1, -1);
        merkleTree.updateAdd(keyOfMinus2, -2);
        merkleTree.updateAdd(keyOfMinus3, -3);

        merkleTree.updateRemove(keyOfMinus3, -3);

        int expectedHashNode5 = 0;
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -1);
        expectedHashNode5 = MerkleTreeUtil.addHash(expectedHashNode5, -2);

        int expectedHashNode4 = 0;
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 1);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 2);
        expectedHashNode4 = MerkleTreeUtil.addHash(expectedHashNode4, 4);

        int expectedHashNode1 = MerkleTreeUtil.sumHash(0, expectedHashNode4);
        int expectedHashNode2 = MerkleTreeUtil.sumHash(0, expectedHashNode5);
        int expectedHashNode0 = MerkleTreeUtil.sumHash(expectedHashNode1, expectedHashNode2);

        assertEquals(expectedHashNode0, merkleTree.getNodeHash(0));
        assertEquals(expectedHashNode1, merkleTree.getNodeHash(1));
        assertEquals(expectedHashNode2, merkleTree.getNodeHash(2));
        assertEquals(0, merkleTree.getNodeHash(3));
        assertEquals(expectedHashNode4, merkleTree.getNodeHash(4));
        assertEquals(expectedHashNode5, merkleTree.getNodeHash(5));
        assertEquals(0, merkleTree.getNodeHash(6));
    }

    @Test
    public void testUpdateReplace() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        merkleTree.updateAdd(1, 1);
        merkleTree.updateAdd(3, 3);
        merkleTree.updateReplace(3, 3, 4);

        int expectedHash = 0;
        expectedHash = MerkleTreeUtil.addHash(expectedHash, 1);
        expectedHash = MerkleTreeUtil.addHash(expectedHash, 4);

        int nodeHash = merkleTree.getNodeHash(6);

        assertEquals(expectedHash, nodeHash);
    }

    @Test
    public void testUpdateRemove() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        merkleTree.updateAdd(1, 1);
        merkleTree.updateAdd(3, 3);
        merkleTree.updateRemove(3, 3);

        int expectedHash = 0;
        expectedHash = MerkleTreeUtil.addHash(expectedHash, 1);

        int nodeHash = merkleTree.getNodeHash(6);

        assertEquals(expectedHash, nodeHash);
    }

    @Test
    public void forEachKeyOfLeaf() {
        MerkleTree merkleTree = new ArrayMerkleTree(3);

        int keyOf1 = keyInLeaf(3);
        int keyOf2 = keyInLeaf(4);
        int keyOf3 = keyInLeaf(5);
        int keyOf4 = keyInLeaf(6);

        merkleTree.updateAdd(keyOf1, 1); // leaf 3
        merkleTree.updateAdd(keyOf2, 2); // leaf 4
        merkleTree.updateAdd(keyOf3, 3); // leaf 5
        merkleTree.updateAdd(keyOf4, 4); // leaf 6

        // level 0
        verifyKeysUnderNode(merkleTree, 0, keyOf1, keyOf2, keyOf3, keyOf4);

        // level 1
        verifyKeysUnderNode(merkleTree, 1, keyOf1, keyOf2);
        verifyKeysUnderNode(merkleTree, 2, keyOf3, keyOf4);

        // level 2 (leaves)
        verifyKeysUnderNode(merkleTree, 3, keyOf1);
        verifyKeysUnderNode(merkleTree, 4, keyOf2);
        verifyKeysUnderNode(merkleTree, 5, keyOf3);
        verifyKeysUnderNode(merkleTree, 6, keyOf4);
    }

    @Test
    public void testTreeDepthsDontImpactNodeHashes() {
        MerkleTree merkleTreeShallow = new ArrayMerkleTree(2);
        MerkleTree merkleTreeDeep = new ArrayMerkleTree(4);

        int keyOf1 = keyInLeaf(7);
        int keyOf2 = keyInLeaf(8);
        int keyOf3 = keyInLeaf(9);
        int keyOf4 = keyInLeaf(10);
        int keyOf5 = keyInLeaf(11);
        int keyOf6 = keyInLeaf(12);
        int keyOf7 = keyInLeaf(13);
        int keyOf8 = keyInLeaf(14);

        merkleTreeShallow.updateAdd(keyOf1, 1); // leaf 1
        merkleTreeShallow.updateAdd(keyOf2, 2); // leaf 1
        merkleTreeShallow.updateAdd(keyOf3, 3); // leaf 1
        merkleTreeShallow.updateAdd(keyOf4, 4); // leaf 1
        merkleTreeShallow.updateAdd(keyOf5, 5); // leaf 2
        merkleTreeShallow.updateAdd(keyOf6, 6); // leaf 2
        merkleTreeShallow.updateAdd(keyOf7, 7); // leaf 2
        merkleTreeShallow.updateAdd(keyOf8, 8); // leaf 2

        merkleTreeDeep.updateAdd(keyOf1, 1); // leaf 7
        merkleTreeDeep.updateAdd(keyOf2, 2); // leaf 8
        merkleTreeDeep.updateAdd(keyOf3, 3); // leaf 9
        merkleTreeDeep.updateAdd(keyOf4, 4); // leaf 10
        merkleTreeDeep.updateAdd(keyOf5, 5); // leaf 11
        merkleTreeDeep.updateAdd(keyOf6, 6); // leaf 12
        merkleTreeDeep.updateAdd(keyOf7, 7); // leaf 13
        merkleTreeDeep.updateAdd(keyOf8, 8); // leaf 14

        verifyTreesAreSameOnCommonLevels(merkleTreeShallow, merkleTreeDeep);

        merkleTreeShallow.updateReplace(keyOf4, 4, 42);
        merkleTreeDeep.updateReplace(keyOf4, 4, 42);

        verifyTreesAreSameOnCommonLevels(merkleTreeShallow, merkleTreeDeep);

        merkleTreeShallow.updateRemove(keyOf4, 42);
        merkleTreeDeep.updateRemove(keyOf4, 42);

        verifyTreesAreSameOnCommonLevels(merkleTreeShallow, merkleTreeDeep);
    }

    @Test
    public void testClear() {
        MerkleTree merkleTree = new ArrayMerkleTree(4);

        merkleTree.updateAdd(keyInLeaf(7), 1); // leaf 7
        merkleTree.updateAdd(keyInLeaf(8), 2); // leaf 8
        merkleTree.updateAdd(keyInLeaf(9), 3); // leaf 9
        merkleTree.updateAdd(keyInLeaf(10), 4); // leaf 10
        merkleTree.updateAdd(keyInLeaf(11), 5); // leaf 11
        merkleTree.updateAdd(keyInLeaf(12), 6); // leaf 12
        merkleTree.updateAdd(keyInLeaf(13), 7); // leaf 13
        merkleTree.updateAdd(keyInLeaf(14), 8); // leaf 14

        assertNotEquals(0, merkleTree.getNodeHash(0));

        merkleTree.clear();

        for (int nodeOrder = 0; nodeOrder < MerkleTreeUtil.getNumberOfNodes(merkleTree.depth()); nodeOrder++) {
            assertEquals(0, merkleTree.getNodeHash(nodeOrder));
        }

        merkleTree.forEachKeyOfNode(0, new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                fail("Consumer is not expected to be invoked. Leaf keys should be empty.");
            }
        });
    }

    private void verifyTreesAreSameOnCommonLevels(MerkleTree merkleTreeShallow, MerkleTree merkleTreeDeep) {
        for (int i = 0; i < MerkleTreeUtil.getNumberOfNodes(merkleTreeShallow.depth()); i++) {
            assertEquals(merkleTreeShallow.getNodeHash(i), merkleTreeDeep.getNodeHash(i));
        }
    }

    private void verifyKeysUnderNode(MerkleTree merkleTree, int nodeOrder, Integer... expectedKeys) {
        KeyCatcherConsumer consumerNode = new KeyCatcherConsumer();
        merkleTree.forEachKeyOfNode(nodeOrder, consumerNode);
        assertEquals(expectedKeys.length, consumerNode.keys.size());
        assertTrue(consumerNode.keys.containsAll(asList(expectedKeys)));
    }

    private static int keyInLeaf(int nodeOrder) {
        return keyInLeaf(nodeOrder, -1);
    }

    private static int keyInLeaf(int nodeOrder, int above) {
        int nodeRangeLow = MerkleTreeUtil.getNodeRangeLow(nodeOrder);
        int nodeRangeHigh = MerkleTreeUtil.getNodeRangeHigh(nodeOrder);
        return findKeyForRange(nodeRangeLow, nodeRangeHigh, above + 1);
    }

    private static int findKeyForRange(int rangeLow, int rangeHigh, int findFrom) {
        int i = findFrom;
        for (;;) {
            int result = i * ArrayMerkleTree.HUGE_PRIME;
            if (rangeLow <= result && rangeHigh >= result) {
                return i;
            }
            i++;
        }
    }

    private static class KeyCatcherConsumer implements Consumer<Object> {
        private Set<Object> keys = new HashSet<Object>();

        @Override
        public void accept(Object key) {
            keys.add(key);
        }
    }
}
