package io.debezium.v4.core.storage;

import java.time.Instant;
import java.util.*;

public interface NoSqlStore<T> {
    void insert(String id, T entity);
    void insert(String id, T entity, Map<String, Object> metadata);
    Optional<T> get(String id);
    List<T> find(Map<String, Object> filters);
    List<T> findAll();
    List<T> findSorted(Map<String, Object> filters, String sortField, boolean asc, int limit);
    T update(String id, T entity);
    void delete(String id);
    long count();
    boolean exists(String id);
    void createIndex(String field);
    void dropIndex(String field);
    void clear();
}

public interface JobHistoryStore extends NoSqlStore<JobRun> {
    List<JobRun> findByPipelineId(String pipelineId);
    List<JobRun> findByStatus(JobRun.Status status);
    List<JobRun> findByTimeRange(Instant from, Instant to);
    List<JobRun> findRecent(int limit);
}
