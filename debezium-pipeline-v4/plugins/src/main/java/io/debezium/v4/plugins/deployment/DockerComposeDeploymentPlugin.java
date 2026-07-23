package io.debezium.v4.plugins.deployment;

import io.debezium.v4.core.spi.DeploymentPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class DockerComposeDeploymentPlugin implements DeploymentPlugin {

    @Override
    public String name() { return "docker-compose-deployment"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String description() { return "Generates Docker Compose deployment files"; }

    @Override
    public String generate(Map<String, Object> config) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("version: '3.8'\n\n");
        yaml.append("services:\n");
        yaml.append("  zookeeper:\n");
        yaml.append("    image: confluentinc/cp-zookeeper:7.6.0\n");
        yaml.append("    environment:\n");
        yaml.append("      ZOOKEEPER_CLIENT_PORT: 2181\n");
        yaml.append("      ZOOKEEPER_TICK_TIME: 2000\n\n");
        yaml.append("  kafka:\n");
        yaml.append("    image: confluentinc/cp-kafka:7.6.0\n");
        yaml.append("    depends_on:\n");
        yaml.append("      - zookeeper\n");
        yaml.append("    ports:\n");
        yaml.append("      - \"9092:9092\"\n");
        yaml.append("    environment:\n");
        yaml.append("      KAFKA_BROKER_ID: 1\n");
        yaml.append("      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181\n");
        yaml.append("      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092\n");
        yaml.append("      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1\n\n");
        yaml.append("  debezium-connect:\n");
        yaml.append("    image: debezium/connect:3.7\n");
        yaml.append("    depends_on:\n");
        yaml.append("      - kafka\n");
        yaml.append("    ports:\n");
        yaml.append("      - \"8083:8083\"\n");
        yaml.append("    environment:\n");
        yaml.append("      BOOTSTRAP_SERVERS: kafka:9092\n");
        yaml.append("      GROUP_ID: debezium-v4\n");
        yaml.append("      CONFIG_STORAGE_TOPIC: debezium-connect-configs\n");
        yaml.append("      OFFSET_STORAGE_TOPIC: debezium-connect-offsets\n");
        yaml.append("      STATUS_STORAGE_TOPIC: debezium-connect-status\n");
        return yaml.toString();
    }
}
