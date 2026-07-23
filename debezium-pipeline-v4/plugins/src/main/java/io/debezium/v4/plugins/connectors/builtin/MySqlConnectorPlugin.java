package io.debezium.v4.plugins.connectors.builtin;

import io.debezium.v4.core.spi.ConnectorPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MySqlConnectorPlugin implements ConnectorPlugin {

    @Override
    public String name() { return "mysql"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String type() { return "source"; }
    @Override
    public String getConnectorClass() { return "io.debezium.connector.mysql.MySqlConnector"; }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "connector.class", Map.of("type", "string", "default", getConnectorClass()),
                "database.hostname", Map.of("type", "string", "required", true),
                "database.port", Map.of("type", "integer", "default", 3306),
                "database.user", Map.of("type", "string", "required", true),
                "database.password", Map.of("type", "string", "required", true, "secret", true),
                "database.server.name", Map.of("type", "string", "required", true),
                "database.include.list", Map.of("type", "string"),
                "table.include.list", Map.of("type", "string"),
                "snapshot.mode", Map.of("type", "string", "default", "initial"),
                "topic.prefix", Map.of("type", "string", "required", true)
            )
        );
    }

    @Override
    public List<String> getRequiredConfig() {
        return List.of("database.hostname", "database.user", "database.password", "topic.prefix");
    }

    @Override
    public List<String> getOptionalConfig() {
        return List.of("database.port", "database.include.list", "table.include.list", "snapshot.mode",
            "database.server.id", "database.history.kafka.bootstrap.servers", "database.history.kafka.topic",
            "decimal.handling.mode", "time.precision.mode", "include.schema.changes", "tombstones.on.delete",
            "message.key.columns", "column.propagate.source.type", "datatype.propagate.source.type");
    }
}
