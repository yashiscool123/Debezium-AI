package io.debezium.pipeline.generator.service;

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.connector.common.BaseSourceConnector;
import io.debezium.pipeline.generator.model.ConnectorConfig;
import io.debezium.pipeline.generator.model.ConnectorType;
import io.debezium.pipeline.generator.model.KafkaConfig;
import io.debezium.pipeline.generator.model.SchemaRegistryConfig;
import io.debezium.pipeline.generator.model.TransformConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConnectorConfigBuilder {

    private final Map<ConnectorType, Class<? extends BaseSourceConnector>> connectorClasses;

    public ConnectorConfigBuilder() {
        this.connectorClasses = Map.ofEntries(
            Map.entry(ConnectorType.MYSQL, loadConnectorClass("io.debezium.connector.mysql.MySqlConnector")),
            Map.entry(ConnectorType.POSTGRESQL, loadConnectorClass("io.debezium.connector.postgresql.PostgresConnector")),
            Map.entry(ConnectorType.MONGODB, loadConnectorClass("io.debezium.connector.mongodb.MongoDbConnector")),
            Map.entry(ConnectorType.SQLSERVER, loadConnectorClass("io.debezium.connector.sqlserver.SqlServerConnector")),
            Map.entry(ConnectorType.ORACLE, loadConnectorClass("io.debezium.connector.oracle.OracleConnector")),
            Map.entry(ConnectorType.MARIADB, loadConnectorClass("io.debezium.connector.mariadb.MariaDbConnector")),
            Map.entry(ConnectorType.JDBC, loadConnectorClass("io.debezium.connector.jdbc.JdbcConnector")),
            Map.entry(ConnectorType.DB2, loadConnectorClass("io.debezium.connector.db2.Db2Connector")),
            Map.entry(ConnectorType.CASSANDRA, loadConnectorClass("io.debezium.connector.cassandra.CassandraConnector")),
            Map.entry(ConnectorType.VITESS, loadConnectorClass("io.debezium.connector.vitess.VitessConnector")),
            Map.entry(ConnectorType.SPOKER, loadConnectorClass("io.debezium.connector.spanner.SpannerConnector")),
            Map.entry(ConnectorType.INFORMIX, loadConnectorClass("io.debezium.connector.informix.InformixConnector")),
            Map.entry(ConnectorType.YASHANDB, loadConnectorClass("io.debezium.connector.yashandb.YashanDBConnector")),
            Map.entry(ConnectorType.COCKROACHDB, loadConnectorClass("io.debezium.connector.cockroachdb.CockroachDbConnector"))
        );
    }

    @SuppressWarnings("unchecked")
    private Class<? extends BaseSourceConnector> loadConnectorClass(String className) {
        try {
            return (Class<? extends BaseSourceConnector>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.warnf("Connector class not found: %s", className);
            return null;
        }
    }

    public ConnectorConfig build(ConnectorType type, Map<String, String> userConfig, KafkaConfig kafka, SchemaRegistryConfig schemaRegistry) {
        Class<? extends BaseSourceConnector> connectorClass = connectorClasses.get(type);
        if (connectorClass == null) {
            throw new IllegalArgumentException("Unsupported connector type: " + type);
        }

        try {
            BaseSourceConnector connector = connectorClass.getDeclaredConstructor().newInstance();
            ConfigDef configDef = connector.config();
            
            Map<String, ConfigValue> validated = configDef.validate(userConfig);
            Map<String, String> finalConfig = new HashMap<>(userConfig);
            
            // Apply defaults for missing values
            configDef.configValues().forEach((key, value) -> {
                if (!finalConfig.containsKey(key) && value.defaultValue != null) {
                    finalConfig.put(key, value.defaultValue.toString());
                }
            });

            // Add Kafka and Schema Registry configs
            addKafkaConfig(finalConfig, kafka);
            addSchemaRegistryConfig(finalConfig, schemaRegistry);

            return ConnectorConfig.builder()
                .name(generateConnectorName(type))
                .connectorType(type)
                .config(finalConfig)
                .kafka(kafka)
                .schemaRegistry(schemaRegistry)
                .transforms(List.of())
                .predicates(Map.of())
                .errorsTolerance("none")
                .errorsLogEnable("true")
                .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build connector config for " + type, e);
        }
    }

    public Map<String, Field> getConfigFields(ConnectorType type) {
        Class<? extends BaseSourceConnector> connectorClass = connectorClasses.get(type);
        if (connectorClass == null) {
            throw new IllegalArgumentException("Unsupported connector type: " + type);
        }

        try {
            BaseSourceConnector connector = connectorClass.getDeclaredConstructor().newInstance();
            Method method = connectorClass.getMethod("configFields");
            @SuppressWarnings("unchecked")
            Map<String, Field> fields = (Map<String, Field>) method.invoke(connector);
            return fields;
        } catch (Exception e) {
            Log.warnf("Could not get config fields for %s: %s", type, e.getMessage());
            return Map.of();
        }
    }

    public ConfigSchema getConfigSchema(ConnectorType type) {
        Map<String, Field> fields = getConfigFields(type);
        List<ConfigField> configFields = fields.entrySet().stream()
            .map(entry -> ConfigField.fromField(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        return new ConfigSchema(type.getId(), type.getDisplayName(), configFields);
    }

    public ValidationResult validateConfig(ConnectorType type, Map<String, String> config) {
        Class<? extends BaseSourceConnector> connectorClass = connectorClasses.get(type);
        if (connectorClass == null) {
            return ValidationResult.invalid("Unsupported connector type: " + type);
        }

        try {
            BaseSourceConnector connector = connectorClass.getDeclaredConstructor().newInstance();
            ConfigDef configDef = connector.config();
            Map<String, ConfigValue> validated = configDef.validate(config);
            
            List<ValidationError> errors = new ArrayList<>();
            List<ValidationWarning> warnings = new ArrayList<>();
            
            validated.forEach((key, value) -> {
                if (!value.errorMessages.isEmpty()) {
                    value.errorMessages.forEach(msg -> errors.add(new ValidationError(key, msg)));
                }
                if (!value.recommendedValues.isEmpty()) {
                    warnings.add(new ValidationWarning(key, "Recommended values: " + value.recommendedValues));
                }
            });

            return new ValidationResult(errors.isEmpty(), errors, warnings);
        } catch (Exception e) {
            return ValidationResult.invalid("Validation failed: " + e.getMessage());
        }
    }

    private void addKafkaConfig(Map<String, String> config, KafkaConfig kafka) {
        if (kafka != null) {
            config.put("bootstrap.servers", kafka.bootstrapServers());
            if (kafka.topicPrefix() != null) {
                config.put("topic.prefix", kafka.topicPrefix());
            }
            if (kafka.producerConfig() != null) {
                kafka.producerConfig().forEach((k, v) -> config.put("producer." + k, v));
            }
            if (kafka.consumerConfig() != null) {
                kafka.consumerConfig().forEach((k, v) -> config.put("consumer." + k, v));
            }
        }
    }

    private void addSchemaRegistryConfig(Map<String, String> config, SchemaRegistryConfig schemaRegistry) {
        if (schemaRegistry != null && schemaRegistry.url() != null) {
            config.put("schema.registry.url", schemaRegistry.url());
            config.put("schema.registry.compatibility", schemaRegistry.schemaCompatibility());
            config.put("auto.register.schemas", String.valueOf(schemaRegistry.autoRegisterSchemas()));
            config.put("use.latest.version", String.valueOf(schemaRegistry.useLatestVersion()));
            if (schemaRegistry.properties() != null) {
                schemaRegistry.properties().forEach((k, v) -> config.put("schema.registry." + k, v));
            }
        }
    }

    private String generateConnectorName(ConnectorType type) {
        return type.getId() + "-connector-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public record ConfigSchema(String connectorType, String displayName, List<ConfigField> fields) {}
    
    public record ConfigField(
        String name,
        String displayName,
        String description,
        String type,
        String defaultValue,
        boolean required,
        boolean important,
        List<String> enumValues,
        String validationRegex,
        int minValue,
        int maxValue,
        String group,
        int order
    ) {
        public static ConfigField fromField(String name, Field field) {
            return new ConfigField(
                name,
                field.name(),
                field.documentation(),
                field.type().getSimpleName(),
                field.defaultValue() != null ? field.defaultValue().toString() : null,
                field.required(),
                field.importance() == Field.Importance.HIGH,
                field.validator() != null ? List.of() : List.of(),
                null, 0, 0, null, 0
            );
        }
    }

    public record ValidationResult(
        boolean valid,
        List<ValidationError> errors,
        List<ValidationWarning> warnings
    ) {
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, List.of(new ValidationError("config", message)), List.of());
        }
    }

    public record ValidationError(String field, String message) {}
    public record ValidationWarning(String field, String message) {}
}