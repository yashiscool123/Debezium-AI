package io.debezium.pipeline.generator.service;

import io.debezium.ai.embeddings.EmbeddingsModelFactory;
import io.debezium.ai.embeddings.FieldToEmbedding;
import io.debezium.ai.embeddings.metadata.EmbeddingsMetadataProvider;
import io.debezium.pipeline.generator.model.SchemaField;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class EmbeddingService {

    @Inject
    EmbeddingsModelFactory modelFactory;

    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    private FieldToEmbedding fieldToEmbedding;

    public void initialize(String provider, Map<String, String> config) {
        this.fieldToEmbedding = modelFactory.create(provider, config);
        Log.infof("Initialized embedding service with provider: %s", provider);
    }

    public float[] embedField(SchemaField field) {
        String cacheKey = buildCacheKey(field);
        
        return embeddingCache.computeIfAbsent(cacheKey, k -> {
            try {
                return fieldToEmbedding.embed(field);
            } catch (Exception e) {
                Log.warnf("Failed to embed field %s: %s", field.name(), e.getMessage());
                return new float[0];
            }
        });
    }

    public float[] embedText(String text) {
        String cacheKey = "text:" + text.hashCode();
        
        return embeddingCache.computeIfAbsent(cacheKey, k -> {
            try {
                return fieldToEmbedding.embedText(text);
            } catch (Exception e) {
                Log.warnf("Failed to embed text: %s", e.getMessage());
                return new float[0];
            }
        });
    }

    public List<EmbeddingResult> findSimilar(SchemaField sourceField, List<SchemaField> targetFields, int topK) {
        float[] sourceEmbedding = embedField(sourceField);
        if (sourceEmbedding.length == 0) {
            return List.of();
        }

        return targetFields.parallelStream()
            .map(target -> {
                float[] targetEmbedding = embedField(target);
                if (targetEmbedding.length == 0) {
                    return new EmbeddingResult(target, 0.0f);
                }
                float similarity = cosineSimilarity(sourceEmbedding, targetEmbedding);
                return new EmbeddingResult(target, similarity);
            })
            .filter(r -> r.similarity() > 0.0f)
            .sorted(Comparator.comparing(EmbeddingResult::similarity).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    public List<EmbeddingResult> findSimilarText(String sourceText, List<String> targetTexts, int topK) {
        float[] sourceEmbedding = embedText(sourceText);
        if (sourceEmbedding.length == 0) {
            return List.of();
        }

        return targetTexts.parallelStream()
            .map(target -> {
                float[] targetEmbedding = embedText(target);
                if (targetEmbedding.length == 0) {
                    return new EmbeddingResult(target, 0.0f);
                }
                float similarity = cosineSimilarity(sourceEmbedding, targetEmbedding);
                return new EmbeddingResult(target, similarity);
            })
            .filter(r -> r.similarity() > 0.0f)
            .sorted(Comparator.comparing(EmbeddingResult::similarity).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    public CompletableFuture<float[]> embedFieldAsync(SchemaField field) {
        return CompletableFuture.supplyAsync(() -> embedField(field));
    }

    public CompletableFuture<List<EmbeddingResult>> findSimilarAsync(SchemaField sourceField, List<SchemaField> targetFields, int topK) {
        return CompletableFuture.supplyAsync(() -> findSimilar(sourceField, targetFields, topK));
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String buildCacheKey(SchemaField field) {
        return String.format("%s.%s.%s:%s:%d:%d",
            field.catalog(), field.schema(), field.tableName(),
            field.name(), field.precision(), field.scale()
        );
    }

    public void clearCache() {
        embeddingCache.clear();
        Log.info("Embedding cache cleared");
    }

    public int cacheSize() {
        return embeddingCache.size();
    }

    public record EmbeddingResult(SchemaField field, float similarity) {
        public EmbeddingResult(String text, float similarity) {
            this(null, similarity);
        }
    }

    public record EmbeddingMetadata(
        String modelName,
        int dimensions,
        String provider,
        long cacheSize
    ) {
        public static EmbeddingMetadata from(EmbeddingService service, String provider) {
            return new EmbeddingMetadata(
                "unknown",
                384, // default for MiniLM
                provider,
                service.cacheSize()
            );
        }
    }
}