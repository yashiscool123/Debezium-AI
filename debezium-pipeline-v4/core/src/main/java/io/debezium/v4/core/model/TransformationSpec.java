package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record TransformationSpec(
        String name,
        String type,
        int order,
        Map<String, String> configuration,
        String predicate,
        String description) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String type;
        private int order;
        private Map<String, String> configuration;
        private String predicate;
        private String description;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder configuration(Map<String, String> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder predicate(String predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public TransformationSpec build() {
            return new TransformationSpec(name, type, order, configuration, predicate, description);
        }
    }
}
