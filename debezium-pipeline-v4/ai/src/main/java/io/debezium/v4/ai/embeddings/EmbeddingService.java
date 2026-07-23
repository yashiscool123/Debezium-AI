package io.debezium.v4.ai.embeddings;

import io.debezium.v4.core.model.ColumnMappingSpec;
import io.debezium.v4.core.model.SchemaField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EmbeddingService {
    private final EmbeddingProvider provider;
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    public EmbeddingService(EmbeddingProvider provider) { this.provider = provider; }

    public float[] embed(String text) {
        String key = "t:" + text.hashCode();
        return cache.computeIfAbsent(key, k -> provider.embed(text));
    }

    public float[] embedField(String fieldName, String dataType, String description) {
        String text = fieldName + " " + dataType + (description != null ? " " + description : "");
        return embed(text);
    }

    public double similarity(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public List<SimilarityResult> findSimilar(SimilarityQuery query, List<SimilarityQuery> targets, int topK) {
        float[] queryVec = embed(query.text());
        return targets.parallelStream().map(t -> {
            float[] targetVec = embed(t.text());
            return new SimilarityResult(t, similarity(queryVec, targetVec));
        }).filter(r -> r.score() > 0).sorted((a,b) -> Double.compare(b.score(), a.score())).limit(topK).collect(Collectors.toList());
    }

    public void clearCache() { cache.clear(); }
    public int cacheSize() { return cache.size(); }

    public interface EmbeddingProvider {
        float[] embed(String text);
        default int dimensions() { return 384; }
    }

    public record SimilarityQuery(String id, String text, Map<String,String> metadata) {}
    public record SimilarityResult(SimilarityQuery target, double score) {}
}
