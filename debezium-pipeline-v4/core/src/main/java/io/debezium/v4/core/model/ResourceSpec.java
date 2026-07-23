package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record ResourceSpec(
        String cpuRequest,
        String cpuLimit,
        String memoryRequest,
        String memoryLimit,
        String storageRequest,
        String storageLimit) {

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

        public Builder cpuRequest(String cpuRequest) {
            this.cpuRequest = cpuRequest;
            return this;
        }

        public Builder cpuLimit(String cpuLimit) {
            this.cpuLimit = cpuLimit;
            return this;
        }

        public Builder memoryRequest(String memoryRequest) {
            this.memoryRequest = memoryRequest;
            return this;
        }

        public Builder memoryLimit(String memoryLimit) {
            this.memoryLimit = memoryLimit;
            return this;
        }

        public Builder storageRequest(String storageRequest) {
            this.storageRequest = storageRequest;
            return this;
        }

        public Builder storageLimit(String storageLimit) {
            this.storageLimit = storageLimit;
            return this;
        }

        public ResourceSpec build() {
            return new ResourceSpec(cpuRequest, cpuLimit, memoryRequest, memoryLimit, storageRequest, storageLimit);
        }
    }
}
