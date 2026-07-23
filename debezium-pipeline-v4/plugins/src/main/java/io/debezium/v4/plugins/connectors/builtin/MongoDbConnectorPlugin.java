package io.debezium.v4.plugins.connectors.builtin;

import io.debezium.v4.core.spi.ConnectorPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MongoDbConnectorPlugin implements ConnectorPlugin {

    @Override
    public String name() { return "mongodb"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String type() { return "source"; }
    @Override
    public String getConnectorClass() { return "io.debezium.connector.mongodb.MongoDbConnector"; }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "connector.class", Map.of("type", "string", "default", getConnectorClass()),
                "mongodb.hosts", Map.of("type", "string", "default", "localhost:27017", "required", true),
                "mongodb.user", Map.of("type", "string"),
                "mongodb.password", Map.of("type", "string", "secret", true),
                "topic.prefix", Map.of("type", "string", "required", true),
                "collection.include.list", Map.of("type", "string")
            )
        );
    }

    @Override
    public List<String> getRequiredConfig() {
        return List.of("mongodb.hosts", "topic.prefix");
    }

    @Override
    public List<String> getOptionalConfig() {
        return List.of("mongodb.user", "mongodb.password", "mongodb.authsource", "collection.include.list",
            "collection.exclude.list", "snapshot.mode", "capture.mode", "tombstones.on.delete",
            "field.renames", "field.exclude.list", "sanitize.field.names");
    }
}
