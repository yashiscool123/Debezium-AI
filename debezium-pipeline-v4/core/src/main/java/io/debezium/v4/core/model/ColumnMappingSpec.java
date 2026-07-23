package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record ColumnMappingSpec(
        String sourceColumn,
        String targetColumn,
        String sourceDataType,
        String targetDataType,
        boolean nullable,
        boolean primaryKey,
        String transformationRule,
        double confidence,
        String matchType,
        Map<String, String> metadata) {

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
        private String transformationRule;
        private double confidence;
        private String matchType;
        private Map<String, String> metadata;

        public Builder sourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
            return this;
        }

        public Builder targetColumn(String targetColumn) {
            this.targetColumn = targetColumn;
            return this;
        }

        public Builder sourceDataType(String sourceDataType) {
            this.sourceDataType = sourceDataType;
            return this;
        }

        public Builder targetDataType(String targetDataType) {
            this.targetDataType = targetDataType;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder primaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public Builder transformationRule(String transformationRule) {
            this.transformationRule = transformationRule;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ColumnMappingSpec build() {
            return new ColumnMappingSpec(sourceColumn, targetColumn, sourceDataType, targetDataType, nullable, primaryKey, transformationRule, confidence, matchType, metadata);
        }
    }
}
