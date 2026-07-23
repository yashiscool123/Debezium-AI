package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record TargetDatabaseConfig(
    String type,
    String host,
    int port,
    String username,
    String password,
    String databaseName,
    String schemaName,
    Map<String, String> additionalProperties
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String host;
        private int port;
        private String username;
        private String password;
        private String databaseName;
        private String schemaName;
        private Map<String, String> additionalProperties;

        public Builder type(String type) { this.type = type; return this; }
        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder databaseName(String databaseName) { this.databaseName = databaseName; return this; }
        public Builder schemaName(String schemaName) { this.schemaName = schemaName; return this; }
        public Builder additionalProperties(Map<String, String> additionalProperties) { this.additionalProperties = additionalProperties; return this; }

        public TargetDatabaseConfig build() {
            return new TargetDatabaseConfig(type, host, port, username, password, databaseName, schemaName, additionalProperties);
        }
    }
}