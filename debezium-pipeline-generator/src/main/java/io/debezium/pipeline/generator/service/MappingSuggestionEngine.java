package io.debezium.pipeline.generator.service;

import io.debezium.pipeline.generator.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class MappingSuggestionEngine {

    @Inject
    SchemaIntrospectionService schemaIntrospection;

    @Inject
    EmbeddingService embeddingService;

    public record MappingSuggestion(
        List<SuggestedMapping> mappings,
        List<UnmappedField> unmappedSourceFields,
        List<UnmappedField> unmappedTargetFields,
        double overallConfidence,
        List<String> warnings
    ) {}

    public record SuggestedMapping(
        String sourceTable,
        String targetTable,
        String sourceTopic,
        String targetTopic,
        List<ColumnMapping> columnMappings,
        TransformationConfig transformation,
        double confidence,
        String matchType
    ) {}

    public record UnmappedField(
        String table,
        String column,
        String dataType,
        String reason
    ) {}

    public record TableMappingRequest(
        String sourceTable,
        String targetTable,
        List<String> excludedColumns,
        Map<String, String> columnRenames,
        TransformationConfig transformation
    ) {}

    public MappingSuggestion suggestMappings(
            SourceDatabaseConfig sourceConfig,
            TargetDatabaseConfig targetConfig,
            List<TableMappingRequest> explicitMappings) {
        
        // Introspect source schema
        SchemaIntrospectionService.SchemaInfo sourceSchema = schemaIntrospection.introspect(sourceConfig);
        
        // Introspect target schema (if JDBC)
        SchemaIntrospectionService.SchemaInfo targetSchema = null;
        if (isJdbcType(targetConfig.type())) {
            SourceDatabaseConfig targetAsSource = SourceDatabaseConfig.builder()
                .type(targetConfig.type())
                .host(targetConfig.host())
                .port(targetConfig.port())
                .username(targetConfig.username())
                .password(targetConfig.password())
                .databaseName(targetConfig.databaseName())
                .build();
            targetSchema = schemaIntrospection.introspect(targetAsSource);
        }
        
        List<SuggestedMapping> mappings = new ArrayList<>();
        List<UnmappedField> unmappedSource = new ArrayList<>();
        List<UnmappedField> unmappedTarget = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Process explicit mappings first
        Set<String> mappedSourceTables = new HashSet<>();
        Set<String> mappedTargetTables = new HashSet<>();
        
        for (TableMappingRequest req : explicitMappings) {
            Optional<SchemaIntrospectionService.TableInfo> sourceTable = 
                sourceSchema.tables().stream()
                    .filter(t -> t.name().equalsIgnoreCase(req.sourceTable()))
                    .findFirst();
            
            Optional<SchemaIntrospectionService.TableInfo> targetTable = 
                targetSchema != null ? targetSchema.tables().stream()
                    .filter(t -> t.name().equalsIgnoreCase(req.targetTable()))
                    .findFirst() : Optional.empty();
            
            if (sourceTable.isPresent() && targetTable.isPresent()) {
                SuggestedMapping mapping = buildMapping(
                    sourceTable.get(), targetTable.get(), req
                );
                mappings.add(mapping);
                mappedSourceTables.add(req.sourceTable());
                mappedTargetTables.add(req.targetTable());
            } else if (sourceTable.isPresent()) {
                warnings.add("Target table not found: " + req.targetTable());
            } else {
                warnings.add("Source table not found: " + req.sourceTable());
            }
        }
        
        // Auto-map remaining tables using semantic similarity
        if (targetSchema != null) {
            for (SchemaIntrospectionService.TableInfo sourceTable : sourceSchema.tables()) {
                if (mappedSourceTables.contains(sourceTable.name())) continue;
                
                Optional<SchemaIntrospectionService.TableInfo> bestTarget = findBestTargetTable(
                    sourceTable, targetSchema.tables(), mappedTargetTables
                );
                
                if (bestTarget.isPresent()) {
                    SuggestedMapping mapping = buildMapping(sourceTable, bestTarget.get(), null);
                    mappings.add(mapping);
                    mappedSourceTables.add(sourceTable.name());
                    mappedTargetTables.add(bestTarget.get().name());
                } else {
                    // No target match - add all columns as unmapped
                    for (SchemaField col : sourceTable.columns()) {
                        unmappedSource.add(new UnmappedField(
                            sourceTable.name(), col.name(), col.dataType(),
                            "No matching target table found"
                        ));
                    }
                }
            }
            
            // Find unmapped target tables
            for (SchemaIntrospectionService.TableInfo targetTable : targetSchema.tables()) {
                if (!mappedTargetTables.contains(targetTable.name())) {
                    for (SchemaField col : targetTable.columns()) {
                        unmappedTarget.add(new UnmappedField(
                            targetTable.name(), col.name(), col.dataType(),
                            "No matching source table found"
                        ));
                    }
                }
            }
        }
        
        double overallConfidence = mappings.isEmpty() ? 0.0 : 
            mappings.stream().mapToDouble(SuggestedMapping::confidence).average().orElse(0.0);
        
        return new MappingSuggestion(mappings, unmappedSource, unmappedTarget, overallConfidence, warnings);
    }

    private SuggestedMapping buildMapping(
            SchemaIntrospectionService.TableInfo sourceTable,
            SchemaIntrospectionService.TableInfo targetTable,
            TableMappingRequest explicitReq) {
        
        // Build column mappings using embedding similarity
        List<ColumnMapping> columnMappings = new ArrayList<>();
        
        for (SchemaField sourceCol : sourceTable.columns()) {
            // Check for explicit rename
            String targetColName = explicitReq != null && explicitReq.columnRenames() != null
                ? explicitReq.columnRenames().getOrDefault(sourceCol.name(), sourceCol.name())
                : sourceCol.name();
            
            // Check if excluded
            boolean excluded = explicitReq != null && explicitReq.excludedColumns() != null
                && explicitReq.excludedColumns().contains(sourceCol.name());
            
            if (excluded) continue;
            
            // Find best target column match
            Optional<SchemaField> bestMatch = findBestColumnMatch(
                sourceCol, targetTable.columns()
            );
            
            if (bestMatch.isPresent()) {
                SchemaField targetCol = bestMatch.get();
                
                // Use explicit rename if provided
                String finalTargetName = targetColName.equals(sourceCol.name()) 
                    ? targetCol.name() 
                    : targetColName;
                
                TransformationRule rule = inferTransformation(sourceCol, targetCol);
                
                columnMappings.add(ColumnMapping.builder()
                    .sourceColumn(sourceCol.name())
                    .targetColumn(finalTargetName)
                    .sourceDataType(sourceCol.dataType())
                    .targetDataType(targetCol.dataType())
                    .nullable(sourceCol.nullable())
                    .primaryKey(sourceCol.primaryKey())
                    .transformationRule(rule)
                    .confidenceScore(calculateColumnConfidence(sourceCol, targetCol))
                    .metadata(Map.of(
                        "sourceTable", sourceTable.name(),
                        "targetTable", targetTable.name(),
                        "matchType", "semantic"
                    ))
                    .build());
            } else {
                // No match found
                columnMappings.add(ColumnMapping.builder()
                    .sourceColumn(sourceCol.name())
                    .targetColumn(sourceCol.name())
                    .sourceDataType(sourceCol.dataType())
                    .targetDataType(sourceCol.dataType())
                    .nullable(sourceCol.nullable())
                    .primaryKey(sourceCol.primaryKey())
                    .transformationRule(TransformationRule.builder()
                        .type("none")
                        .description("No target match found")
                        .build())
                    .confidenceScore(0.0)
                    .metadata(Map.of(
                        "sourceTable", sourceTable.name(),
                        "targetTable", targetTable.name(),
                        "matchType", "unmatched"
                    ))
                    .build());
            }
        }
        
        // Calculate overall confidence for this mapping
        double confidence = columnMappings.isEmpty() ? 0.0 :
            columnMappings.stream().mapToDouble(ColumnMapping::confidenceScore).average().orElse(0.0);
        
        // Determine topics
        String sourceTopic = sourceTable.name();
        String targetTopic = targetTable.name();
        
        return new SuggestedMapping(
            sourceTable.name(),
            targetTable.name(),
            sourceTopic,
            targetTopic,
            columnMappings,
            explicitReq != null ? explicitReq.transformation() : null,
            confidence,
            explicitReq != null ? "explicit" : "auto"
        );
    }

    private Optional<SchemaIntrospectionService.TableInfo> findBestTargetTable(
            SchemaIntrospectionService.TableInfo sourceTable,
            List<SchemaIntrospectionService.TableInfo> targetTables,
            Set<String> excluded) {
        
        return targetTables.stream()
            .filter(t -> !excluded.contains(t.name()))
            .max(Comparator.comparingDouble(t -> 
                tableSimilarity(sourceTable, t)
            ));
    }

    private double tableSimilarity(
            SchemaIntrospectionService.TableInfo source,
            SchemaIntrospectionService.TableInfo target) {
        
        // Name similarity
        double nameSim = stringSimilarity(source.name().toLowerCase(), target.name().toLowerCase());
        
        // Column count similarity
        int srcCols = source.columns().size();
        int tgtCols = target.columns().size();
        double colCountSim = 1.0 - Math.abs(srcCols - tgtCols) / (double) Math.max(srcCols, tgtCols);
        
        // Column name overlap
        Set<String> srcColNames = source.columns().stream()
            .map(SchemaField::name)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        Set<String> tgtColNames = target.columns().stream()
            .map(SchemaField::name)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(srcColNames);
        intersection.retainAll(tgtColNames);
        Set<String> union = new HashSet<>(srcColNames);
        union.addAll(tgtColNames);
        double colNameSim = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        
        // Weighted combination
        return 0.4 * nameSim + 0.3 * colCountSim + 0.3 * colNameSim;
    }

    private Optional<SchemaField> findBestColumnMatch(
            SchemaField source, List<SchemaField> targets) {
        
        return targets.stream()
            .max(Comparator.comparingDouble(t -> columnSimilarity(source, t)));
    }

    private double columnSimilarity(SchemaField source, SchemaField target) {
        // Name similarity (exact match gets high score)
        double nameSim = stringSimilarity(source.name().toLowerCase(), target.name().toLowerCase());
        
        // Type compatibility
        double typeSim = areTypesCompatible(source.dataType(), target.dataType()) ? 1.0 : 0.3;
        
        // Nullable compatibility
        double nullableSim = source.nullable() == target.nullable() ? 1.0 : 0.8;
        
        // Primary key
        double pkSim = source.primaryKey() == target.primaryKey() ? 1.0 : 0.5;
        
        return 0.5 * nameSim + 0.2 * typeSim + 0.15 * nullableSim + 0.15 * pkSim;
    }

    private double stringSimilarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        
        // Levenshtein distance based similarity
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(a, b);
        return 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            costs[j] = j;
        }
        
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                    a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private TransformationRule inferTransformation(SchemaField source, SchemaField target) {
        if (source.dataType().equalsIgnoreCase(target.dataType()) 
            && source.name().equalsIgnoreCase(target.name())) {
            return TransformationRule.builder()
                .type("none")
                .description("Direct mapping")
                .build();
        }
        
        StringBuilder desc = new StringBuilder();
        List<String> rules = new ArrayList<>();
        
        if (!source.name().equalsIgnoreCase(target.name())) {
            rules.add("rename");
            desc.append("Rename ").append(source.name()).append(" to ").append(target.name());
        }
        
        if (!source.dataType().equalsIgnoreCase(target.dataType())) {
            rules.add("cast");
            if (desc.length() > 0) desc.append("; ");
            desc.append("Cast from ").append(source.dataType()).append(" to ").append(target.dataType());
        }
        
        return TransformationRule.builder()
            .type(String.join(",", rules))
            .description(desc.toString())
            .expression(rules.contains("cast") 
                ? "CAST(" + source.name() + " AS " + target.dataType() + ")" 
                : null)
            .build();
    }

    private double calculateColumnConfidence(SchemaField source, SchemaField target) {
        double nameSim = stringSimilarity(source.name().toLowerCase(), target.name().toLowerCase());
        double typeSim = areTypesCompatible(source.dataType(), target.dataType()) ? 1.0 : 0.3;
        double nullableSim = source.nullable() == target.nullable() ? 1.0 : 0.8;
        double pkSim = source.primaryKey() == target.primaryKey() ? 1.0 : 0.5;
        
        return 0.5 * nameSim + 0.2 * typeSim + 0.15 * nullableSim + 0.15 * pkSim;
    }

    private boolean areTypesCompatible(String source, String target) {
        String s = source.toLowerCase();
        String t = target.toLowerCase();
        
        // Same type
        if (s.equals(t)) return true;
        
        // Numeric compatibility
        Set<String> numeric = Set.of("integer", "bigint", "smallint", "tinyint", "decimal", 
            "numeric", "number", "float", "double", "real");
        if (numeric.contains(s) && numeric.contains(t)) return true;
        
        // String compatibility
        Set<String> string = Set.of("varchar", "char", "text", "clob", "string");
        if (string.contains(s) && string.contains(t)) return true;
        
        // Temporal compatibility
        Set<String> temporal = Set.of("date", "time", "timestamp", "datetime");
        if (temporal.contains(s) && temporal.contains(t)) return true;
        
        // Boolean
        if (s.contains("bool") && t.contains("bool")) return true;
        
        // Binary
        Set<String> binary = Set.of("blob", "binary", "varbinary", "bytes");
        if (binary.contains(s) && binary.contains(t)) return true;
        
        return false;
    }

    private boolean isJdbcType(String type) {
        return Set.of("mysql", "postgresql", "sqlserver", "oracle", "mariadb", "db2", 
            "jdbc", "informix", "cockroachdb", "yugabyte").contains(type.toLowerCase());
    }
}