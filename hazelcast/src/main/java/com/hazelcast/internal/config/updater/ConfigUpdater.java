package com.hazelcast.internal.config.updater;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ConfigUpdater {
    private static final ILogger LOGGER = Logger.getLogger(ConfigUpdater.class);

    static final char DIVIDER = '/';
    static final char NAMED = '#';
    static final char ATTRIBUTE = ':';

    private final File sourceFile;
    private final String rootTag;
    private final List<ConfigUpdate> updates = new LinkedList<>();

    public ConfigUpdater(String sourceFilePath, String rootTag) {
        this.sourceFile = new File(sourceFilePath);
        this.rootTag = rootTag;
    }

    public ConfigUpdateBuilder newUpdate() {
        return new ConfigUpdateBuilder(this, rootTag);
    }

    public void update() throws IOException {
        if (LOGGER.isFineEnabled()) {
            LOGGER.fine("Updating config file '" + sourceFile.getAbsolutePath() + "'");
        }

        FileInputStream fis = new FileInputStream(sourceFile);
        final byte[] configBytes = new byte[(int) sourceFile.length()];
        int read = fis.read(configBytes);

        YamlUpdatePlanner planner = new YamlUpdatePlanner(configBytes);
        List<UpdatePlanEntry> plan = planner.plan(updates);

        Collections.sort(plan, (o1, o2) -> o1.startPos < o2.startPos ? -1 : 1);

        if (LOGGER.isFineEnabled()) {
            StringBuilder sb = new StringBuilder("Config update plan:\n");
            for (UpdatePlanEntry planEntry : plan) {
                sb.append('\t').append("- ").append(planEntry).append('\n');
            }

            LOGGER.fine(sb.toString());
        }

        FileOutputStream fos = new FileOutputStream("/home/blaze/hazelcast-updated.yaml");
        int pos = 0;
        for (UpdatePlanEntry planEntry : plan) {
            fos.write(configBytes, pos, planEntry.startPos - pos);
            pos = planEntry.endPos;
            fos.write(planEntry.update.modification().getBytes());
        }
        fos.write(configBytes, pos, configBytes.length - pos);
        fos.close();
    }

    void addUpdate(ConfigUpdate update) {
        updates.add(update);
    }
}
