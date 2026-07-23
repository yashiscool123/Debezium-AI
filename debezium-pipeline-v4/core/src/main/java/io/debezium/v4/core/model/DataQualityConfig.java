package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record DataQualityConfig(
        boolean enabled,
        boolean failOnError,
        String mode,
        List<DataQualityRuleSpec> rules,
        Map<String, String> globalOptions) {

    public static Builder builder() { return new Builder(); }

    public static DataQualityConfig disabled() {
        return new DataQualityConfig(false, false, "async", List.of(), Map.of());
    }

    public static class Builder {
        private boolean enabled = true;
        private boolean failOnError = false;
        private String mode = "async";
        private List<DataQualityRuleSpec> rules = List.of();
        private Map<String, String> globalOptions = Map.of();

        public Builder enabled(boolean v) { this.enabled = v; return this; }
        public Builder failOnError(boolean v) { this.failOnError = v; return this; }
        public Builder mode(String v) { this.mode = v; return this; }
        public Builder rules(List<DataQualityRuleSpec> v) { this.rules = v; return this; }
        public Builder globalOptions(Map<String, String> v) { this.globalOptions = v; return this; }
        public DataQualityConfig build() { return new DataQualityConfig(enabled, failOnError, mode, rules, globalOptions); }
    }
}
