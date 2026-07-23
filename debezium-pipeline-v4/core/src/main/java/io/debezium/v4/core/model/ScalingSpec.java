package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record ScalingSpec(
        int minReplicas,
        int maxReplicas,
        String metric,
        String targetUtilization,
        int stabilizationWindowSeconds) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int minReplicas = 1;
        private int maxReplicas = 3;
        private String metric = "cpu";
        private String targetUtilization = "70";
        private int stabilizationWindowSeconds = 300;

        public Builder minReplicas(int minReplicas) {
            this.minReplicas = minReplicas;
            return this;
        }

        public Builder maxReplicas(int maxReplicas) {
            this.maxReplicas = maxReplicas;
            return this;
        }

        public Builder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public Builder targetUtilization(String targetUtilization) {
            this.targetUtilization = targetUtilization;
            return this;
        }

        public Builder stabilizationWindowSeconds(int stabilizationWindowSeconds) {
            this.stabilizationWindowSeconds = stabilizationWindowSeconds;
            return this;
        }

        public ScalingSpec build() {
            return new ScalingSpec(minReplicas, maxReplicas, metric, targetUtilization, stabilizationWindowSeconds);
        }
    }
}
