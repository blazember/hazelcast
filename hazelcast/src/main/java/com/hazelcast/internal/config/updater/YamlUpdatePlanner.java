package com.hazelcast.internal.config.updater;

import com.hazelcast.internal.util.BiTuple;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.constructor.BaseConstructor;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.NodeType;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class YamlUpdatePlanner implements UpdatePlanner {
    private final List<UpdatePlanEntry> plan = new LinkedList<>();
    private final byte[] configBytes;

    public YamlUpdatePlanner(byte[] configBytes) {
        this.configBytes = configBytes;
    }

    @Override
    public List<UpdatePlanEntry> plan(List<ConfigUpdate> updates) {
        LoadSettings loadSettings = LoadSettings.builder()
                .setUseMarks(true)
                .build();
        HzLoad load = new HzLoad(loadSettings, updates);
        load.loadFromInputStream(new ByteArrayInputStream(configBytes));

        return plan;
    }


    private class HzLoad extends Load {
        private final BaseConstructor constructor;
        private final List<ConfigUpdate> updates;

        private HzLoad(LoadSettings settings, List<ConfigUpdate> updates) {
            this(settings, new StandardConstructor(settings), updates);
        }

        private HzLoad(LoadSettings settings, BaseConstructor constructor, List<ConfigUpdate> updates) {
            super(settings, constructor);
            this.constructor = constructor;
            this.updates = updates;
        }

        @Override
        protected Object loadOne(Composer composer) {
            Optional<Node> nodeOptional = composer.getSingleNode();
            nodeOptional.ifPresent(this::map);
            return constructor.constructSingleDocument(nodeOptional);
        }

        private void map(Node node) {
            map("", node, updates);
        }

        private void map(String path, Node node, List<ConfigUpdate> updates) {
            if (node.getNodeType() == NodeType.MAPPING) {
                MappingNode nodeMap = (MappingNode) node;

                Map<String, BiTuple<ScalarNode, Node>> children = new HashMap<>();
                for (NodeTuple nodeTuple : nodeMap.getValue()) {
                    ScalarNode keyNode = (ScalarNode) nodeTuple.getKeyNode();
                    Node valueNode = nodeTuple.getValueNode();
                    children.put(keyNode.getValue(), BiTuple.of(keyNode, valueNode));
                }

                boolean named = false;
//                boolean attribute = false;
//                boolean child = false;
                for (int i = 0; i < updates.size() && !named; i++) {
                    char nextChar = updates.get(i).searchPattern().charAt(path.length());
                    if (nextChar == ConfigUpdater.NAMED) {
                        named = true;
//                    } else if (nextChar ==ConfigUpdater.ATTRIBUTE) {
//                        named = true;
//                    } else if (nextChar ==ConfigUpdater.DIVIDER) {
//                        named = true;
                    }
                }


                for (Map.Entry<String, BiTuple<ScalarNode, Node>> entry : children.entrySet()) {
                    String key = entry.getKey();
                    String keyPath;
                    if (named) {
                        keyPath = path + ConfigUpdater.NAMED + key;
                    } else {
                        keyPath = path + ConfigUpdater.DIVIDER + key;
                    }

                    Node valueNode = entry.getValue().element2;

                    List<ConfigUpdate> filteredUpdates = new LinkedList<>();
                    for (ConfigUpdate update : updates) {
                        if (update.searchPattern().startsWith(keyPath)) {
                            filteredUpdates.add(update);
                        }
                    }

                    if (!filteredUpdates.isEmpty()) {
                        for (ConfigUpdate update : filteredUpdates) {
                            Optional<Mark> startMark = valueNode.getStartMark();
                            Optional<Mark> endMark = valueNode.getEndMark();
                            if (update.searchPattern().equals(keyPath) && startMark.isPresent() && endMark.isPresent()) {
                                int startPos = startMark.get().getIndex();
                                int endPos = endMark.get().getIndex();
                                plan.add(new UpdatePlanEntry(startPos, endPos, update));
                            }
                        }

                        if (valueNode.getNodeType() == NodeType.MAPPING) {
                            map(keyPath, valueNode, filteredUpdates);
                        }
                    }
                }
            }
        }
    }
}
