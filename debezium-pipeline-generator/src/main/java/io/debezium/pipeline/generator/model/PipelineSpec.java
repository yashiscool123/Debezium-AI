package io.debezium.pipeline.generator.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PipelineSpec(
    String id,
    String name,
    String description,
    String version,
    ConnectorConfig sourceConnector,
    ConnectorConfig sinkConnector,
    List<MappingRule> mappings,
    List<TransformConfig> globalTransforms,
    DeploymentConfig deployment,
    Map<String, String> metadata,
    Instant createdAt,
    Instant updatedAt,
    Status status
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String version = "1.0.0";
        private ConnectorConfig sourceConnector;
        private ConnectorConfig sinkConnector;
        private List<MappingRule> mappings;
        private List<TransformConfig> globalTransforms;
        private DeploymentConfig deployment;
        private Map<String, String> metadata;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private Status status = Status.DRAFT;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder sourceConnector(ConnectorConfig sourceConnector) { this.sourceConnector = sourceConnector; return this; }
        public Builder sinkConnector(ConnectorConfig sinkConnector) { this.sinkConnector = sinkConnector; return this; }
        public Builder mappings(List<MappingRule> mappings) { this.mappings = mappings; return this; }
        public Builder globalTransforms(List<TransformConfig> globalTransforms) { this.globalTransforms = globalTransforms; return this; }
        public Builder deployment(DeploymentConfig deployment) { this.deployment = deployment; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder status(Status status) { this.status = status; return this; }

        public PipelineSpec build() {
            return new PipelineSpec(id, name, description, version, sourceConnector, sinkConnector, mappings, globalTransforms, deployment, metadata, createdAt, updatedAt, status);
        }
    }

    public enum Status {
        DRAFT,
        VALIDATING,
        VALID,
        INVALID,
        DEPLOYING,
        DEPLOYED,
        FAILED,
        ARCHIVED
    }
}