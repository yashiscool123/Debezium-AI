package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record MappingRule(
    String sourceTable,
    String targetTable,
    String sourceTopic,
    String targetTopic,
    List<ColumnMapping> columnMappings,
    TransformConfig transformation,
    Map<String, String> metadata,
    boolean enabled
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceTable;
        private String targetTable;
        private String sourceTopic;
        private String targetTopic;
        private List<ColumnMapping> columnMappings;
        private TransformConfig transformation;
        private Map<String, String> metadata;
        private boolean enabled = true;

        public Builder sourceTable(String sourceTable) { this.sourceTable = sourceTable; return this; }
        public Builder targetTable(String targetTable) { this.targetTable = targetTable; return this; }
        public Builder sourceTopic(String sourceTopic) { this.sourceTopic = sourceTopic; return this; }
        public Builder targetTopic(String targetTopic) { this.targetTopic = targetTopic; return this; }
        public Builder columnMappings(List<ColumnMapping> columnMappings) { this.columnMappings = columnMappings; return this; }
        public Builder transformation(TransformConfig transformation) { this.transformation = transformation; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

        public MappingRule build() {
            return new MappingRule(sourceTable, targetTable, sourceTopic, targetTopic, columnMappings, transformation, metadata, enabled);
        }
    }
}