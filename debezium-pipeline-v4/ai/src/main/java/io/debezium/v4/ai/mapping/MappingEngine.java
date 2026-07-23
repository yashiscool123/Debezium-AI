package io.debezium.v4.ai.mapping;

import io.debezium.v4.ai.embeddings.EmbeddingService;
import io.debezium.v4.ai.llm.LLMService;
import io.debezium.v4.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class MappingEngine {
    private final EmbeddingService embeddingService;
    private final LLMService llmService;

    public MappingEngine(EmbeddingService embeddingService, LLMService llmService) {
        this.embeddingService = embeddingService;
        this.llmService = llmService;
    }

    public MappingResult generateMappings(List<SchemaTable> sourceTables, List<SchemaTable> targetTables, MappingOptions options) {
        List<TableMappingSpec> mappings = new ArrayList<>();
        Set<String> mappedSource = new HashSet<>();
        Set<String> mappedTarget = new HashSet<>();

        double threshold = options != null ? options.confidenceThreshold() : 0.3;
        boolean useLLM = options != null && options.useLLM();

        for (SchemaTable source : sourceTables) {
            SchemaTable bestTarget = findBestTarget(source, targetTables, mappedTarget);
            if (bestTarget == null) continue;

            List<ColumnMappingSpec> colMappings = new ArrayList<>();

            for (SchemaField srcCol : source.columns()) {
                ColumnMappingSpec bestCol = findBestColumnMapping(srcCol, bestTarget.columns(), threshold);
                if (bestCol != null) {
                    colMappings.add(bestCol);
                }
            }

            double confidence = colMappings.isEmpty() ? 0 :
                colMappings.stream().mapToDouble(ColumnMappingSpec::confidence).average().orElse(0);

            TableMappingSpec mapping = TableMappingSpec.builder()
                .sourceTable(source.name())
                .targetTable(bestTarget.name())
                .sourceSchema(source.schema())
                .targetSchema(bestTarget.schema())
                .columnMappings(colMappings)
                .confidence(confidence)
                .matchType("auto")
                .enabled(confidence >= threshold)
                .build();

            mappings.add(mapping);
            mappedSource.add(source.fullName());
            mappedTarget.add(bestTarget.fullName());
        }

        if (useLLM) {
            String context = buildSchemaContext(sourceTables, targetTables);
            List<Map<String,String>> llmSuggestions = llmService.suggestMappings(context);
            mergeLLMSuggestions(mappings, llmSuggestions);
        }

        List<UnmappedField> unmappedSource = findUnmapped(sourceTables, mappedSource);
        List<UnmappedField> unmappedTarget = findUnmapped(targetTables, mappedTarget);

        double overall = mappings.isEmpty() ? 0 : mappings.stream().mapToDouble(TableMappingSpec::confidence).average().orElse(0);
        return new MappingResult(mappings, unmappedSource, unmappedTarget, overall);
    }

    private SchemaTable findBestTarget(SchemaTable source, List<SchemaTable> targets, Set<String> excluded) {
        return targets.stream().filter(t -> !excluded.contains(t.fullName()))
            .max(Comparator.comparingDouble(t -> tableSimilarity(source, t))).orElse(null);
    }

    private double tableSimilarity(SchemaTable a, SchemaTable b) {
        double nameSim = stringSimilarity(a.name().toLowerCase(), b.name().toLowerCase());
        double colCountSim = 1.0 - Math.abs(a.columns().size() - b.columns().size()) / (double) Math.max(a.columns().size(), b.columns().size());
        return 0.6 * nameSim + 0.4 * colCountSim;
    }

    private ColumnMappingSpec findBestColumnMapping(SchemaField source, List<SchemaField> targets, double threshold) {
        ColumnMappingSpec best = null;
        double bestScore = threshold;

        for (SchemaField target : targets) {
            double score = columnSimilarity(source, target);
            if (score > bestScore) {
                bestScore = score;
                String rule = source.name().equalsIgnoreCase(target.name()) ? "direct" :
                    source.dataType().equalsIgnoreCase(target.dataType()) ? "rename" : "cast";
                best = ColumnMappingSpec.builder()
                    .sourceColumn(source.name()).targetColumn(target.name())
                    .sourceDataType(source.dataType()).targetDataType(target.dataType())
                    .nullable(source.nullable()).primaryKey(source.primaryKey())
                    .transformationRule(rule).confidence(bestScore).matchType("semantic")
                    .metadata(Map.of("sourceTable", source.tableName(), "targetTable", target.tableName()))
                    .build();
            }
        }
        return best;
    }

    private double columnSimilarity(SchemaField a, SchemaField b) {
        double nameSim = stringSimilarity(a.name().toLowerCase(), b.name().toLowerCase());
        double typeSim = areTypesCompatible(a.dataType(), b.dataType()) ? 1 : 0.3;
        double pkSim = a.primaryKey() == b.primaryKey() ? 1 : 0.5;
        return 0.5 * nameSim + 0.3 * typeSim + 0.2 * pkSim;
    }

    private double stringSimilarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(a, b) / maxLen;
    }

    private int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i; int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j-1]), a.charAt(i-1) == b.charAt(j-1) ? nw : nw + 1);
                nw = costs[j]; costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private boolean areTypesCompatible(String a, String b) {
        String s = a.toLowerCase(), t = b.toLowerCase();
        if (s.equals(t)) return true;
        Set<String> numeric = Set.of("int","integer","bigint","smallint","tinyint","decimal","numeric","float","double","real");
        Set<String> string = Set.of("varchar","char","text","clob","string");
        Set<String> temporal = Set.of("date","time","timestamp","datetime");
        return (numeric.contains(s) && numeric.contains(t)) || (string.contains(s) && string.contains(t))
            || (temporal.contains(s) && temporal.contains(t)) || (s.contains("bool") && t.contains("bool"));
    }

    private String buildSchemaContext(List<SchemaTable> source, List<SchemaTable> target) {
        StringBuilder sb = new StringBuilder("SOURCE:\n");
        for (SchemaTable t : source) { sb.append("Table ").append(t.name()).append(":\n");
            for (SchemaField c : t.columns()) sb.append("  ").append(c.name()).append(" ").append(c.dataType()).append("\n"); }
        sb.append("TARGET:\n");
        for (SchemaTable t : target) { sb.append("Table ").append(t.name()).append(":\n");
            for (SchemaField c : t.columns()) sb.append("  ").append(c.name()).append(" ").append(c.dataType()).append("\n"); }
        return sb.toString();
    }

    private void mergeLLMSuggestions(List<TableMappingSpec> mappings, List<Map<String,String>> suggestions) {
        // Merging logic would go here
    }

    private List<UnmappedField> findUnmapped(List<SchemaTable> tables, Set<String> mapped) {
        List<UnmappedField> result = new ArrayList<>();
        for (SchemaTable t : tables) {
            if (!mapped.contains(t.fullName())) {
                for (SchemaField c : t.columns())
                    result.add(new UnmappedField(t.name(), c.name(), c.dataType(), "No matching target found"));
            }
        }
        return result;
    }

    public record SchemaTable(String name, String schema, String catalog, String fullName, List<SchemaField> columns) {}
    public record SchemaField(String name, String dataType, boolean nullable, boolean primaryKey, String tableName, String description) {}
    public record MappingOptions(double confidenceThreshold, boolean useLLM, boolean autoApprove, Map<String,String> customRules) {}
    public record MappingResult(List<TableMappingSpec> mappings, List<UnmappedField> unmappedSource, List<UnmappedField> unmappedTarget, double overallConfidence) {}
    public record UnmappedField(String table, String column, String dataType, String reason) {}
}
