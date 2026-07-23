package io.debezium.v4.core.storage;

import java.time.Instant;
import java.util.*;

public record JobRun(
    String id,
    String pipelineId,
    String pipelineName,
    String tenantId,
    Status status,
    String triggerType,
    String triggeredBy,
    Instant startedAt,
    Instant completedAt,
    long durationMs,
    int recordsProcessed,
    int recordsFailed,
    List<String> errors,
    List<String> warnings,
    Map<String, Object> metrics,
    Map<String, Object> configuration
) {
    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, PAUSED, RETRYING
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id; private String pipelineId; private String pipelineName;
        private String tenantId = "default"; private Status status = Status.PENDING;
        private String triggerType = "manual"; private String triggeredBy;
        private Instant startedAt = Instant.now(); private Instant completedAt;
        private long durationMs; private int recordsProcessed; private int recordsFailed;
        private List<String> errors = new ArrayList<>(); private List<String> warnings = new ArrayList<>();
        private Map<String, Object> metrics = new HashMap<>(); private Map<String, Object> configuration = new HashMap<>();

        public Builder id(String v) { this.id = v; return this; }
        public Builder pipelineId(String v) { this.pipelineId = v; return this; }
        public Builder pipelineName(String v) { this.pipelineName = v; return this; }
        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder status(Status v) { this.status = v; return this; }
        public Builder triggerType(String v) { this.triggerType = v; return this; }
        public Builder triggeredBy(String v) { this.triggeredBy = v; return this; }
        public Builder startedAt(Instant v) { this.startedAt = v; return this; }
        public Builder completedAt(Instant v) { this.completedAt = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }
        public Builder recordsProcessed(int v) { this.recordsProcessed = v; return this; }
        public Builder recordsFailed(int v) { this.recordsFailed = v; return this; }
        public Builder errors(List<String> v) { this.errors = v; return this; }
        public Builder addError(String e) { this.errors.add(e); return this; }
        public Builder warnings(List<String> v) { this.warnings = v; return this; }
        public Builder metrics(Map<String, Object> v) { this.metrics = v; return this; }
        public Builder configuration(Map<String, Object> v) { this.configuration = v; return this; }
        public JobRun build() { return new JobRun(id, pipelineId, pipelineName, tenantId, status, triggerType, triggeredBy, startedAt, completedAt, durationMs, recordsProcessed, recordsFailed, Collections.unmodifiableList(errors), Collections.unmodifiableList(warnings), Collections.unmodifiableMap(metrics), Collections.unmodifiableMap(configuration)); }
    }
}
