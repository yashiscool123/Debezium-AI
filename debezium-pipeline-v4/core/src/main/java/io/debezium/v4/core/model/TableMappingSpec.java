package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record TableMappingSpec(
        String sourceTable,
        String targetTable,
        String sourceSchema,
        String targetSchema,
        List<ColumnMappingSpec> columnMappings,
        TransformationSpec transformation,
        String matchType,
        double confidence,
        Map<String, String> metadata,
        boolean enabled) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceTable;
        private String targetTable;
        private String sourceSchema;
        private String targetSchema;
        private List<ColumnMappingSpec> columnMappings;
        private TransformationSpec transformation;
        private String matchType;
        private double confidence;
        private Map<String, String> metadata;
        private boolean enabled;

        public Builder sourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
            return this;
        }

        public Builder targetTable(String targetTable) {
            this.targetTable = targetTable;
            return this;
        }

        public Builder sourceSchema(String sourceSchema) {
            this.sourceSchema = sourceSchema;
            return this;
        }

        public Builder targetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
            return this;
        }

        public Builder columnMappings(List<ColumnMappingSpec> columnMappings) {
            this.columnMappings = columnMappings;
            return this;
        }

        public Builder transformation(TransformationSpec transformation) {
            this.transformation = transformation;
            return this;
        }

        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public TableMappingSpec build() {
            return new TableMappingSpec(sourceTable, targetTable, sourceSchema, targetSchema, columnMappings, transformation, matchType, confidence, metadata, enabled);
        }
    }
}
