package io.debezium.pipeline.generator.model;

import java.util.Map;

public record ColumnMapping(
    String sourceColumn,
    String targetColumn,
    String sourceDataType,
    String targetDataType,
    boolean nullable,
    boolean primaryKey,
    TransformationRule transformationRule,
    double confidenceScore,
    Map<String, String> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceColumn;
        private String targetColumn;
        private String sourceDataType;
        private String targetDataType;
        private boolean nullable;
        private boolean primaryKey;
        private TransformationRule transformationRule;
        private double confidenceScore;
        private Map<String, String> metadata;

        public Builder sourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; return this; }
        public Builder targetColumn(String targetColumn) { this.targetColumn = targetColumn; return this; }
        public Builder sourceDataType(String sourceDataType) { this.sourceDataType = sourceDataType; return this; }
        public Builder targetDataType(String targetDataType) { this.targetDataType = targetDataType; return this; }
        public Builder nullable(boolean nullable) { this.nullable = nullable; return this; }
        public Builder primaryKey(boolean primaryKey) { this.primaryKey = primaryKey; return this; }
        public Builder transformationRule(TransformationRule transformationRule) { this.transformationRule = transformationRule; return this; }
        public Builder confidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public ColumnMapping build() {
            return new ColumnMapping(sourceColumn, targetColumn, sourceDataType, targetDataType, nullable, primaryKey, transformationRule, confidenceScore, metadata);
        }
    }
}