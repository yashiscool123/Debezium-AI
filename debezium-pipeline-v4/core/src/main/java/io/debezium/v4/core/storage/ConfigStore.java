package io.debezium.v4.core.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ConfigStore implements NoSqlStore<Map<String, Object>> {

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> fieldIndexes = new ConcurrentHashMap<>();

    @Override
    public void insert(String id, Map<String, Object> entity) {
        store.put(id, new HashMap<>(entity));
    }

    @Override
    public void insert(String id, Map<String, Object> entity, Map<String, Object> metadata) {
        var enriched = new HashMap<>(entity);
        if (metadata != null) enriched.put("_metadata", metadata);
        store.put(id, enriched);
    }

    @Override
    public Optional<Map<String, Object>> get(String id) {
        return Optional.ofNullable(store.get(id)).map(HashMap::new);
    }

    @Override
    public List<Map<String, Object>> find(Map<String, Object> filters) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (var entry : store.entrySet()) {
            boolean match = true;
            for (var f : filters.entrySet()) {
                Object val = entry.getValue().get(f.getKey());
                if (!Objects.equals(val, f.getValue())) { match = false; break; }
            }
            if (match) results.add(new HashMap<>(entry.getValue()));
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return store.values().stream().map(HashMap::new).toList();
    }

    @Override
    public List<Map<String, Object>> findSorted(Map<String, Object> filters, String sortField, boolean asc, int limit) {
        return find(filters).stream()
            .sorted((a, b) -> {
                Object va = a.get(sortField);
                Object vb = b.get(sortField);
                if (va instanceof Comparable ca && vb instanceof Comparable cb) {
                    @SuppressWarnings("unchecked")
                    int cmp = ca.compareTo(cb);
                    return asc ? cmp : -cmp;
                }
                return 0;
            })
            .limit(limit)
            .toList();
    }

    @Override
    public Map<String, Object> update(String id, Map<String, Object> entity) {
        store.put(id, new HashMap<>(entity));
        return entity;
    }

    @Override
    public void delete(String id) { store.remove(id); }

    @Override
    public long count() { return store.size(); }

    @Override
    public boolean exists(String id) { return store.containsKey(id); }

    @Override
    public void createIndex(String field) {
        var index = new HashMap<String, List<String>>();
        for (var entry : store.entrySet()) {
            Object val = entry.getValue().get(field);
            if (val != null) {
                index.computeIfAbsent(val.toString(), k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        fieldIndexes.put(field, index);
    }

    @Override
    public void dropIndex(String field) { fieldIndexes.remove(field); }

    @Override
    public void clear() { store.clear(); fieldIndexes.clear(); }

    public List<String> findByField(String field, Object value) {
        var index = fieldIndexes.get(field);
        if (index != null) return index.getOrDefault(value.toString(), List.of());
        return store.entrySet().stream()
            .filter(e -> Objects.equals(e.getValue().get(field), value))
            .map(Map.Entry::getKey)
            .toList();
    }
}
