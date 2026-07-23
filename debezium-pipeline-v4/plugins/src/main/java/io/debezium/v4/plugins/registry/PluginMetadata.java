package io.debezium.v4.plugins.registry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PluginMetadata(
    String name,
    String version,
    String description,
    String vendor,
    PluginType type,
    List<String> dependencies,
    Map<String, String> attributes,
    Instant loadedAt
) {
    public PluginMetadata(String name, String version, String description, PluginType type) {
        this(name, version, description, "Debezium Community", type, List.of(), Map.of(), Instant.now());
    }

    public enum PluginType {
        CONNECTOR, TRANSFORMER, DEPLOYMENT, PREDICATE, CONVERTER, SINK, SOURCE
    }
}
