package io.debezium.pipeline.generator.model;

public record ScalingConfig(
    int minReplicas,
    int maxReplicas,
    String metric,
    String targetValue,
    int stabilizationWindowSeconds
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int minReplicas = 1;
        private int maxReplicas = 3;
        private String metric = "cpu";
        private String targetValue = "70%";
        private int stabilizationWindowSeconds = 300;

        public Builder minReplicas(int minReplicas) { this.minReplicas = minReplicas; return this; }
        public Builder maxReplicas(int maxReplicas) { this.maxReplicas = maxReplicas; return this; }
        public Builder metric(String metric) { this.metric = metric; return this; }
        public Builder targetValue(String targetValue) { this.targetValue = targetValue; return this; }
        public Builder stabilizationWindowSeconds(int stabilizationWindowSeconds) { this.stabilizationWindowSeconds = stabilizationWindowSeconds; return this; }

        public ScalingConfig build() {
            return new ScalingConfig(minReplicas, maxReplicas, metric, targetValue, stabilizationWindowSeconds);
        }
    }
}