package io.debezium.v4.ai.training;

import io.debezium.v4.core.model.ColumnMappingSpec;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FeedbackTrainer {
    private final List<FeedbackEntry> feedbackHistory = new CopyOnWriteArrayList<>();
    private final Map<String, WeightConfig> weights = new HashMap<>();

    public FeedbackTrainer() {
        weights.put("name", new WeightConfig(0.5, 0.01));
        weights.put("type", new WeightConfig(0.3, 0.01));
        weights.put("nullable", new WeightConfig(0.15, 0.005));
        weights.put("primaryKey", new WeightConfig(0.05, 0.005));
    }

    public void recordFeedback(FeedbackEntry entry) {
        feedbackHistory.add(entry);
        adjustWeights(entry);
    }

    private void adjustWeights(FeedbackEntry entry) {
        if (!entry.accepted()) return;
        double delta = entry.confidence() > 0.8 ? 0.005 : entry.confidence() > 0.5 ? 0.002 : -0.002;
        weights.computeIfPresent("name", (k, w) -> new WeightConfig(w.weight + delta, w.learningRate));
        weights.computeIfPresent("type", (k, w) -> new WeightConfig(w.weight - delta * 0.5, w.learningRate));
    }

    public Map<String, WeightConfig> getWeights() { return Map.copyOf(weights); }

    public void resetWeights() {
        weights.put("name", new WeightConfig(0.5, 0.01));
        weights.put("type", new WeightConfig(0.3, 0.01));
        weights.put("nullable", new WeightConfig(0.15, 0.005));
        weights.put("primaryKey", new WeightConfig(0.05, 0.005));
    }

    public TrainingStats getStats() {
        long total = feedbackHistory.size();
        long accepted = feedbackHistory.stream().filter(FeedbackEntry::accepted).count();
        return new TrainingStats(total, accepted, total > 0 ? (double) accepted / total : 0, weights);
    }

    public record FeedbackEntry(ColumnMappingSpec mapping, boolean accepted, double confidence, String userId, long timestamp) {}
    public record WeightConfig(double weight, double learningRate) {}
    public record TrainingStats(long totalFeedbacks, long acceptedFeedbacks, double acceptanceRate, Map<String, WeightConfig> weights) {}
}
