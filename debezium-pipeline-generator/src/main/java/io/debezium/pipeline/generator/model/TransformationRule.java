package io.debezium.pipeline.generator.model;

import java.util.Map;

public record TransformationRule(
    String type,
    String expression,
    String smtClass,
    Map<String, String> parameters,
    String description
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String expression;
        private String smtClass;
        private Map<String, String> parameters;
        private String description;

        public Builder type(String type) { this.type = type; return this; }
        public Builder expression(String expression) { this.expression = expression; return this; }
        public Builder smtClass(String smtClass) { this.smtClass = smtClass; return this; }
        public Builder parameters(Map<String, String> parameters) { this.parameters = parameters; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public TransformationRule build() {
            return new TransformationRule(type, expression, smtClass, parameters, description);
        }
    }

    public enum Type {
        CAST("cast"),
        RENAME("rename"),
        FLATTEN("flatten"),
        EXTRACT_FIELD("extract_field"),
        MASK("mask"),
        FILTER("filter"),
        ROUTE("route"),
        CUSTOM("custom"),
        KSQL("ksql"),
        FLINK_SQL("flink_sql");

        private final String value;
        Type(String value) { this.value = value; }
        public String value() { return value; }
    }
}