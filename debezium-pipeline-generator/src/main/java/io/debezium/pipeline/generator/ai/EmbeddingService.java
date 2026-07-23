package io.debezium.pipeline.generator.ai;

import io.debezium.ai.embeddings.EmbeddingsModelFactory;
import io.debezium.ai.embeddings.FieldToEmbedding;
import io.debezium.ai.embeddings.metadata.EmbeddingsMetadataProvider;
import io.debezium.pipeline.generator.model.ColumnMapping;
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

    @Inject
    EmbeddingsMetadataProvider metadataProvider;

    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    private FieldToEmbedding fieldToEmbedding;

    public void initialize(String provider) {
        this.fieldToEmbedding = new FieldToEmbedding(modelFactory, metadataProvider);
        Log.infof("Initialized embedding service with provider: %s", provider);
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

    public float[] embedField(SchemaField field) {
        String cacheKey = "field:" + field.fullName().hashCode();
        return embeddingCache.computeIfAbsent(cacheKey, k -> {
            try {
                return fieldToEmbedding.embedField(field.name(), field.dataType(), field.description());
            } catch (Exception e) {
                Log.warnf("Failed to embed field %s: %s", field.name(), e.getMessage());
                return new float[0];
            }
        });
    }

    public float[] embedTable(String catalog, String schema, String table, List<SchemaField> fields) {
        String cacheKey = "table:" + catalog + "." + schema + "." + table;
        return embeddingCache.computeIfAbsent(cacheKey, k -> {
            try {
                StringBuilder text = new StringBuilder();
                text.append("Table ").append(catalog).append(".").append(schema).append(".").append(table).append("\n");
                for (SchemaField field : fields) {
                    text.append("  ").append(field.name()).append(" (").append(field.dataType()).append(")");
                    if (field.description() != null) {
                        text.append(" - ").append(field.description());
                    }
                    text.append("\n");
                }
                return fieldToEmbedding.embedText(text.toString());
            } catch (Exception e) {
                Log.warnf("Failed to embed table %s: %s", cacheKey, e.getMessage());
                return new float[0];
            }
        });
    }

    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length || vec1.length == 0) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public List<ColumnMapping> findSimilarColumns(
            List<SchemaField> sourceFields,
            List<SchemaField> targetFields,
            double threshold) {
        
        List<ColumnMapping> mappings = new ArrayList<>();
        
        // Pre-compute embeddings
        Map<SchemaField, float[]> sourceEmbeddings = new HashMap<>();
        Map<SchemaField, float[]> targetEmbeddings = new HashMap<>();
        
        for (SchemaField field : sourceFields) {
            sourceEmbeddings.put(field, embedField(field));
        }
        for (SchemaField field : targetFields) {
            targetEmbeddings.put(field, embedField(field));
        }
        
        // Find best matches
        for (SchemaField source : sourceFields) {
            float[] sourceVec = sourceEmbeddings.get(source);
            if (sourceVec.length == 0) continue;
            
            ColumnMapping bestMatch = null;
            double bestScore = threshold;
            
            for (SchemaField target : targetFields) {
                float[] targetVec = targetEmbeddings.get(target);
                if (targetVec.length == 0) continue;
                
                double similarity = cosineSimilarity(sourceVec, targetVec);
                
                // Boost score for exact name match
                if (source.name().equalsIgnoreCase(target.name())) {
                    similarity = Math.min(1.0, similarity + 0.3);
                }
                
                // Boost for compatible types
                if (areTypesCompatible(source.dataType(), target.dataType())) {
                    similarity = Math.min(1.0, similarity + 0.1);
                }
                
                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestMatch = ColumnMapping.builder()
                        .sourceColumn(source.name())
                        .targetColumn(target.name())
                        .sourceDataType(source.dataType())
                        .targetDataType(target.dataType())
                        .nullable(source.nullable())
                        .primaryKey(source.primaryKey())
                        .transformationRule(inferTransformationRule(source, target))
                        .confidenceScore(bestScore)
                        .metadata(Map.of(
                            "matchType", "semantic",
                            "sourceTable", source.tableName(),
                            "targetTable", target.tableName()
                        ))
                        .build();
                }
            }
            
            if (bestMatch != null) {
                mappings.add(bestMatch);
            }
        }
        
        return mappings;
    }

    private boolean areTypesCompatible(String sourceType, String targetType) {
        String s = sourceType.toLowerCase();
        String t = targetType.toLowerCase();
        
        // Numeric types
        if (isNumeric(s) && isNumeric(t)) return true;
        // String types
        if (isString(s) && isString(t)) return true;
        // Date/time types
        if (isTemporal(s) && isTemporal(t)) return true;
        // Boolean
        if (s.contains("bool") && t.contains("bool")) return true;
        
        return false;
    }

    private boolean isNumeric(String type) {
        return type.contains("int") || type.contains("decimal") || type.contains("numeric") ||
               type.contains("float") || type.contains("double") || type.contains("real") ||
               type.contains("number") || type.contains("bigint") || type.contains("smallint");
    }

    private boolean isString(String type) {
        return type.contains("char") || type.contains("text") || type.contains("varchar") ||
               type.contains("string") || type.contains("clob");
    }

    private boolean isTemporal(String type) {
        return type.contains("date") || type.contains("time") || type.contains("timestamp");
    }

    private TransformationRule inferTransformationRule(SchemaField source, SchemaField target) {
        String ruleType = "none";
        String expression = null;
        String description = "Direct mapping";
        
        if (!source.dataType().equalsIgnoreCase(target.dataType())) {
            ruleType = "cast";
            expression = "CAST(" + source.name() + " AS " + target.dataType() + ")";
            description = "Cast from " + source.dataType() + " to " + target.dataType();
        }
        
        if (!source.name().equalsIgnoreCase(target.name())) {
            ruleType = "rename";
            expression = source.name() + " AS " + target.name();
            description = "Rename " + source.name() + " to " + target.name();
        }
        
        return TransformationRule.builder()
            .type(ruleType)
            .expression(expression)
            .smtClass(null)
            .parameters(Map.of())
            .description(description)
            .build();
    }

    public CompletableFuture<List<ColumnMapping>> findSimilarColumnsAsync(
            List<SchemaField> sourceFields,
            List<SchemaField> targetFields,
            double threshold) {
        return CompletableFuture.supplyAsync(() -> findSimilarColumns(sourceFields, targetFields, threshold));
    }

    public void clearCache() {
        embeddingCache.clear();
        Log.info("Embedding cache cleared");
    }

    public int cacheSize() {
        return embeddingCache.size();
    }
}