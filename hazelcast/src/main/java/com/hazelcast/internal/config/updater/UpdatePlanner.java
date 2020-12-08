package com.hazelcast.internal.config.updater;

import java.util.List;

interface UpdatePlanner {
    List<UpdatePlanEntry> plan(List<ConfigUpdate> updates);
}
