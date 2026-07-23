package io.debezium.pipeline.generator.model;

import java.util.Map;

public record SchemaRegistryConfig(
    String type,
    String url,
    String schemaCompatibility,
    boolean autoRegisterSchemas,
    boolean useLatestVersion,
    Map<String, String> properties
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "apicurio";
        private String url;
        private String schemaCompatibility = "BACKWARD";
        private boolean autoRegisterSchemas = true;
        private boolean useLatestVersion = true;
        private Map<String, String> properties;

        public Builder type(String type) { this.type = type; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder schemaCompatibility(String schemaCompatibility) { this.schemaCompatibility = schemaCompatibility; return this; }
        public Builder autoRegisterSchemas(boolean autoRegisterSchemas) { this.autoRegisterSchemas = autoRegisterSchemas; return this; }
        public Builder useLatestVersion(boolean useLatestVersion) { this.useLatestVersion = useLatestVersion; return this; }
        public Builder properties(Map<String, String> properties) { this.properties = properties; return this; }

        public SchemaRegistryConfig build() {
            return new SchemaRegistryConfig(type, url, schemaCompatibility, autoRegisterSchemas, useLatestVersion, properties);
        }
    }

    public enum Type {
        APICURIO("apicurio"),
        CONFLUENT("confluent"),
        NONE("none");

        private final String value;
        Type(String value) { this.value = value; }
        public String value() { return value; }
    }
}