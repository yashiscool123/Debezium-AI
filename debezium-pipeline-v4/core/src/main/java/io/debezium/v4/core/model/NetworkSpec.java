package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record NetworkSpec(
        String serviceType,
        int port,
        int targetPort,
        IngressSpec ingress) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceType = "ClusterIP";
        private int port = 8083;
        private int targetPort = 8083;
        private IngressSpec ingress;

        public Builder serviceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder targetPort(int targetPort) {
            this.targetPort = targetPort;
            return this;
        }

        public Builder ingress(IngressSpec ingress) {
            this.ingress = ingress;
            return this;
        }

        public NetworkSpec build() {
            return new NetworkSpec(serviceType, port, targetPort, ingress);
        }
    }
}
