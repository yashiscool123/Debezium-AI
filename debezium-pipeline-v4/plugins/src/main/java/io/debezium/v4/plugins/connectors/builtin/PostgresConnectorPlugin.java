package io.debezium.v4.plugins.connectors.builtin;

import io.debezium.v4.core.spi.ConnectorPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PostgresConnectorPlugin implements ConnectorPlugin {

    @Override
    public String name() { return "postgresql"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String type() { return "source"; }
    @Override
    public String getConnectorClass() { return "io.debezium.connector.postgresql.PostgresConnector"; }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "connector.class", Map.of("type", "string", "default", getConnectorClass()),
                "database.hostname", Map.of("type", "string", "required", true),
                "database.port", Map.of("type", "integer", "default", 5432),
                "database.user", Map.of("type", "string", "required", true),
                "database.password", Map.of("type", "string", "required", true, "secret", true),
                "database.dbname", Map.of("type", "string", "required", true),
                "topic.prefix", Map.of("type", "string", "required", true),
                "plugin.name", Map.of("type", "string", "default", "pgoutput"),
                "slot.name", Map.of("type", "string", "default", "debezium"),
                "publication.name", Map.of("type", "string", "default", "dbz_publication")
            )
        );
    }

    @Override
    public List<String> getRequiredConfig() {
        return List.of("database.hostname", "database.user", "database.password", "database.dbname", "topic.prefix");
    }

    @Override
    public List<String> getOptionalConfig() {
        return List.of("database.port", "plugin.name", "slot.name", "publication.name",
            "schema.include.list", "schema.exclude.list", "table.include.list", "table.exclude.list",
            "column.include.list", "column.exclude.list", "decimal.handling.mode",
            "interval.handling.mode", "hstore.handling.mode", "include.unknown.datatypes",
            "snapshot.mode", "slot.max.retries", "slot.retry.delay.ms");
    }
}
