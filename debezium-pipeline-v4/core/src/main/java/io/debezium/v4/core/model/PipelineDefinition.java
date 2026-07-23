package io.debezium.v4.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PipelineDefinition(
    String id,
    String name,
    String description,
    String version,
    String tenantId,
    String serviceUserId,
    boolean runAsServiceUser,
    SourceSpec source,
    TargetSpec target,
    List<TableMappingSpec> tableMappings,
    List<TransformationSpec> transformations,
    DataQualityConfig dataQuality,
    DeploymentSpec deployment,
    MonitoringSpec monitoring,
    Map<String, String> tags,
    Map<String, String> metadata,
    PipelineMetadata pipelineMetadata
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String name;
        private String description;
        private String version = "1.0.0";
        private String tenantId = "default";
        private String serviceUserId;
        private boolean runAsServiceUser = false;
        private SourceSpec source;
        private TargetSpec target;
        private List<TableMappingSpec> tableMappings = List.of();
        private List<TransformationSpec> transformations = List.of();
        private DataQualityConfig dataQuality;
        private DeploymentSpec deployment;
        private MonitoringSpec monitoring;
        private Map<String, String> tags = Map.of();
        private Map<String, String> metadata = Map.of();
        private PipelineMetadata pipelineMetadata;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder serviceUserId(String serviceUserId) { this.serviceUserId = serviceUserId; return this; }
        public Builder runAsServiceUser(boolean runAsServiceUser) { this.runAsServiceUser = runAsServiceUser; return this; }
        public Builder source(SourceSpec source) { this.source = source; return this; }
        public Builder target(TargetSpec target) { this.target = target; return this; }
        public Builder tableMappings(List<TableMappingSpec> tableMappings) { this.tableMappings = tableMappings; return this; }
        public Builder transformations(List<TransformationSpec> transformations) { this.transformations = transformations; return this; }
        public Builder dataQuality(DataQualityConfig dataQuality) { this.dataQuality = dataQuality; return this; }
        public Builder deployment(DeploymentSpec deployment) { this.deployment = deployment; return this; }
        public Builder monitoring(MonitoringSpec monitoring) { this.monitoring = monitoring; return this; }
        public Builder tags(Map<String, String> tags) { this.tags = tags; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder pipelineMetadata(PipelineMetadata pipelineMetadata) { this.pipelineMetadata = pipelineMetadata; return this; }

        public PipelineDefinition build() {
            if (pipelineMetadata == null) {
                pipelineMetadata = PipelineMetadata.builder().build();
            }
            if (dataQuality == null) {
                dataQuality = DataQualityConfig.disabled();
            }
            return new PipelineDefinition(id, name, description, version, tenantId, serviceUserId,
                runAsServiceUser, source, target, tableMappings, transformations, dataQuality, deployment,
                monitoring, tags, metadata, pipelineMetadata);
        }
    }

    public record PipelineMetadata(
        PipelineStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deployedAt,
        String createdBy,
        int versionNumber,
        String previousVersionId,
        String checksum
    ) {
        public static Builder builder() {
            return new Builder();
        }
        public static class Builder {
            private PipelineStatus status = PipelineStatus.DRAFT;
            private Instant createdAt = Instant.now();
            private Instant updatedAt = Instant.now();
            private Instant deployedAt;
            private String createdBy = "system";
            private int versionNumber = 1;
            private String previousVersionId;
            private String checksum;
            public Builder status(PipelineStatus status) { this.status = status; return this; }
            public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
            public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
            public Builder deployedAt(Instant deployedAt) { this.deployedAt = deployedAt; return this; }
            public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
            public Builder versionNumber(int versionNumber) { this.versionNumber = versionNumber; return this; }
            public Builder previousVersionId(String previousVersionId) { this.previousVersionId = previousVersionId; return this; }
            public Builder checksum(String checksum) { this.checksum = checksum; return this; }
            public PipelineMetadata build() {
                return new PipelineMetadata(status, createdAt, updatedAt, deployedAt, createdBy, versionNumber, previousVersionId, checksum);
            }
        }
    }

    public enum PipelineStatus {
        DRAFT, VALIDATING, VALID, INVALID, DEPLOYING, DEPLOYED, RUNNING, DEGRADED, FAILED, STOPPED, ARCHIVED
    }
}
