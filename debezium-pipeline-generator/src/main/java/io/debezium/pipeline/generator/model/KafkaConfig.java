package io.debezium.pipeline.generator.model;

import java.util.Map;

public record KafkaConfig(
    String bootstrapServers,
    String topicPrefix,
    String topicNamingStrategy,
    int partitions,
    short replicationFactor,
    Map<String, String> producerConfig,
    Map<String, String> consumerConfig,
    String securityProtocol,
    String saslMechanism,
    String saslJaasConfig
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String bootstrapServers;
        private String topicPrefix;
        private String topicNamingStrategy;
        private int partitions = 1;
        private short replicationFactor = 1;
        private Map<String, String> producerConfig;
        private Map<String, String> consumerConfig;
        private String securityProtocol;
        private String saslMechanism;
        private String saslJaasConfig;

        public Builder bootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; return this; }
        public Builder topicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; return this; }
        public Builder topicNamingStrategy(String topicNamingStrategy) { this.topicNamingStrategy = topicNamingStrategy; return this; }
        public Builder partitions(int partitions) { this.partitions = partitions; return this; }
        public Builder replicationFactor(short replicationFactor) { this.replicationFactor = replicationFactor; return this; }
        public Builder producerConfig(Map<String, String> producerConfig) { this.producerConfig = producerConfig; return this; }
        public Builder consumerConfig(Map<String, String> consumerConfig) { this.consumerConfig = consumerConfig; return this; }
        public Builder securityProtocol(String securityProtocol) { this.securityProtocol = securityProtocol; return this; }
        public Builder saslMechanism(String saslMechanism) { this.saslMechanism = saslMechanism; return this; }
        public Builder saslJaasConfig(String saslJaasConfig) { this.saslJaasConfig = saslJaasConfig; return this; }

        public KafkaConfig build() {
            return new KafkaConfig(bootstrapServers, topicPrefix, topicNamingStrategy, partitions, replicationFactor, producerConfig, consumerConfig, securityProtocol, saslMechanism, saslJaasConfig);
        }
    }
}