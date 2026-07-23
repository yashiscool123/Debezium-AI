package io.debezium.v4.plugins.deployment;

import io.debezium.v4.core.spi.DeploymentPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class StrimziDeploymentPlugin implements DeploymentPlugin {

    @Override
    public String name() { return "strimzi-deployment"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String description() { return "Generates Strimzi KafkaConnector custom resources"; }

    @Override
    public String generate(Map<String, Object> config) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("apiVersion: kafka.strimzi.io/v1beta2\n");
        yaml.append("kind: KafkaConnector\n");
        yaml.append("metadata:\n");

        Map<String, Object> pipeline = (Map<String, Object>) config.get("pipeline");
        if (pipeline != null && pipeline.get("name") != null) {
            yaml.append("  name: ").append(pipeline.get("name")).append("\n");
        }

        yaml.append("  labels:\n");
        yaml.append("    strimzi.io/cluster: debezium-connect\n");
        yaml.append("    app.kubernetes.io/part-of: debezium-v4\n");
        yaml.append("spec:\n");
        yaml.append("  class: io.debezium.connector.mysql.MySqlConnector\n");
        yaml.append("  tasksMax: 1\n");
        yaml.append("  config:\n");
        yaml.append("    database.hostname: ${DB_HOST}\n");
        yaml.append("    database.port: \"${DB_PORT:3306}\"\n");
        yaml.append("    database.user: \"${DB_USER}\"\n");
        yaml.append("    database.password: \"${DB_PASSWORD}\"\n");
        yaml.append("    database.server.name: \"${DB_SERVER_NAME}\"\n");
        yaml.append("    topic.prefix: debezium-v4\n");
        yaml.append("    snapshot.mode: initial\n");
        yaml.append("    transforms: unwrap,route\n");
        yaml.append("    transforms.unwrap.type: io.debezium.transforms.ExtractNewRecordState\n");
        yaml.append("    transforms.route.type: org.apache.kafka.connect.transforms.RegexRouter\n");
        yaml.append("    transforms.route.regex: (.*)\n");
        yaml.append("    transforms.route.replacement: $1-clean\n");
        return yaml.toString();
    }
}
