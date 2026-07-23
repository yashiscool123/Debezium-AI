package io.debezium.pipeline.generator.service;

import io.debezium.pipeline.generator.model.ConnectorType;
import io.debezium.pipeline.generator.model.KafkaConfig;
import io.debezium.relational.TableId;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

@ApplicationScoped
public class TopicNamingStrategyBuilder {

    public enum StrategyType {
        DEFAULT("default", "Default Debezium naming"),
        TOPIC_PREFIX("topic_prefix", "Add prefix to table name"),
        SCHEMA_TABLE("schema_table", "schema.table format"),
        DATABASE_SCHEMA_TABLE("database_schema_table", "database.schema.table format"),
        CUSTOM("custom", "Custom pattern with placeholders"),
        HASH_PARTITION("hash_partition", "Hash-based partitioning for large tables"),
        TIME_BASED("time_based", "Time-based partitioning");

        private final String id;
        private final String description;
        StrategyType(String id, String description) {
            this.id = id;
            this.description = description;
        }
        public String id() { return id; }
        public String description() { return description; }
    }

    public record NamingStrategy(
        String name,
        StrategyType type,
        String pattern,
        Map<String, String> placeholders,
        Function<TableId, String> generator
    ) {}

    public NamingStrategy build(StrategyType type, String topicPrefix, Map<String, String> customPlaceholders) {
        return switch (type) {
            case DEFAULT -> buildDefault(topicPrefix);
            case TOPIC_PREFIX -> buildTopicPrefix(topicPrefix);
            case SCHEMA_TABLE -> buildSchemaTable(topicPrefix);
            case DATABASE_SCHEMA_TABLE -> buildDatabaseSchemaTable(topicPrefix);
            case CUSTOM -> buildCustom(topicPrefix, customPlaceholders);
            case HASH_PARTITION -> buildHashPartition(topicPrefix);
            case TIME_BASED -> buildTimeBased(topicPrefix);
        };
    }

    private NamingStrategy buildDefault(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "default",
            StrategyType.DEFAULT,
            prefix + "${database}.${schema}.${table}",
            Map.of("database", "Database name", "schema", "Schema name", "table", "Table name"),
            tableId -> prefix + tableId.catalog() + "." + tableId.schema() + "." + tableId.table()
        );
    }

    private NamingStrategy buildTopicPrefix(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "topic_prefix",
            StrategyType.TOPIC_PREFIX,
            prefix + "${table}",
            Map.of("table", "Table name"),
            tableId -> prefix + tableId.table()
        );
    }

    private NamingStrategy buildSchemaTable(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "schema_table",
            StrategyType.SCHEMA_TABLE,
            prefix + "${schema}.${table}",
            Map.of("schema", "Schema name", "table", "Table name"),
            tableId -> prefix + tableId.schema() + "." + tableId.table()
        );
    }

    private NamingStrategy buildDatabaseSchemaTable(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "database_schema_table",
            StrategyType.DATABASE_SCHEMA_TABLE,
            prefix + "${database}.${schema}.${table}",
            Map.of("database", "Database/Catalog name", "schema", "Schema name", "table", "Table name"),
            tableId -> prefix + tableId.catalog() + "." + tableId.schema() + "." + tableId.table()
        );
    }

    private NamingStrategy buildCustom(String topicPrefix, Map<String, String> placeholders) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        String pattern = placeholders != null && placeholders.containsKey("pattern") 
            ? placeholders.get("pattern") 
            : prefix + "${database}.${schema}.${table}";
        
        Map<String, String> mergedPlaceholders = new HashMap<>(Map.of(
            "database", "Database/Catalog name",
            "schema", "Schema name",
            "table", "Table name",
            "connector", "Connector name",
            "timestamp", "Current timestamp",
            "uuid", "Random UUID"
        ));
        if (placeholders != null) {
            mergedPlaceholders.putAll(placeholders);
        }

        return new NamingStrategy(
            "custom",
            StrategyType.CUSTOM,
            pattern,
            mergedPlaceholders,
            tableId -> applyPattern(pattern, tableId, Map.of())
        );
    }

    private NamingStrategy buildHashPartition(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "hash_partition",
            StrategyType.HASH_PARTITION,
            prefix + "${table}_${hash}",
            Map.of("table", "Table name", "hash", "Hash partition (0-N)"),
            tableId -> {
                int partitions = 10; // Default, should be configurable
                int hash = Math.abs(tableId.table().hashCode()) % partitions;
                return prefix + tableId.table() + "_" + hash;
            }
        );
    }

    private NamingStrategy buildTimeBased(String topicPrefix) {
        String prefix = topicPrefix != null ? topicPrefix + "." : "";
        return new NamingStrategy(
            "time_based",
            StrategyType.TIME_BASED,
            prefix + "${table}_${year}_${month}",
            Map.of("table", "Table name", "year", "Year (YYYY)", "month", "Month (MM)"),
            tableId -> {
                java.time.LocalDate now = java.time.LocalDate.now();
                return String.format("%s%s_%04d_%02d", prefix, tableId.table(), now.getYear(), now.getMonthValue());
            }
        );
    }

    private String applyPattern(String pattern, TableId tableId, Map<String, String> extras) {
        String result = pattern
            .replace("${database}", tableId.catalog() != null ? tableId.catalog() : "")
            .replace("${schema}", tableId.schema() != null ? tableId.schema() : "")
            .replace("${table}", tableId.table())
            .replace("${connector}", extras.getOrDefault("connector", "debezium"))
            .replace("${timestamp}", String.valueOf(System.currentTimeMillis()))
            .replace("${uuid}", UUID.randomUUID().toString().substring(0, 8))
            .replace("${year}", String.format("%04d", java.time.LocalDate.now().getYear()))
            .replace("${month}", String.format("%02d", java.time.LocalDate.now().getMonthValue()))
            .replace("${day}", String.format("%02d", java.time.LocalDate.now().getDayOfMonth()));
        
        // Clean up double dots and trailing dots
        result = result.replaceAll("\\.+", ".");
        result = result.replaceAll("^\\.|\\.$", "");
        return result;
    }

    public String generateTopicName(NamingStrategy strategy, TableId tableId, Map<String, String> extras) {
        if (strategy.generator() != null) {
            return strategy.generator().apply(tableId);
        }
        return applyPattern(strategy.pattern(), tableId, extras);
    }

    public List<String> previewTopics(NamingStrategy strategy, List<TableId> tables, Map<String, String> extras) {
        return tables.stream()
            .map(table -> generateTopicName(strategy, table, extras))
            .distinct()
            .sorted()
            .toList();
    }

    public KafkaConfig.TopicNamingStrategy toKafkaConfig(NamingStrategy strategy) {
        return new KafkaConfig.TopicNamingStrategy(
            strategy.type().id(),
            strategy.pattern(),
            strategy.placeholders()
        );
    }

    public record KafkaConfig.TopicNamingStrategy(
        String type,
        String pattern,
        Map<String, String> placeholders
    ) {}
}