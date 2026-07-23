package io.debezium.v4.ai.quality;

import io.debezium.v4.ai.embeddings.EmbeddingService;
import io.debezium.v4.ai.vector.VectorStore;
import io.debezium.v4.core.model.DataQualityRuleSpec;
import io.debezium.v4.core.model.QualityCheckResult;
import io.debezium.v4.ai.llm.LLMService;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AIQualityService {

    private final LLMService llmService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private static final int ANOMALY_TOP_K = 5;

    public AIQualityService(LLMService llmService, EmbeddingService embeddingService, VectorStore vectorStore) {
        this.llmService = llmService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public AIQualityService(LLMService llmService, EmbeddingService embeddingService) {
        this(llmService, embeddingService, new VectorStore());
    }

    // --- LLM-based validation ---

    public QualityCheckResult validateWithLLM(DataQualityRuleSpec rule, String pipelineId,
                                               List<Map<String, String>> records) {
        long start = System.currentTimeMillis();
        List<String> details = new ArrayList<>();
        long failed = 0;

        String instruction = buildValidationPrompt(rule, records);
        String response = llmService.ask(instruction);
        var parsed = parseLLMResponse(response);

        long evaluated = records.size();
        failed = parsed.failedCount;
        details = parsed.details;
        boolean passed = failed == 0;

        if (rule.threshold() != null) {
            double failRate = evaluated > 0 ? (double) failed / evaluated * 100.0 : 0;
            passed = failRate <= rule.threshold();
        }

        return QualityCheckResult.builder()
            .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
            .scope(rule.scope()).column(rule.column()).passed(passed)
            .evaluatedRows(evaluated).failedRows(failed)
            .actualValue(evaluated > 0 ? (double) failed / evaluated * 100.0 : 0)
            .severity(rule.severity()).details(details)
            .sampleFailures(Map.of("llmResponse", response))
            .durationMs(System.currentTimeMillis() - start).build();
    }

    // --- Embedding-based anomaly detection ---

    public QualityCheckResult detectAnomalies(DataQualityRuleSpec rule, String pipelineId,
                                               List<Map<String, String>> records) {
        long start = System.currentTimeMillis();
        List<String> details = new ArrayList<>();
        long failed = 0;
        List<Map<String, Object>> anomalies = new ArrayList<>();

        String column = rule.column();

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);
            String textToEmbed = column != null ? record.getOrDefault(column, "") : record.toString();

            if (textToEmbed.isEmpty()) continue;

            float[] vec = embeddingService.embed(textToEmbed);

            var similar = vectorStore.search(vec, ANOMALY_TOP_K);
            double avgSimilarity = similar.stream().mapToDouble(s -> s.score()).average().orElse(1.0);

            if (avgSimilarity < 0.6) {
                failed++;
                details.add("Row " + (i + 1) + ": anomaly score " + String.format("%.3f", avgSimilarity)
                    + " (below 0.6 threshold)");
                if (anomalies.size() < 5) {
                    anomalies.add(Map.of("row", i + 1, "value", textToEmbed, "score", avgSimilarity));
                }
            }

            vectorStore.insert("rec_" + pipelineId + "_" + i, vec, Map.of(
                "pipelineId", pipelineId, "row", String.valueOf(i), "column", column != null ? column : "*"
            ));
        }

        long evaluated = records.size();
        boolean passed = failed == 0;

        if (rule.threshold() != null) {
            double failRate = evaluated > 0 ? (double) failed / evaluated * 100.0 : 0;
            passed = failRate <= rule.threshold();
        }

        return QualityCheckResult.builder()
            .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
            .scope(rule.scope()).column(rule.column()).passed(passed)
            .evaluatedRows(evaluated).failedRows(failed)
            .actualValue(evaluated > 0 ? (double) failed / evaluated * 100.0 : 0)
            .severity(rule.severity()).details(details)
            .sampleFailures(Map.of("anomalies", anomalies))
            .durationMs(System.currentTimeMillis() - start).build();
    }

    // --- Schema drift detection ---

    public QualityCheckResult detectSchemaDrift(DataQualityRuleSpec rule, String pipelineId,
                                                 List<Map<String, String>> currentRecords,
                                                 List<Map<String, String>> baselineRecords) {
        long start = System.currentTimeMillis();

        if (baselineRecords == null || baselineRecords.isEmpty()) {
            for (Map<String, String> rec : currentRecords) {
                float[] vec = embeddingService.embed(rec.toString());
                vectorStore.insert("drift_baseline_" + pipelineId + "_" + UUID.randomUUID(), vec, Map.of(
                    "pipelineId", pipelineId, "type", "baseline"
                ));
            }
            return QualityCheckResult.builder()
                .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
                .scope("PIPELINE").passed(true).evaluatedRows(currentRecords.size())
                .failedRows(0).severity(rule.severity())
                .details(List.of("Baseline established with " + currentRecords.size() + " records"))
                .durationMs(System.currentTimeMillis() - start).build();
        }

        List<String> details = new ArrayList<>();
        long failed = 0;

        float[] baselineCentroid = computeCentroid(baselineRecords);
        float[] currentCentroid = computeCentroid(currentRecords);
        double driftScore = 1.0 - embeddingService.similarity(baselineCentroid, currentCentroid);

        if (driftScore > 0.2) {
            failed = (long) (driftScore * currentRecords.size());
            details.add("Schema drift detected: " + String.format("%.2f", driftScore * 100) + "% dissimilar");
            details.add("Baseline records: " + baselineRecords.size() + ", Current: " + currentRecords.size());
        } else {
            details.add("No significant drift (score: " + String.format("%.3f", driftScore) + ")");
        }

        boolean passed = driftScore <= 0.2;

        return QualityCheckResult.builder()
            .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
            .scope("PIPELINE").passed(passed).evaluatedRows(currentRecords.size())
            .failedRows(failed).actualValue(driftScore * 100).severity(rule.severity())
            .details(details).durationMs(System.currentTimeMillis() - start).build();
    }

    // --- Semantic consistency check ---

    public QualityCheckResult checkSemanticConsistency(DataQualityRuleSpec rule, String pipelineId,
                                                        List<Map<String, String>> records) {
        long start = System.currentTimeMillis();
        List<String> details = new ArrayList<>();
        long failed = 0;

        String column = rule.column();
        Set<String> uniqueValues = records.stream()
            .map(r -> column != null ? r.getOrDefault(column, "") : r.toString())
            .filter(v -> !v.isEmpty())
            .collect(Collectors.toSet());

        if (uniqueValues.size() < 2) {
            return QualityCheckResult.builder()
                .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
                .scope(rule.scope()).column(rule.column()).passed(true)
                .evaluatedRows(records.size()).failedRows(0).severity(rule.severity())
                .details(List.of("Not enough unique values to compare (" + uniqueValues.size() + ")"))
                .durationMs(System.currentTimeMillis() - start).build();
        }

        double totalSimilarity = 0;
        int comparisons = 0;
        List<String> valueList = new ArrayList<>(uniqueValues);

        for (int i = 0; i < Math.min(valueList.size(), 10); i++) {
            for (int j = i + 1; j < Math.min(valueList.size(), 10); j++) {
                float[] vecA = embeddingService.embed(valueList.get(i));
                float[] vecB = embeddingService.embed(valueList.get(j));
                totalSimilarity += embeddingService.similarity(vecA, vecB);
                comparisons++;
            }
        }

        double avgSimilarity = comparisons > 0 ? totalSimilarity / comparisons : 1.0;

        if (avgSimilarity < 0.7) {
            failed = (long) ((1.0 - avgSimilarity) * records.size());
            details.add("Low semantic consistency: " + String.format("%.2f", avgSimilarity * 100) + "% average similarity");
            details.add("Found " + uniqueValues.size() + " distinct values with high variance");
        } else {
            details.add("Semantic consistency OK: " + String.format("%.1f", avgSimilarity * 100) + "% average similarity");
        }

        boolean passed = avgSimilarity >= 0.7;

        return QualityCheckResult.builder()
            .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
            .scope(rule.scope()).column(rule.column()).passed(passed)
            .evaluatedRows(records.size()).failedRows(failed)
            .actualValue(avgSimilarity * 100).severity(rule.severity())
            .details(details).durationMs(System.currentTimeMillis() - start).build();
    }

    // --- Helpers ---

    private String buildValidationPrompt(DataQualityRuleSpec rule, List<Map<String, String>> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a data quality validation assistant. ");
        sb.append("Validate the following records against this rule:\n");
        sb.append("Rule: ").append(rule.name()).append("\n");
        sb.append("Description: ").append(rule.description() != null ? rule.description() : "").append("\n");
        sb.append("Expected: ").append(rule.expectedValue() != null ? rule.expectedValue() : "any valid value").append("\n");
        if (rule.column() != null) sb.append("Column: ").append(rule.column()).append("\n");
        if (rule.predicate() != null) sb.append("Predicate: ").append(rule.predicate()).append("\n");
        sb.append("Configuration: ").append(rule.configuration()).append("\n\n");
        sb.append("Records (").append(records.size()).append(" rows):\n");

        int limit = Math.min(records.size(), 20);
        for (int i = 0; i < limit; i++) {
            sb.append("Row ").append(i + 1).append(": ").append(records.get(i)).append("\n");
        }

        sb.append("\nReturn a JSON object with:\n");
        sb.append("- \"valid\": boolean (true if all records pass)\n");
        sb.append("- \"failedCount\": number of failed records\n");
        sb.append("- \"details\": array of string descriptions of each failure\n");
        sb.append("- \"failedRows\": array of row numbers that failed\n");
        return sb.toString();
    }

    private LLMValidationResult parseLLMResponse(String response) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(response);
            boolean valid = root.path("valid").asBoolean(true);
            int failedCount = root.path("failedCount").asInt(0);
            var detailsArr = root.path("details");
            List<String> details = new ArrayList<>();
            if (detailsArr.isArray()) {
                detailsArr.forEach(d -> details.add(d.asText()));
            }
            return new LLMValidationResult(valid, failedCount, details);
        } catch (Exception e) {
            return new LLMValidationResult(true, 0, List.of("LLM response parse failed: " + e.getMessage()));
        }
    }

    private float[] computeCentroid(List<Map<String, String>> records) {
        if (records.isEmpty()) return new float[384];
        float[][] allVecs = new float[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            allVecs[i] = embeddingService.embed(records.get(i).toString());
        }
        if (allVecs[0] == null) return new float[384];
        float[] centroid = new float[allVecs[0].length];
        for (float[] vec : allVecs) {
            for (int j = 0; j < vec.length; j++) {
                centroid[j] += vec[j];
            }
        }
        for (int j = 0; j < centroid.length; j++) {
            centroid[j] /= allVecs.length;
        }
        return centroid;
    }

    public void clearBaseline(String pipelineId) {
        vectorStore.searchWithFilter(new float[384], 1000, Map.of("pipelineId", pipelineId))
            .forEach(r -> vectorStore.delete(r.entry().id()));
    }

    private record LLMValidationResult(boolean valid, int failedCount, List<String> details) {}
}
