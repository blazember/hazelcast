package com.hazelcast.map.merge;

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.internal.util.ThreadLocalRandomProvider;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.hazelcast.test.HazelcastParametersRunnerFactory;
import com.hazelcast.test.SplitBrainTestSupport;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(HazelcastParametersRunnerFactory.class)
@Category({QuickTest.class, ParallelTest.class})
public class NoIndexLossAfterSplitBrainHealTest extends SplitBrainTestSupport {

    private static final int ENTRY_COUNT = 10000;
    private static final int[] BRAINS = new int[]{2, 3};
    private static final String MAP_NAME = "test";
    private static final Predicate VALUE_IS_NOT_EQUAL_X = Predicates.not(Predicates.equal("value", "X"));
    private static final Predicate VALUE_IS_ONE_OF_A_B_C_D = Predicates.in("value", "A", "B", "C", "D");
    private static final List<String> POSSIBLE_VALUES = Arrays.asList("A", "B", "C", "D");

    @Parameterized.Parameter
    public InMemoryFormat inMemoryFormat;

    @Parameterized.Parameters(name = "format:{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {InMemoryFormat.BINARY},
                {InMemoryFormat.OBJECT},
        });
    }

    @Override
    protected int[] brains() {
        return BRAINS;
    }

    @Override
    protected Config config() {
        return super.config().addMapConfig(new MapConfig()
                .setName(MAP_NAME).setInMemoryFormat(inMemoryFormat)
                .addMapIndexConfig(new MapIndexConfig()
                        .setAttribute("value").setOrdered(true)));
    }

    @Override
    protected void onBeforeSplitBrainCreated(HazelcastInstance[] instances) {
        for (long key = 0; key < ENTRY_COUNT; key++) {
            String randomValue = POSSIBLE_VALUES.get(ThreadLocalRandomProvider.get().nextInt(POSSIBLE_VALUES.size()));
            instances[0].getMap(MAP_NAME).put(key, new TestObject(key, randomValue));
        }

        for (HazelcastInstance instance : instances) {
            IMap testMap = instance.getMap(MAP_NAME);

            assertSizeEventually(ENTRY_COUNT, testMap);
            assertEquals(ENTRY_COUNT, testMap.keySet(VALUE_IS_NOT_EQUAL_X).size());
            assertEquals(ENTRY_COUNT, testMap.keySet(VALUE_IS_ONE_OF_A_B_C_D).size());
        }
    }

    @Override
    protected void onAfterSplitBrainHealed(HazelcastInstance[] instances) {
        for (HazelcastInstance instance : instances) {
            IMap testMap = instance.getMap(MAP_NAME);

            assertSizeEventually(ENTRY_COUNT, testMap);
            assertEquals(ENTRY_COUNT, testMap.keySet(VALUE_IS_NOT_EQUAL_X).size());
            assertEquals(ENTRY_COUNT, testMap.keySet(VALUE_IS_ONE_OF_A_B_C_D).size());
        }
    }

    private static class TestObject implements Serializable {

        private Long id;
        private String value;

        public TestObject() {
        }

        public TestObject(Long id, String value) {
            this.id = id;
            this.value = value;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}