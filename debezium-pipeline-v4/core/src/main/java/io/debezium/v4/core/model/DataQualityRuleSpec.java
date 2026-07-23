package io.debezium.v4.core.model;

import java.util.Map;

public record DataQualityRuleSpec(
        String name,
        String description,
        boolean enabled,
        String scope,
        String column,
        String ruleType,
        Map<String, String> configuration,
        String severity,
        String predicate,
        String expectedValue,
        Double threshold) {

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name;
        private String description;
        private boolean enabled = true;
        private String scope = "ROW";
        private String column;
        private String ruleType = "NOT_NULL";
        private Map<String, String> configuration = Map.of();
        private String severity = "WARN";
        private String predicate;
        private String expectedValue;
        private Double threshold;

        public Builder name(String v) { this.name = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder enabled(boolean v) { this.enabled = v; return this; }
        public Builder scope(String v) { this.scope = v; return this; }
        public Builder column(String v) { this.column = v; return this; }
        public Builder ruleType(String v) { this.ruleType = v; return this; }
        public Builder configuration(Map<String, String> v) { this.configuration = v; return this; }
        public Builder severity(String v) { this.severity = v; return this; }
        public Builder predicate(String v) { this.predicate = v; return this; }
        public Builder expectedValue(String v) { this.expectedValue = v; return this; }
        public Builder threshold(Double v) { this.threshold = v; return this; }
        public DataQualityRuleSpec build() { return new DataQualityRuleSpec(name, description, enabled, scope, column, ruleType, configuration, severity, predicate, expectedValue, threshold); }
    }
}
