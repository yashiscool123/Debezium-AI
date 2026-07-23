package io.debezium.pipeline.generator;

import io.debezium.pipeline.generator.model.*;
import io.debezium.pipeline.generator.service.PipelineValidator;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineValidatorTest {

    private final PipelineValidator validator = new PipelineValidator();

    @Test
    void testValidPipelinePasses() {
        PipelineSpec spec = createValidPipeline();
        PipelineValidator.ValidationReport report = validator.validate(spec);
        assertTrue(report.valid(), "Valid pipeline should pass validation");
    }

    @Test
    void testNullPipelineFails() {
        PipelineValidator.ValidationReport report = validator.validate(null);
        assertFalse(report.valid());
        assertFalse(report.issues().isEmpty());
    }

    @Test
    void testMissingDatabaseHostnameFails() {
        Map<String, String> config = new HashMap<>();
        config.put("database.port", "3306");
        config.put("database.user", "root");
        ConnectorConfig connector = ConnectorConfig.builder()
            .name("mysql-connector")
            .connectorClass("io.debezium.connector.mysql.MySqlConnector")
            .type("mysql")
            .config(config)
            .build();
        PipelineSpec spec = PipelineSpec.builder()
            .name("test")
            .sourceConnector(connector)
            .build();
        PipelineValidator.ValidationReport report = validator.validate(spec);
        assertTrue(report.issues().stream().anyMatch(i -> "database.hostname".equals(i.field())));
    }

    @Test
    void testEmptyMappingsWarning() {
        PipelineSpec spec = createValidPipeline();
        PipelineValidator.ValidationReport report = validator.validate(spec);
        assertTrue(report.issues().stream().anyMatch(i -> i.category().equals("mapping")));
    }

    @Test
    void testConfigValidationForMySQL() {
        PipelineValidator.ValidationReport report = validator.validateConfig(
            ConnectorType.MYSQL,
            Map.of("database.hostname", "localhost", "database.user", "root")
        );
        assertNotNull(report);
    }

    @Test
    void testInvalidPortShowsWarning() {
        Map<String, String> config = new HashMap<>();
        config.put("database.hostname", "localhost");
        config.put("database.port", "invalid");
        config.put("database.user", "root");
        config.put("database.server.name", "server1");
        ConnectorConfig connector = ConnectorConfig.builder()
            .name("mysql-connector")
            .type("mysql")
            .connectorClass("io.debezium.connector.mysql.MySqlConnector")
            .config(config)
            .build();
        PipelineSpec spec = PipelineSpec.builder()
            .name("test-pipeline")
            .sourceConnector(connector)
            .build();
        PipelineValidator.ValidationReport report = validator.validate(spec);
        assertTrue(report.issues().stream().anyMatch(i -> i.field().equals("database.port")));
    }

    @Test
    void testValidationMetadata() {
        PipelineSpec spec = createValidPipeline();
        PipelineValidator.ValidationReport report = validator.validate(spec);
        assertNotNull(report.metadata());
        assertTrue(report.metadata().containsKey("totalIssues"));
        assertTrue(report.metadata().containsKey("errorCount"));
        assertTrue(report.metadata().containsKey("warningCount"));
    }

    private PipelineSpec createValidPipeline() {
        ConnectorConfig sourceConnector = ConnectorConfig.builder()
            .name("mysql-connector")
            .connectorClass("io.debezium.connector.mysql.MySqlConnector")
            .type("mysql")
            .config(Map.of(
                "database.hostname", "localhost",
                "database.port", "3306",
                "database.user", "dbuser",
                "database.password", "********",
                "database.server.name", "dbserver1",
                "database.include.list", "mydb",
                "snapshot.mode", "initial",
                "topic.prefix", "dbserver1",
                "decimal.handling.mode", "precise"
            ))
            .transforms(List.of(
                TransformConfig.builder()
                    .name("unwrap")
                    .type("io.debezium.transforms.ExtractNewRecordState")
                    .config(Map.of("drop.tombstones", "false", "delete.handling.mode", "rewrite"))
                    .build()
            ))
            .build();

        return PipelineSpec.builder()
            .id(UUID.randomUUID().toString())
            .name("test-pipeline")
            .description("A test pipeline")
            .version("1.0.0")
            .sourceConnector(sourceConnector)
            .deployment(DeploymentConfig.builder()
                .namespace("debezium")
                .type("strimzi")
                .replicas(1)
                .resourceRequirements(ResourceRequirements.builder()
                    .cpuRequest("500m")
                    .memoryRequest("512Mi")
                    .build())
                .build())
            .mappings(List.of())
            .build();
    }
}