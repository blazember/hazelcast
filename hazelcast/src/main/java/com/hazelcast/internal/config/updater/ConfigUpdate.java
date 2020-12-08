package com.hazelcast.internal.config.updater;

abstract class ConfigUpdate {
    protected final String searchPattern;

    ConfigUpdate(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    String searchPattern() {
        return searchPattern;
    }

    abstract String modification();

    @Override
    public String toString() {
        return "ConfigUpdate{" +
                "searchPattern='" + searchPattern + '\'' +
                '}';
    }
}
