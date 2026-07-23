package io.debezium.pipeline.generator.service;

import io.debezium.pipeline.generator.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class PipelineValidator {

    @Inject
    ConnectorConfigBuilder configBuilder;

    @Inject
    SchemaIntrospectionService schemaIntrospection;

    public record ValidationReport(
        boolean valid,
        String message,
        List<ValidationIssue> issues,
        Map<String, String> metadata
    ) {}

    public record ValidationIssue(
        String severity,
        String category,
        String field,
        String message,
        String suggestion
    ) {}

    public ValidationReport validate(PipelineSpec spec) {
        List<ValidationIssue> issues = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();

        if (spec == null) {
            return new ValidationReport(false, "Pipeline spec is null", 
                List.of(new ValidationIssue("ERROR", "general", "pipeline", "Pipeline specification is empty", "Create a pipeline specification")), 
                Map.of());
        }

        // 1. Validate source connector
        validateConnectorConfig(spec.sourceConnector(), issues);

        // 2. Validate sink connector if present
        if (spec.sinkConnector() != null) {
            validateConnectorConfig(spec.sinkConnector(), issues);
        }

        // 3. Validate naming conventions
        validateNaming(spec, issues);

        // 4. Validate topic configuration
        validateTopics(spec, issues);

        // 5. Validate transformations
        validateTransformations(spec, issues);

        // 6. Validate deployment configuration
        validateDeployment(spec.deployment(), issues);

        // 7. Validate table mappings
        validateMappings(spec.mappings(), issues);

        boolean valid = issues.stream().noneMatch(i -> "ERROR".equals(i.severity()));
        metadata.put("totalIssues", String.valueOf(issues.size()));
        metadata.put("errorCount", String.valueOf(issues.stream().filter(i -> "ERROR".equals(i.severity())).count()));
        metadata.put("warningCount", String.valueOf(issues.stream().filter(i -> "WARNING".equals(i.severity())).count()));
        metadata.put("infoCount", String.valueOf(issues.stream().filter(i -> "INFO".equals(i.severity())).count()));

        return new ValidationReport(valid, 
            valid ? "Pipeline validation passed" : "Pipeline validation failed", 
            issues, metadata);
    }

    private void validateConnectorConfig(ConnectorConfig connector, List<ValidationIssue> issues) {
        if (connector == null) {
            issues.add(new ValidationIssue("ERROR", "connector", "connector", 
                "Connector configuration is missing", "Provide connector configuration"));
            return;
        }

        if (connector.name() == null || connector.name().isBlank()) {
            issues.add(new ValidationIssue("ERROR", "connector", "name",
                "Connector name is required", "Provide a unique connector name"));
        }

        if (connector.connectorClass() == null || connector.connectorClass().isBlank()) {
            issues.add(new ValidationIssue("ERROR", "connector", "connectorClass",
                "Connector class is required", "Specify the connector implementation class"));
        }

        if (connector.type() == null || connector.type().isBlank()) {
            issues.add(new ValidationIssue("ERROR", "connector", "type",
                "Connector type is required", "Specify the connector type (e.g., mysql, postgresql)"));
        }

        Map<String, String> config = connector.config();
        if (config != null) {
            // Validate required fields based on type
            String type = connector.type().toLowerCase();
            switch (type) {
                case "mysql", "mariadb" -> {
                    validateRequiredConfig(config, issues, "database.hostname", "Database hostname is required");
                    validateRequiredConfig(config, issues, "database.user", "Database user is required");
                    validateRequiredConfig(config, issues, "database.server.name", "Server name is required for MySQL/MariaDB");
                }
                case "postgresql" -> {
                    validateRequiredConfig(config, issues, "database.hostname", "Database hostname is required");
                    validateRequiredConfig(config, issues, "database.user", "Database user is required");
                    validateRequiredConfig(config, issues, "plugin.name", "Logical decoding plugin name is required for PostgreSQL");
                }
                case "mongodb" -> {
                    validateRequiredConfig(config, issues, "mongodb.connection.string", "MongoDB connection string is required");
                }
                case "sqlserver" -> {
                    validateRequiredConfig(config, issues, "database.hostname", "Database hostname is required");
                    validateRequiredConfig(config, issues, "database.user", "Database user is required");
                    validateRequiredConfig(config, issues, "database.server.name", "Server name is required for SQL Server");
                }
                case "oracle" -> {
                    validateRequiredConfig(config, issues, "database.hostname", "Database hostname is required");
                    validateRequiredConfig(config, issues, "database.user", "Database user is required");
                    validateRequiredConfig(config, issues, "database.server.name", "Server name is required for Oracle");
                }
            }

            // Validate port if provided
            if (config.containsKey("database.port")) {
                try {
                    int port = Integer.parseInt(config.get("database.port"));
                    if (port < 1 || port > 65535) {
                        issues.add(new ValidationIssue("ERROR", "connector", "database.port",
                            "Port must be between 1 and 65535", "Use a valid port number"));
                    }
                } catch (NumberFormatException e) {
                    issues.add(new ValidationIssue("ERROR", "connector", "database.port",
                        "Port must be a valid number", "Use a numeric value for port"));
                }
            }

            // Validate snapshot mode if provided
            if (config.containsKey("snapshot.mode")) {
                String mode = config.get("snapshot.mode");
                Set<String> validModes = Set.of("initial", "when_needed", "never", "initial_only",
                    "always", "schema_only", "schema_only_recovery", "custom", "recovery");
                if (!validModes.contains(mode)) {
                    issues.add(new ValidationIssue("WARNING", "connector", "snapshot.mode",
                        "Unknown snapshot mode: " + mode, "Use one of: " + String.join(", ", validModes)));
                }
            }

            // Validate decimal handling mode
            if (config.containsKey("decimal.handling.mode")) {
                String mode = config.get("decimal.handling.mode");
                Set<String> validModes = Set.of("precise", "double", "string", "prefer_decimal");
                if (!validModes.contains(mode)) {
                    issues.add(new ValidationIssue("WARNING", "connector", "decimal.handling.mode",
                        "Unknown decimal handling mode: " + mode, "Use one of: precise, double, string"));
                }
            }

            // Validate SSL configuration
            if (config.containsKey("database.ssl.mode")) {
                String sslMode = config.get("database.ssl.mode");
                Set<String> validSslModes = Set.of("disabled", "preferred", "required", "verify_ca", "verify_identity");
                if (!validSslModes.contains(sslMode)) {
                    issues.add(new ValidationIssue("WARNING", "connector", "database.ssl.mode",
                        "Unknown SSL mode: " + sslMode, "Use one of: disabled, preferred, required, verify_ca, verify_identity"));
                }
            }

            // Validate column blacklist
            if (config.containsKey("column.blacklist")) {
                issues.add(new ValidationIssue("WARNING", "connector", "column.blacklist",
                    "Column blacklist may expose sensitive data", "Consider using column.mask.with.x.x instead"));
            }

            // Warning about schema only backup
            if ("schema_only".equals(config.get("snapshot.mode"))) {
                issues.add(new ValidationIssue("INFO", "connector", "snapshot.mode",
                    "Using schema_only snapshot mode - no data will be captured initially",
                    "Use 'initial' mode to capture existing data"));
            }
        }
    }

    private void validateRequiredConfig(Map<String, String> config, List<ValidationIssue> issues,
                                         String field, String message) {
        if (config.get(field) == null || config.get(field).isBlank()) {
            issues.add(new ValidationIssue("ERROR", "connector", field, message,
                "Add '" + field + "' configuration property"));
        }
    }

    private void validateNaming(PipelineSpec spec, List<ValidationIssue> issues) {
        if (spec.name() != null && spec.name().length() > 128) {
            issues.add(new ValidationIssue("WARNING", "naming", "name",
                "Pipeline name exceeds 128 characters", "Use a shorter name"));
        }

        if (spec.sourceConnector() != null && spec.sourceConnector().name() != null) {
            if (!spec.sourceConnector().name().matches("^[a-zA-Z0-9._-]+$")) {
                issues.add(new ValidationIssue("ERROR", "naming", "connectorName",
                    "Connector name contains invalid characters", "Use only alphanumeric, dots, underscores, and hyphens"));
            }
        }
    }

    private void validateTopics(PipelineSpec spec, List<ValidationIssue> issues) {
        if (spec.sourceConnector() != null && spec.sourceConnector().config() != null) {
            Map<String, String> config = spec.sourceConnector().config();

            if (config.containsKey("topic.prefix")) {
                String prefix = config.get("topic.prefix");
                if (!prefix.matches("^[a-zA-Z0-9._-]+$")) {
                    issues.add(new ValidationIssue("ERROR", "topic", "topic.prefix",
                        "Topic prefix contains invalid characters", "Use only alphanumeric, dots, underscores, and hyphens"));
                }
            }

            if (config.containsKey("topic.creation.groups")) {
                issues.add(new ValidationIssue("INFO", "topic", "topic.creation.groups",
                    "Custom topic creation groups configured", "Verify topic creation settings in Kafka"));
            }
        }

        // Validate topic name length
        if (spec.sourceConnector() != null && spec.sourceConnector().name() != null) {
            String topicName = spec.sourceConnector().name().toLowerCase() + ".topic";
            if (topicName.length() > 249) {
                issues.add(new ValidationIssue("WARNING", "topic", "name",
                    "Topic name may exceed Kafka's 255 character limit", "Use shorter connector name"));
            }
        }
    }

    private void validateTransformations(PipelineSpec spec, List<ValidationIssue> issues) {
        if (spec.globalTransforms() != null) {
            Set<String> transformNames = new HashSet<>();
            for (TransformConfig transform : spec.globalTransforms()) {
                // Check for duplicate names
                if (!transformNames.add(transform.name())) {
                    issues.add(new ValidationIssue("ERROR", "transformation", "name",
                        "Duplicate transformation name: " + transform.name(),
                        "Use unique names for each transformation"));
                }

                // Validate type
                if (transform.type() == null || transform.type().isBlank()) {
                    issues.add(new ValidationIssue("ERROR", "transformation", "type",
                        "Transformation type is required", "Specify the SMT class name"));
                }

                // Check for common misconfigurations
                if (transform.type().contains("ExtractNewRecordState") && !"unwrap".equals(transform.name())) {
                    issues.add(new ValidationIssue("INFO", "transformation", transform.name(),
                        "Common convention: name ExtractNewRecordState transformations 'unwrap'",
                        "Rename to 'unwrap' for consistency"));
                }

                if (transform.type().contains("RegexRouter") && !transform.config().containsKey("regex")) {
                    issues.add(new ValidationIssue("ERROR", "transformation", "config.regex",
                        "RegexRouter requires 'regex' configuration", "Add the regex pattern"));
                }

                if (transform.type().contains("RegexRouter") && !transform.config().containsKey("replacement")) {
                    issues.add(new ValidationIssue("ERROR", "transformation", "config.replacement",
                        "RegexRouter requires 'replacement' configuration", "Add the replacement pattern"));
                }

                // Validate SMT exists if known class not found
                validateSMTExists(transform, issues);
            }
        }
    }

    private void validateSMTExists(TransformConfig transform, List<ValidationIssue> issues) {
        if (transform.type() == null) return;

        Set<String> knownSMTs = Set.of(
            "io.debezium.transforms.ExtractNewRecordState",
            "io.debezium.transforms.outbox.EventRouter",
            "io.debezium.transforms.ContentBasedRouter",
            "io.debezium.transforms.Filter",
            "org.apache.kafka.connect.transforms.Flatten$Value",
            "org.apache.kafka.connect.transforms.Flatten$Key",
            "org.apache.kafka.connect.transforms.MaskField$Value",
            "org.apache.kafka.connect.transforms.MaskField$Key",
            "org.apache.kafka.connect.transforms.ReplaceField$Value",
            "org.apache.kafka.connect.transforms.ReplaceField$Key",
            "org.apache.kafka.connect.transforms.Cast$Value",
            "org.apache.kafka.connect.transforms.Cast$Key",
            "org.apache.kafka.connect.transforms.TimestampConverter$Value",
            "org.apache.kafka.connect.transforms.TimestampConverter$Key",
            "org.apache.kafka.connect.transforms.ExtractField$Value",
            "org.apache.kafka.connect.transforms.ExtractField$Key",
            "org.apache.kafka.connect.transforms.HoistField$Value",
            "org.apache.kafka.connect.transforms.HoistField$Key",
            "org.apache.kafka.connect.transforms.SetSchemaMetadata$Value",
            "org.apache.kafka.connect.transforms.SetSchemaMetadata$Key",
            "org.apache.kafka.connect.transforms.RegexRouter",
            "org.apache.kafka.connect.transforms.TimestampRouter"
        );

        if (knownSMTs.contains(transform.type())) {
            // Known SMT, no additional validation needed
            return;
        }

        if (transform.type().startsWith("io.debezium.") || transform.type().startsWith("org.apache.kafka.connect.")) {
            issues.add(new ValidationIssue("INFO", "transformation", "type",
                "Unknown SMT: " + transform.type() + " - class will be verified at runtime",
                "Verify the SMT exists in the classpath"));
        }
    }

    private void validateDeployment(DeploymentConfig deployment, List<ValidationIssue> issues) {
        if (deployment == null) {
            issues.add(new ValidationIssue("WARNING", "deployment", "deployment",
                "No deployment configuration provided", "Add deployment configuration for automated deployment"));
            return;
        }

        if (deployment.namespace() != null && !deployment.namespace().matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")) {
            issues.add(new ValidationIssue("ERROR", "deployment", "namespace",
                "Invalid Kubernetes namespace name", "Use lowercase alphanumeric and hyphens"));
        }

        if (deployment.replicas() != null && deployment.replicas() < 1) {
            issues.add(new ValidationIssue("ERROR", "deployment", "replicas",
                "Replicas must be at least 1", "Specify a positive number of replicas"));
        }

        if (deployment.resources() != null) {
            ResourceRequirements resources = deployment.resources();
            validateResource(resources.cpuRequest(), "cpu", issues);
            validateResource(resources.memoryRequest(), "memory", issues);
        }
    }

    private void validateResource(String value, String name, List<ValidationIssue> issues) {
        if (value == null) return;

        // Validate CPU format (e.g., 500m, 1, 2.5)
        if ("cpu".equals(name) && !value.matches("^\\d+(\\.\\d+)?[m]?$")) {
            issues.add(new ValidationIssue("WARNING", "deployment", "resources." + name,
                "Invalid CPU resource format: " + value, "Use format like '500m' or '1'"));
        }

        // Validate memory format (e.g., 512Mi, 1Gi, 1024M)
        if ("memory".equals(name) && !value.matches("^\\d+[KMGTV]i?$")) {
            issues.add(new ValidationIssue("WARNING", "deployment", "resources." + name,
                "Invalid memory resource format: " + value, "Use format like '512Mi' or '1Gi'"));
        }
    }

    private void validateMappings(List<MappingRule> mappings, List<ValidationIssue> issues) {
        if (mappings == null || mappings.isEmpty()) {
            issues.add(new ValidationIssue("WARNING", "mapping", "mappings",
                "No table mappings defined", "Add table-level mappings between source and target"));
            return;
        }

        Set<String> sourceTables = new HashSet<>();
        boolean hasTargetTopic = false;

        for (MappingRule mapping : mappings) {
            // Check for duplicate source tables
            if (!sourceTables.add(mapping.sourceTable())) {
                issues.add(new ValidationIssue("WARNING", "mapping", mapping.sourceTable(),
                    "Duplicate source table mapping: " + mapping.sourceTable(),
                    "Each source table should be mapped only once"));
            }

            // Check for topic configuration
            if (mapping.targetTopic() != null) {
                hasTargetTopic = true;
            }

            // Validate column mappings
            if (mapping.columnMappings() != null && !mapping.columnMappings().isEmpty()) {
                for (ColumnMapping cm : mapping.columnMappings()) {
                    validateColumnMapping(cm, issues);
                }
            }
        }

        if (hasTargetTopic) {
            issues.add(new ValidationIssue("INFO", "mapping", "topic",
                "Explicit target topics configured", "Ensure topics exist or topic auto-creation is enabled"));
        }
    }

    private void validateColumnMapping(ColumnMapping mapping, List<ValidationIssue> issues) {
        if (mapping.sourceColumn() == null || mapping.sourceColumn().isBlank()) {
            issues.add(new ValidationIssue("ERROR", "mapping", "sourceColumn",
                "Source column name is required", "Specify the source column name"));
        }

        if (mapping.transformationRule() != null && "cast".equals(mapping.transformationRule().type())) {
            if (mapping.sourceDataType() != null && mapping.targetDataType() != null) {
                String source = mapping.sourceDataType().toLowerCase();
                String target = mapping.targetDataType().toLowerCase();
                if (!areTypesCompatible(source, target)) {
                    issues.add(new ValidationIssue("WARNING", "mapping", 
                        mapping.sourceColumn() + " -> " + mapping.targetColumn(),
                        String.format("Potentially incompatible type cast: %s -> %s", source, target),
                        "Verify the type conversion is valid"));
                }
            }
        }
    }

    private boolean areTypesCompatible(String source, String target) {
        if (source.equals(target)) return true;

        Set<String> numeric = Set.of("int", "integer", "bigint", "smallint", "tinyint", "decimal", "numeric", "float", "double", "real");
        if (numeric.contains(source) && numeric.contains(target)) return true;

        Set<String> string = Set.of("varchar", "char", "text", "clob", "string");
        if (string.contains(source) && string.contains(target)) return true;

        Set<String> temporal = Set.of("date", "time", "timestamp", "datetime");
        if (temporal.contains(source) && temporal.contains(target)) return true;

        return false;
    }

    public ValidationReport validateConfig(ConnectorType type, Map<String, String> config) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        ConnectorConfigBuilder.ValidationResult result = configBuilder.validateConfig(type, config);
        for (ConnectorConfigBuilder.ValidationError err : result.errors()) {
            issues.add(new ValidationIssue("ERROR", "config", err.field(), err.message(), ""));
        }
        for (ConnectorConfigBuilder.ValidationWarning warn : result.warnings()) {
            issues.add(new ValidationIssue("WARNING", "config", warn.field(), warn.message(), ""));
        }

        boolean valid = issues.stream().noneMatch(i -> "ERROR".equals(i.severity()));
        return new ValidationReport(valid, valid ? "Validation passed" : "Validation failed", issues, Map.of());
    }

    public ValidationReport testConnection(SourceDatabaseConfig config) {
        List<ValidationIssue> issues = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            String url = schemaIntrospection.buildJdbcUrl(config);
            DriverManager.getConnection(url, config.username(), config.password()).close();
            long duration = System.currentTimeMillis() - startTime;

            issues.add(new ValidationIssue("INFO", "connection", "database",
                "Connection successful (" + duration + "ms)", ""));
            return new ValidationReport(true, "Connection successful", issues, Map.of("duration", duration + "ms"));
        } catch (SQLException e) {
            String message = e.getMessage();
            String suggestion = switch (e.getSQLState()) {
                case "08001" -> "Check hostname and port";
                case "28000" -> "Invalid username or password";
                case "3D000" -> "Database does not exist";
                case "08004" -> "Server rejected connection";
                default -> "Check database configuration and connectivity";
            };

            issues.add(new ValidationIssue("ERROR", "connection", "database",
                "Connection failed: " + message, suggestion));
            return new ValidationReport(false, "Connection failed", issues, Map.of());
        }
    }

    private double calculateOverallScore(List<ValidationIssue> issues) {
        if (issues.isEmpty()) return 100.0;
        double score = 100.0;
        for (ValidationIssue issue : issues) {
            score -= switch (issue.severity()) {
                case "ERROR" -> 15.0;
                case "WARNING" -> 5.0;
                case "INFO" -> 1.0;
                default -> 0.0;
            };
        }
        return Math.max(0.0, score);
    }
}