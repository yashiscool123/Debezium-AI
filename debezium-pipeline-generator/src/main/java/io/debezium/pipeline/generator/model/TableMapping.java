package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record TableMapping(
    String sourceTable,
    String targetTable,
    List<ColumnMapping> columnMappings,
    TransformationConfig transformation,
    Map<String, String> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceTable;
        private String targetTable;
        private List<ColumnMapping> columnMappings;
        private TransformationConfig transformation;
        private Map<String, String> metadata;

        public Builder sourceTable(String sourceTable) { this.sourceTable = sourceTable; return this; }
        public Builder targetTable(String targetTable) { this.targetTable = targetTable; return this; }
        public Builder columnMappings(List<ColumnMapping> columnMappings) { this.columnMappings = columnMappings; return this; }
        public Builder transformation(TransformationConfig transformation) { this.transformation = transformation; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public TableMapping build() {
            return new TableMapping(sourceTable, targetTable, columnMappings, transformation, metadata);
        }
    }
}