package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record PipelineConfig(
    String id,
    String name,
    String description,
    SourceDatabaseConfig source,
    TargetDatabaseConfig target,
    List<TableMapping> tableMappings,
    KafkaConfig kafka,
    SchemaRegistryConfig schemaRegistry,
    DeploymentConfig deployment,
    Map<String, String> tags,
    String createdBy,
    long createdAt,
    long updatedAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private SourceDatabaseConfig source;
        private TargetDatabaseConfig target;
        private List<TableMapping> tableMappings;
        private KafkaConfig kafka;
        private SchemaRegistryConfig schemaRegistry;
        private DeploymentConfig deployment;
        private Map<String, String> tags;
        private String createdBy;
        private long createdAt;
        private long updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder source(SourceDatabaseConfig source) { this.source = source; return this; }
        public Builder target(TargetDatabaseConfig target) { this.target = target; return this; }
        public Builder tableMappings(List<TableMapping> tableMappings) { this.tableMappings = tableMappings; return this; }
        public Builder kafka(KafkaConfig kafka) { this.kafka = kafka; return this; }
        public Builder schemaRegistry(SchemaRegistryConfig schemaRegistry) { this.schemaRegistry = schemaRegistry; return this; }
        public Builder deployment(DeploymentConfig deployment) { this.deployment = deployment; return this; }
        public Builder tags(Map<String, String> tags) { this.tags = tags; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }

        public PipelineConfig build() {
            return new PipelineConfig(id, name, description, source, target, tableMappings, kafka, schemaRegistry, deployment, tags, createdBy, createdAt, updatedAt);
        }
    }
}