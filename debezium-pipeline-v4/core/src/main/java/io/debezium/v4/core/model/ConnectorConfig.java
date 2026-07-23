package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record ConnectorConfig(
        String connectorClass,
        String name,
        Map<String, String> config,
        Map<String, String> secrets) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String connectorClass;
        private String name;
        private Map<String, String> config;
        private Map<String, String> secrets;

        public Builder connectorClass(String connectorClass) {
            this.connectorClass = connectorClass;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder config(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public Builder secrets(Map<String, String> secrets) {
            this.secrets = secrets;
            return this;
        }

        public ConnectorConfig build() {
            return new ConnectorConfig(connectorClass, name, config, secrets);
        }
    }
}
