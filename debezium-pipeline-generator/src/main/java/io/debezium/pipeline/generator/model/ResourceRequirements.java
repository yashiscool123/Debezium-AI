package io.debezium.pipeline.generator.model;

import java.util.Map;

public record ResourceRequirements(
    String cpuRequest,
    String cpuLimit,
    String memoryRequest,
    String memoryLimit,
    String storageRequest,
    String storageLimit,
    Map<String, String> custom
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cpuRequest = "500m";
        private String cpuLimit = "1000m";
        private String memoryRequest = "512Mi";
        private String memoryLimit = "1Gi";
        private String storageRequest = "1Gi";
        private String storageLimit = "10Gi";
        private Map<String, String> custom;

        public Builder cpuRequest(String cpuRequest) { this.cpuRequest = cpuRequest; return this; }
        public Builder cpuLimit(String cpuLimit) { this.cpuLimit = cpuLimit; return this; }
        public Builder memoryRequest(String memoryRequest) { this.memoryRequest = memoryRequest; return this; }
        public Builder memoryLimit(String memoryLimit) { this.memoryLimit = memoryLimit; return this; }
        public Builder storageRequest(String storageRequest) { this.storageRequest = storageRequest; return this; }
        public Builder storageLimit(String storageLimit) { this.storageLimit = storageLimit; return this; }
        public Builder custom(Map<String, String> custom) { this.custom = custom; return this; }

        public ResourceRequirements build() {
            return new ResourceRequirements(cpuRequest, cpuLimit, memoryRequest, memoryLimit, storageRequest, storageLimit, custom);
        }
    }
}