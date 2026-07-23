package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record DeploymentConfig(
    String type,
    String namespace,
    Map<String, String> labels,
    Map<String, String> annotations,
    ResourceRequirements resources,
    Integer replicas,
    List<String> dependencies,
    ScalingConfig scaling,
    NetworkConfig network,
    SecurityConfig security,
    Map<String, String> environment,
    String strimziVersion
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "strimzi";
        private String namespace = "default";
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private ResourceRequirements resources;
        private Integer replicas = 1;
        private List<String> dependencies;
        private ScalingConfig scaling;
        private NetworkConfig network;
        private SecurityConfig security;
        private Map<String, String> environment;
        private String strimziVersion = "0.41.0";

        public Builder type(String type) { this.type = type; return this; }
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder labels(Map<String, String> labels) { this.labels = labels; return this; }
        public Builder annotations(Map<String, String> annotations) { this.annotations = annotations; return this; }
        public Builder resources(ResourceRequirements resources) { this.resources = resources; return this; }
        public Builder resourceRequirements(ResourceRequirements resources) { this.resources = resources; return this; }
        public Builder replicas(Integer replicas) { this.replicas = replicas; return this; }
        public Builder dependencies(List<String> dependencies) { this.dependencies = dependencies; return this; }
        public Builder scaling(ScalingConfig scaling) { this.scaling = scaling; return this; }
        public Builder network(NetworkConfig network) { this.network = network; return this; }
        public Builder security(SecurityConfig security) { this.security = security; return this; }
        public Builder environment(Map<String, String> environment) { this.environment = environment; return this; }
        public Builder strimziVersion(String strimziVersion) { this.strimziVersion = strimziVersion; return this; }

        public DeploymentConfig build() {
            return new DeploymentConfig(type, namespace, labels, annotations, resources, replicas, dependencies, scaling, network, security, environment, strimziVersion);
        }
    }

    public enum DeploymentType {
        STRIMZI("strimzi"),
        DOCKER_COMPOSE("docker-compose"),
        KUBERNETES("kubernetes"),
        HELM("helm"),
        OPERATOR("operator");

        private final String value;
        DeploymentType(String value) { this.value = value; }
        public String value() { return value; }
    }
}