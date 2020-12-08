package com.hazelcast.internal.config.updater;

import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.internal.config.ConfigSections;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ConfigUpdaterTest {

    @Test
    public void test() throws IOException {
        ConfigUpdater updater = new ConfigUpdater("/home/blaze/hazelcast.yaml", ConfigSections.HAZELCAST.getName())
                .newUpdate()
                .map("map-to-update")
                .path("in-memory-format")
                .updateText(InMemoryFormat.NATIVE)

//                .newUpdate()
//                .map("map-to-update")
//                .path("merkle-tree")
//                .updateAttribute("enabled", "true")

                .newUpdate()
                .map("map-to-update")
                .path("merkle-tree")
                .path("depth")
                .updateText("12");

        updater.update();
    }
}
