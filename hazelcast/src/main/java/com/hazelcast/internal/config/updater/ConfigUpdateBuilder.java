package com.hazelcast.internal.config.updater;

import java.util.LinkedList;
import java.util.List;

public class ConfigUpdateBuilder {

    private final ConfigUpdater configUpdater;
    private final StringBuilder pathBuilder;
    private final List<ConfigUpdate> updates = new LinkedList<>();

    ConfigUpdateBuilder(ConfigUpdater configUpdater, String root) {
        this.configUpdater = configUpdater;
        pathBuilder = new StringBuilder().append(ConfigUpdater.DIVIDER).append(root);
    }

    ConfigUpdateBuilder path(String path) {
        pathBuilder.append(ConfigUpdater.DIVIDER).append(path);
        return this;
    }

    ConfigUpdateBuilder namedPath(String path, String name) {
        pathBuilder.append(ConfigUpdater.DIVIDER)
                .append(path)
                .append(ConfigUpdater.NAMED)
                .append(name);
        return this;
    }

    ConfigUpdateBuilder map(String mapName) {
        return namedPath("map", mapName);
    }

    ConfigUpdater updateAttribute(String attrName, String value) {
        pathBuilder.append(ConfigUpdater.ATTRIBUTE).append(attrName);
        configUpdater.addUpdate(new ConfigReplaceUpdate(pathBuilder.toString().toLowerCase(), value));
        return configUpdater;
    }

    ConfigUpdater updateText(String value) {
        configUpdater.addUpdate(new ConfigReplaceUpdate(pathBuilder.toString().toLowerCase(), value));
        return configUpdater;
    }

    <T extends Enum<?>> ConfigUpdater updateText(T value) {
        configUpdater.addUpdate(new ConfigReplaceUpdate(pathBuilder.toString().toLowerCase(), value.toString()));
        return configUpdater;
    }
}
