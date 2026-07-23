package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record IngressSpec(
        boolean enabled,
        String host,
        String path,
        String tlsSecret) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = false;
        private String host;
        private String path = "/";
        private String tlsSecret;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder tlsSecret(String tlsSecret) {
            this.tlsSecret = tlsSecret;
            return this;
        }

        public IngressSpec build() {
            return new IngressSpec(enabled, host, path, tlsSecret);
        }
    }
}
