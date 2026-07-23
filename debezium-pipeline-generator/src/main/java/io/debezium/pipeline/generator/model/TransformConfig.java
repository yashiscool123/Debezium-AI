package io.debezium.pipeline.generator.model;

import java.util.Map;

public record TransformConfig(
    String name,
    String type,
    Map<String, String> config,
    String predicate
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String type;
        private Map<String, String> config;
        private String predicate;

        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder config(Map<String, String> config) { this.config = config; return this; }
        public Builder predicate(String predicate) { this.predicate = predicate; return this; }

        public TransformConfig build() {
            return new TransformConfig(name, type, config, predicate);
        }
    }

    public enum Type {
        EXTRACT_NEW_RECORD_STATE("io.debezium.transforms.ExtractNewRecordState"),
        FLATTEN("org.apache.kafka.connect.transforms.Flatten$Value"),
        MASK_FIELD("org.apache.kafka.connect.transforms.MaskField$Value"),
        EXTRACT_TOPIC("org.apache.kafka.connect.transforms.ExtractTopic$Value"),
        INSERT_FIELD("org.apache.kafka.connect.transforms.InsertField$Value"),
        REPLACE_FIELD("org.apache.kafka.connect.transforms.ReplaceField$Value"),
        SET_SCHEMA("org.apache.kafka.connect.transforms.SetSchemaMetadata$Value"),
        CAST("org.apache.kafka.connect.transforms.Cast$Value"),
        TIMESTAMP_CONVERTER("org.apache.kafka.connect.transforms.TimestampConverter$Value"),
        HOIST_FIELD("org.apache.kafka.connect.transforms.HoistField$Value"),
        REGEX_ROUTER("org.apache.kafka.connect.transforms.RegexRouter"),
        TIMESTAMP_ROUTER("org.apache.kafka.connect.transforms.TimestampRouter"),
        OUTBOX("io.debezium.transforms.outbox.EventRouter"),
        CONTENT_BASED_ROUTER("io.debezium.transforms.ContentBasedRouter"),
        CUSTOM("custom");

        private final String className;
        Type(String className) { this.className = className; }
        public String className() { return className; }
    }
}