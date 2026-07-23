package io.debezium.pipeline.generator.service;

import io.debezium.pipeline.generator.model.TransformConfig;
import io.debezium.pipeline.generator.model.TransformationConfig;
import io.debezium.pipeline.generator.model.TransformationRule;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransformChainBuilder {

    public record TransformChain(
        String name,
        List<TransformConfig> transforms,
        String ksqlQuery,
        String flinkSql,
        String description
    ) {}

    public record TransformStep(
        String name,
        String type,
        Map<String, String> config,
        String predicate,
        int order,
        String description
    ) {}

    public TransformChain build(TransformationConfig config) {
        List<TransformConfig> transforms = new ArrayList<>();
        int order = 0;

        if (config.steps() != null) {
            for (TransformationConfig.TransformationStep step : config.steps()) {
                transforms.add(buildTransform(step, order++));
            }
        }

        return new TransformChain(
            config.type() != null ? config.type() : "custom",
            transforms,
            config.ksqlQuery(),
            config.flinkSql(),
            "Generated transformation chain"
        );
    }

    private TransformConfig buildTransform(TransformationConfig.TransformationStep step, int order) {
        return TransformConfig.builder()
            .name(step.name() != null ? step.name() : "transform_" + order)
            .type(step.type())
            .config(step.configuration() != null ? step.configuration() : Map.of())
            .predicate(step.condition())
            .build();
    }

    public TransformChain fromRules(List<TransformationRule> rules) {
        List<TransformConfig> transforms = new ArrayList<>();
        int order = 0;

        for (TransformationRule rule : rules) {
            transforms.add(ruleToTransform(rule, order++));
        }

        return new TransformChain(
            "rule_based_chain",
            transforms,
            null,
            null,
            "Generated from mapping rules"
        );
    }

    private TransformConfig ruleToTransform(TransformationRule rule, int order) {
        Map<String, String> config = new HashMap<>();
        
        if (rule.parameters() != null) {
            config.putAll(rule.parameters());
        }

        String type = switch (rule.type()) {
            case "cast" -> TransformConfig.Type.CAST.className();
            case "rename" -> TransformConfig.Type.REPLACE_FIELD.className();
            case "flatten" -> TransformConfig.Type.FLATTEN.className();
            case "extract_field" -> TransformConfig.Type.EXTRACT_NEW_RECORD_STATE.className();
            case "mask" -> TransformConfig.Type.MASK_FIELD.className();
            case "filter" -> TransformConfig.Type.REPLACE_FIELD.className(); // Use replace field with blacklist
            case "route" -> TransformConfig.Type.REGEX_ROUTER.className();
            case "custom" -> rule.smtClass();
            default -> rule.smtClass();
        };

        if (rule.expression() != null && !rule.expression().isEmpty()) {
            config.put("expression", rule.expression());
        }

        return TransformConfig.builder()
            .name(rule.type() + "_" + order)
            .type(type)
            .config(config)
            .predicate(null)
            .build();
    }

    public String generateKSQL(TransformChain chain, String sourceTopic, String targetTopic) {
        StringBuilder ksql = new StringBuilder();
        ksql.append("CREATE STREAM ").append(targetTopic).append(" AS\n");
        ksql.append("SELECT ");

        boolean first = true;
        for (TransformConfig transform : chain.transforms()) {
            if (transform.type().contains("ExtractNewRecordState")) {
                if (!first) ksql.append(", ");
                ksql.append("*");
                first = false;
            } else if (transform.type().contains("Flatten")) {
                // Flatten is handled by KSQL automatically
            } else if (transform.type().contains("MaskField")) {
                String fields = transform.config().getOrDefault("fields", "");
                if (!fields.isEmpty()) {
                    if (!first) ksql.append(", ");
                    ksql.append("MASK(").append(fields).append(")");
                    first = false;
                }
            } else if (transform.type().contains("ReplaceField")) {
                String rename = transform.config().get("renames");
                String blacklist = transform.config().get("blacklist");
                if (rename != null) {
                    // Handle renames
                }
            }
        }

        if (first) {
            ksql.append("*");
        }

        ksql.append("\nFROM ").append(sourceTopic).append(";\n");

        return ksql.toString();
    }

    public String generateFlinkSQL(TransformChain chain, String sourceTopic, String targetTopic) {
        StringBuilder flink = new StringBuilder();
        flink.append("CREATE TABLE ").append(targetTopic).append(" (\n");
        flink.append("  -- Schema will be inferred from source\n");
        flink.append(") WITH (\n");
        flink.append("  'connector' = 'kafka',\n");
        flink.append("  'topic' = '").append(targetTopic).append("',\n");
        flink.append("  'properties.bootstrap.servers' = 'localhost:9092',\n");
        flink.append("  'format' = 'avro'\n");
        flink.append(");\n\n");

        flink.append("INSERT INTO ").append(targetTopic).append("\n");
        flink.append("SELECT ");

        boolean first = true;
        for (TransformConfig transform : chain.transforms()) {
            if (transform.type().contains("ExtractNewRecordState")) {
                if (!first) flink.append(", ");
                flink.append("*");
                first = false;
            } else if (transform.type().contains("MaskField")) {
                String fields = transform.config().getOrDefault("fields", "");
                if (!fields.isEmpty()) {
                    if (!first) flink.append(", ");
                    flink.append("MASK(").append(fields).append(")");
                    first = false;
                }
            } else if (transform.type().contains("Cast")) {
                String spec = transform.config().get("spec");
                if (spec != null) {
                    // Parse cast spec
                }
            }
        }

        if (first) {
            flink.append("*");
        }

        flink.append("\nFROM ").append(sourceTopic).append(";\n");

        return flink.toString();
    }

    public List<TransformConfig> getDefaultTransformsForConnector(String connectorType) {
        return switch (connectorType.toLowerCase()) {
            case "mysql", "mariadb" -> List.of(
                TransformConfig.builder()
                    .name("unwrap")
                    .type(TransformConfig.Type.EXTRACT_NEW_RECORD_STATE.className())
                    .config(Map.of("drop.tombstones", "false", "delete.handling.mode", "rewrite"))
                    .build()
            );
            case "postgresql" -> List.of(
                TransformConfig.builder()
                    .name("unwrap")
                    .type(TransformConfig.Type.EXTRACT_NEW_RECORD_STATE.className())
                    .config(Map.of("drop.tombstones", "false", "delete.handling.mode", "rewrite"))
                    .build()
            );
            case "mongodb" -> List.of(
                TransformConfig.builder()
                    .name("unwrap")
                    .type(TransformConfig.Type.EXTRACT_NEW_RECORD_STATE.className())
                    .config(Map.of("drop.tombstones", "false", "delete.handling.mode", "rewrite", "operation.header", "true"))
                    .build()
            );
            case "sqlserver" -> List.of(
                TransformConfig.builder()
                    .name("unwrap")
                    .type(TransformConfig.Type.EXTRACT_NEW_RECORD_STATE.className())
                    .config(Map.of("drop.tombstones", "false", "delete.handling.mode", "rewrite"))
                    .build()
            );
            default -> List.of();
        };
    }

    public TransformChain merge(TransformChain base, TransformChain overlay) {
        List<TransformConfig> merged = new ArrayList<>(base.transforms());
        merged.addAll(overlay.transforms());
        
        return new TransformChain(
            base.name() + "_merged",
            merged,
            overlay.ksqlQuery() != null ? overlay.ksqlQuery() : base.ksqlQuery(),
            overlay.flinkSql() != null ? overlay.flinkSql() : base.flinkSql(),
            "Merged: " + base.description() + " + " + overlay.description()
        );
    }
}