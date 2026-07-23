package io.debezium.v4.core.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class MongoDbJobHistoryStore implements JobHistoryStore {

    private final Map<String, JobRun> store = new ConcurrentHashMap<>();
    private final Map<String, List<String>> pipelineIndex = new ConcurrentHashMap<>();
    private final Map<JobRun.Status, List<String>> statusIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>> fieldIndexes = new ConcurrentHashMap<>();

    @Override
    public void insert(String id, JobRun entity) {
        store.put(id, entity);
        index(id, entity);
    }

    @Override
    public void insert(String id, JobRun entity, Map<String, Object> metadata) {
        insert(id, entity);
    }

    @Override
    public Optional<JobRun> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<JobRun> find(Map<String, Object> filters) {
        return store.values().stream()
            .filter(j -> filters.entrySet().stream()
                .allMatch(e -> {
                    Object val = switch (e.getKey()) {
                        case "pipelineId" -> j.pipelineId();
                        case "status" -> j.status().name();
                        case "tenantId" -> j.tenantId();
                        case "triggerType" -> j.triggerType();
                        default -> null;
                    };
                    return val != null && val.equals(e.getValue());
                }))
            .collect(Collectors.toList());
    }

    @Override
    public List<JobRun> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<JobRun> findSorted(Map<String, Object> filters, String sortField, boolean asc, int limit) {
        return find(filters).stream()
            .sorted((a, b) -> {
                Comparable<?> ca = switch (sortField) {
                    case "startedAt" -> a.startedAt();
                    case "completedAt" -> a.completedAt();
                    case "durationMs" -> a.durationMs();
                    case "recordsProcessed" -> a.recordsProcessed();
                    case "status" -> a.status().name();
                    default -> a.startedAt();
                };
                Comparable<?> cb = switch (sortField) {
                    case "startedAt" -> b.startedAt();
                    case "completedAt" -> b.completedAt();
                    case "durationMs" -> b.durationMs();
                    case "recordsProcessed" -> b.recordsProcessed();
                    case "status" -> b.status().name();
                    default -> b.startedAt();
                };
                @SuppressWarnings("unchecked")
                int cmp = ((Comparable) ca).compareTo(cb);
                return asc ? cmp : -cmp;
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public JobRun update(String id, JobRun entity) {
        store.put(id, entity);
        return entity;
    }

    @Override
    public void delete(String id) {
        JobRun removed = store.remove(id);
        if (removed != null) {
            deindex(id, removed);
        }
    }

    @Override
    public long count() { return store.size(); }

    @Override
    public boolean exists(String id) { return store.containsKey(id); }

    @Override
    public void createIndex(String field) { /* no-op in mem */ }

    @Override
    public void dropIndex(String field) { fieldIndexes.remove(field); }

    @Override
    public void clear() { store.clear(); pipelineIndex.clear(); statusIndex.clear(); fieldIndexes.clear(); }

    @Override
    public List<JobRun> findByPipelineId(String pipelineId) {
        return find(Map.of("pipelineId", pipelineId));
    }

    @Override
    public List<JobRun> findByStatus(JobRun.Status status) {
        return find(Map.of("status", status.name()));
    }

    @Override
    public List<JobRun> findByTimeRange(Instant from, Instant to) {
        return store.values().stream()
            .filter(j -> !j.startedAt().isBefore(from) && !j.startedAt().isAfter(to))
            .collect(Collectors.toList());
    }

    @Override
    public List<JobRun> findRecent(int limit) {
        return store.values().stream()
            .sorted((a, b) -> b.startedAt().compareTo(a.startedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    private void index(String id, JobRun entity) {
        pipelineIndex.computeIfAbsent(entity.pipelineId(), k -> new ArrayList<>()).add(id);
        statusIndex.computeIfAbsent(entity.status(), k -> new ArrayList<>()).add(id);
    }

    private void deindex(String id, JobRun entity) {
        Optional.ofNullable(pipelineIndex.get(entity.pipelineId())).ifPresent(l -> l.remove(id));
        Optional.ofNullable(statusIndex.get(entity.status())).ifPresent(l -> l.remove(id));
    }
}
