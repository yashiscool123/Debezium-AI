package io.debezium.v4.core.engine;

import io.debezium.v4.core.model.DataQualityConfig;
import io.debezium.v4.core.model.DataQualityRuleSpec;
import io.debezium.v4.core.model.QualityCheckResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class DataQualityEngine {

    private final Map<String, List<QualityCheckResult>> resultsStore = new ConcurrentHashMap<>();
    private static final int MAX_RESULTS_PER_PIPELINE = 1000;

    private static final Map<String, BiPredicate<String, DataQualityRuleSpec>> BUILTIN_CHECKERS = Map.of(
        "NOT_NULL", (value, rule) -> value != null && !value.trim().isEmpty(),
        "NOT_EMPTY", (value, rule) -> value != null && !value.isEmpty(),
        "REGEX", (value, rule) -> {
            if (value == null) return false;
            String pattern = rule.configuration().getOrDefault("pattern", ".*");
            return Pattern.matches(pattern, value);
        },
        "MIN_LENGTH", (value, rule) -> {
            if (value == null) return false;
            int min = Integer.parseInt(rule.configuration().getOrDefault("min", "0"));
            return value.length() >= min;
        },
        "MAX_LENGTH", (value, rule) -> {
            if (value == null) return false;
            int max = Integer.parseInt(rule.configuration().getOrDefault("max", "99999"));
            return value.length() <= max;
        },
        "RANGE", (value, rule) -> {
            if (value == null) return false;
            try {
                double num = Double.parseDouble(value);
                double min = Double.parseDouble(rule.configuration().getOrDefault("min", "-Infinity"));
                double max = Double.parseDouble(rule.configuration().getOrDefault("max", "Infinity"));
                return num >= min && num <= max;
            } catch (NumberFormatException e) {
                return false;
            }
        },
        "EQUALS", (value, rule) -> value != null && value.equals(rule.expectedValue()),
        "UNIQUE", (value, rule) -> true,
        "TYPE_CHECK", (value, rule) -> {
            if (value == null) return false;
            String type = rule.configuration().getOrDefault("type", "string");
            return switch (type.toLowerCase()) {
                case "number" -> { try { Double.parseDouble(value); yield true; } catch (NumberFormatException e) { yield false; } }
                case "integer" -> { try { Long.parseLong(value); yield true; } catch (NumberFormatException e) { yield false; } }
                case "boolean" -> "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
                case "date" -> { try { java.time.LocalDate.parse(value); yield true; } catch (Exception e) { yield false; } }
                default -> true;
            };
        }
    );

    public QualityCheckResult evaluateRule(DataQualityRuleSpec rule, String pipelineId, List<Map<String, String>> sampleRecords) {
        long start = System.currentTimeMillis();
        if (!rule.enabled()) {
            return QualityCheckResult.builder()
                .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
                .scope(rule.scope()).column(rule.column()).passed(true)
                .evaluatedRows(0).failedRows(0).severity(rule.severity())
                .details(List.of("Rule disabled, skipped")).durationMs(0).build();
        }

        var checker = BUILTIN_CHECKERS.get(rule.ruleType());
        if (checker == null) {
            return QualityCheckResult.builder()
                .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
                .scope(rule.scope()).column(rule.column()).passed(true)
                .evaluatedRows(0).failedRows(0).severity(rule.severity())
                .details(List.of("Unknown rule type: " + rule.ruleType())).durationMs(System.currentTimeMillis() - start).build();
        }

        long evaluated = 0;
        long failed = 0;
        List<String> failureDetails = new ArrayList<>();
        List<Map<String, Object>> samples = new ArrayList<>();

        for (Map<String, String> record : sampleRecords) {
            String col = rule.column();
            String value = (col != null && !col.isBlank()) ? record.get(col) : record.toString();
            evaluated++;
            boolean passed = checker.test(value, rule);
            if (!passed) {
                failed++;
                String detail = col != null
                    ? "Column '" + col + "' value '" + value + "' failed " + rule.ruleType()
                    : "Record failed " + rule.ruleType() + ": " + record;
                failureDetails.add(detail);
                if (samples.size() < 5) {
                    samples.add(Map.of("row", evaluated, "column", col != null ? col : "*", "value", value));
                }
            }
        }

        Double actualValue = evaluated > 0 ? (double) failed / evaluated * 100.0 : 0.0;
        boolean passed = failed == 0;

        if (rule.threshold() != null) {
            passed = actualValue <= rule.threshold();
        }

        return QualityCheckResult.builder()
            .pipelineId(pipelineId).ruleName(rule.name()).ruleType(rule.ruleType())
            .scope(rule.scope()).column(rule.column()).passed(passed)
            .evaluatedRows(evaluated).failedRows(failed)
            .actualValue(actualValue).severity(rule.severity())
            .details(failureDetails.size() > 10 ? failureDetails.subList(0, 10) : failureDetails)
            .sampleFailures(Map.of("samples", samples, "total", failed))
            .durationMs(System.currentTimeMillis() - start).build();
    }

    public List<QualityCheckResult> runQualityCheck(String pipelineId, DataQualityConfig config, List<Map<String, String>> sampleRecords) {
        List<QualityCheckResult> results = new ArrayList<>();
        if (config == null || !config.enabled()) return results;

        for (DataQualityRuleSpec rule : config.rules()) {
            QualityCheckResult result = evaluateRule(rule, pipelineId, sampleRecords);
            results.add(result);
            if (config.failOnError() && !result.passed() && "FATAL".equals(result.severity())) {
                break;
            }
        }

        storeResults(pipelineId, results);
        return results;
    }

    public void storeResults(String pipelineId, List<QualityCheckResult> results) {
        resultsStore.merge(pipelineId, results, (old, neu) -> {
            List<QualityCheckResult> merged = new ArrayList<>(neu);
            merged.addAll(old);
            if (merged.size() > MAX_RESULTS_PER_PIPELINE) {
                merged = merged.subList(0, MAX_RESULTS_PER_PIPELINE);
            }
            return merged;
        });
    }

    public List<QualityCheckResult> getResults(String pipelineId) {
        return resultsStore.getOrDefault(pipelineId, List.of());
    }

    public List<QualityCheckResult> getLatestResults(String pipelineId, int limit) {
        var all = getResults(pipelineId);
        return all.stream()
            .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<QualityCheckResult> getFailedResults(String pipelineId) {
        return getResults(pipelineId).stream()
            .filter(r -> !r.passed())
            .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
            .collect(Collectors.toList());
    }

    public long getPassRate(String pipelineId) {
        var results = getResults(pipelineId);
        if (results.isEmpty()) return 100;
        long passed = results.stream().filter(QualityCheckResult::passed).count();
        return (passed * 100) / results.size();
    }

    public long getTotalRowsEvaluated(String pipelineId) {
        return getResults(pipelineId).stream().mapToLong(QualityCheckResult::evaluatedRows).sum();
    }

    public long getTotalFailures(String pipelineId) {
        return getResults(pipelineId).stream().mapToLong(QualityCheckResult::failedRows).sum();
    }

    public void clearResults(String pipelineId) {
        resultsStore.remove(pipelineId);
    }
}
