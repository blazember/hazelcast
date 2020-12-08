package com.hazelcast.internal.config.updater;

class ConfigReplaceUpdate extends ConfigUpdate {
    private final String modification;

    ConfigReplaceUpdate(String searchPattern, String modification) {
        super(searchPattern);
        this.modification = modification;
    }

    @Override
    public String modification() {
        return modification;
    }

    @Override
    public String toString() {
        return "ConfigReplaceUpdate{" +
                "searchPattern='" + searchPattern + '\'' +
                ", modification='" + modification + '\'' +
                '}';
    }
}
