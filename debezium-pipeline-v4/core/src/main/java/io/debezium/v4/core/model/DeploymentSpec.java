package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record DeploymentSpec(
        String type,
        String targetEnvironment,
        String namespace,
        String connectClusterName,
        int replicas,
        ResourceSpec resources,
        ScalingSpec scaling,
        NetworkSpec network,
        Map<String, String> environment,
        Map<String, String> labels,
        Map<String, String> annotations) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "strimzi";
        private String targetEnvironment = "kubernetes";
        private String namespace = "debezium";
        private String connectClusterName = "debezium-connect";
        private int replicas = 1;
        private ResourceSpec resources;
        private ScalingSpec scaling;
        private NetworkSpec network;
        private Map<String, String> environment;
        private Map<String, String> labels;
        private Map<String, String> annotations;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder targetEnvironment(String targetEnvironment) {
            this.targetEnvironment = targetEnvironment;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder connectClusterName(String connectClusterName) {
            this.connectClusterName = connectClusterName;
            return this;
        }

        public Builder replicas(int replicas) {
            this.replicas = replicas;
            return this;
        }

        public Builder resources(ResourceSpec resources) {
            this.resources = resources;
            return this;
        }

        public Builder scaling(ScalingSpec scaling) {
            this.scaling = scaling;
            return this;
        }

        public Builder network(NetworkSpec network) {
            this.network = network;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder annotations(Map<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public DeploymentSpec build() {
            return new DeploymentSpec(type, targetEnvironment, namespace, connectClusterName, replicas, resources, scaling, network, environment, labels, annotations);
        }
    }
}
