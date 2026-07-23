package io.debezium.pipeline.generator.model;

import java.util.Map;

public record TransformationStep(
    String name,
    String type,
    int order,
    Map<String, String> configuration,
    String condition
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String type;
        private int order;
        private Map<String, String> configuration;
        private String condition;

        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder order(int order) { this.order = order; return this; }
        public Builder configuration(Map<String, String> configuration) { this.configuration = configuration; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }

        public TransformationStep build() {
            return new TransformationStep(name, type, order, configuration, condition);
        }
    }
}