package io.debezium.v4.ai.vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VectorStore {
    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    public void insert(String id, float[] vector, Map<String,String> metadata) {
        store.put(id, new VectorEntry(id, vector, metadata, System.currentTimeMillis()));
    }

    public void insertBatch(List<VectorEntry> entries) {
        entries.forEach(e -> store.put(e.id(), e));
    }

    public void delete(String id) { store.remove(id); }

    public List<SearchResult> search(float[] query, int topK) {
        return store.values().parallelStream()
            .map(e -> new SearchResult(e, cosineSimilarity(query, e.vector())))
            .filter(r -> r.score() > 0)
            .sorted((a,b) -> Double.compare(b.score(), a.score()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    public List<SearchResult> searchWithFilter(float[] query, int topK, Map<String,String> filter) {
        return store.values().parallelStream()
            .filter(e -> filter.entrySet().stream().allMatch(f -> e.metadata().getOrDefault(f.getKey(), "").equals(f.getValue())))
            .map(e -> new SearchResult(e, cosineSimilarity(query, e.vector())))
            .filter(r -> r.score() > 0)
            .sorted((a,b) -> Double.compare(b.score(), a.score()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    public int size() { return store.size(); }
    public void clear() { store.clear(); }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public record VectorEntry(String id, float[] vector, Map<String,String> metadata, long timestamp) {}
    public record SearchResult(VectorEntry entry, double score) {}
}
