package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record TransformationConfig(
    String type,
    List<TransformationStep> steps,
    String ksqlQuery,
    String flinkSql,
    Map<String, String> parameters
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private List<TransformationStep> steps;
        private String ksqlQuery;
        private String flinkSql;
        private Map<String, String> parameters;

        public Builder type(String type) { this.type = type; return this; }
        public Builder steps(List<TransformationStep> steps) { this.steps = steps; return this; }
        public Builder ksqlQuery(String ksqlQuery) { this.ksqlQuery = ksqlQuery; return this; }
        public Builder flinkSql(String flinkSql) { this.flinkSql = flinkSql; return this; }
        public Builder parameters(Map<String, String> parameters) { this.parameters = parameters; return this; }

        public TransformationConfig build() {
            return new TransformationConfig(type, steps, ksqlQuery, flinkSql, parameters);
        }
    }
}