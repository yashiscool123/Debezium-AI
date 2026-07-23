package io.debezium.pipeline.generator.model;

import java.util.Map;

public record NetworkConfig(
    String serviceType,
    int port,
    int targetPort,
    String protocol,
    Map<String, String> annotations,
    IngressConfig ingress
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceType = "ClusterIP";
        private int port = 8083;
        private int targetPort = 8083;
        private String protocol = "TCP";
        private Map<String, String> annotations;
        private IngressConfig ingress;

        public Builder serviceType(String serviceType) { this.serviceType = serviceType; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder targetPort(int targetPort) { this.targetPort = targetPort; return this; }
        public Builder protocol(String protocol) { this.protocol = protocol; return this; }
        public Builder annotations(Map<String, String> annotations) { this.annotations = annotations; return this; }
        public Builder ingress(IngressConfig ingress) { this.ingress = ingress; return this; }

        public NetworkConfig build() {
            return new NetworkConfig(serviceType, port, targetPort, protocol, annotations, ingress);
        }
    }

    public record IngressConfig(
        boolean enabled,
        String host,
        String path,
        String tlsSecret,
        Map<String, String> annotations
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean enabled = false;
            private String host;
            private String path = "/";
            private String tlsSecret;
            private Map<String, String> annotations;

            public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
            public Builder host(String host) { this.host = host; return this; }
            public Builder path(String path) { this.path = path; return this; }
            public Builder tlsSecret(String tlsSecret) { this.tlsSecret = tlsSecret; return this; }
            public Builder annotations(Map<String, String> annotations) { this.annotations = annotations; return this; }

            public IngressConfig build() {
                return new IngressConfig(enabled, host, path, tlsSecret, annotations);
            }
        }
    }
}