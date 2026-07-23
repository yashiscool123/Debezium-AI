package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record SinkConfig(
        String type,
        Map<String, String> properties) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "kafka";
        private Map<String, String> properties;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public SinkConfig build() {
            return new SinkConfig(type, properties);
        }
    }
}
