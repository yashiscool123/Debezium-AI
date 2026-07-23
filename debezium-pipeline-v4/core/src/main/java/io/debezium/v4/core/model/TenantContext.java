package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record TenantContext(
        String tenantId,
        String organizationId,
        String environment,
        Map<String, String> properties) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tenantId;
        private String organizationId;
        private String environment = "dev";
        private Map<String, String> properties;

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public TenantContext build() {
            return new TenantContext(tenantId, organizationId, environment, properties);
        }
    }
}
