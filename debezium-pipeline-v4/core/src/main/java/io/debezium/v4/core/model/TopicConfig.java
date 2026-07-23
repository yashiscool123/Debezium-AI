package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record TopicConfig(
        String prefix,
        String namingStrategy,
        String regexPattern,
        String regexReplacement,
        int partitions,
        short replicationFactor,
        Map<String, String> properties) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prefix;
        private String namingStrategy = "database.schema.table";
        private String regexPattern;
        private String regexReplacement;
        private int partitions = 1;
        private short replicationFactor = 1;
        private Map<String, String> properties;

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder namingStrategy(String namingStrategy) {
            this.namingStrategy = namingStrategy;
            return this;
        }

        public Builder regexPattern(String regexPattern) {
            this.regexPattern = regexPattern;
            return this;
        }

        public Builder regexReplacement(String regexReplacement) {
            this.regexReplacement = regexReplacement;
            return this;
        }

        public Builder partitions(int partitions) {
            this.partitions = partitions;
            return this;
        }

        public Builder replicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public TopicConfig build() {
            return new TopicConfig(prefix, namingStrategy, regexPattern, regexReplacement, partitions, replicationFactor, properties);
        }
    }
}
