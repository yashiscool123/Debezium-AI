package io.debezium.v4.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QualityCheckResult(
        String id,
        String pipelineId,
        String ruleName,
        String ruleType,
        String scope,
        String column,
        boolean passed,
        long evaluatedRows,
        long failedRows,
        Double actualValue,
        String severity,
        List<String> details,
        Map<String, Object> sampleFailures,
        Instant timestamp,
        long durationMs) {

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id = java.util.UUID.randomUUID().toString();
        private String pipelineId;
        private String ruleName;
        private String ruleType;
        private String scope;
        private String column;
        private boolean passed;
        private long evaluatedRows;
        private long failedRows;
        private Double actualValue;
        private String severity = "WARN";
        private List<String> details = List.of();
        private Map<String, Object> sampleFailures = Map.of();
        private Instant timestamp = Instant.now();
        private long durationMs;

        public Builder id(String v) { this.id = v; return this; }
        public Builder pipelineId(String v) { this.pipelineId = v; return this; }
        public Builder ruleName(String v) { this.ruleName = v; return this; }
        public Builder ruleType(String v) { this.ruleType = v; return this; }
        public Builder scope(String v) { this.scope = v; return this; }
        public Builder column(String v) { this.column = v; return this; }
        public Builder passed(boolean v) { this.passed = v; return this; }
        public Builder evaluatedRows(long v) { this.evaluatedRows = v; return this; }
        public Builder failedRows(long v) { this.failedRows = v; return this; }
        public Builder actualValue(Double v) { this.actualValue = v; return this; }
        public Builder severity(String v) { this.severity = v; return this; }
        public Builder details(List<String> v) { this.details = v; return this; }
        public Builder sampleFailures(Map<String, Object> v) { this.sampleFailures = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }
        public QualityCheckResult build() { return new QualityCheckResult(id, pipelineId, ruleName, ruleType, scope, column, passed, evaluatedRows, failedRows, actualValue, severity, details, sampleFailures, timestamp, durationMs); }
    }
}
