package io.debezium.v4.monitoring;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class AuditLogger {

    private final ConcurrentLinkedDeque<AuditEntry> log = new ConcurrentLinkedDeque<>();
    private static final int MAX_ENTRIES = 10000;

    public void log(String action, String entityType, String entityId, String userId, Map<String, Object> details) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID().toString(), action, entityType, entityId, userId, details, Instant.now()
        );
        log.addFirst(entry);
        if (log.size() > MAX_ENTRIES) log.removeLast();
    }

    public void log(String action, String entityType, String entityId, String userId) {
        log(action, entityType, entityId, userId, Map.of());
    }

    public List<AuditEntry> getLog(int limit) {
        return log.stream().limit(limit).toList();
    }

    public List<AuditEntry> getLogSince(Instant since) {
        return log.stream().filter(e -> e.timestamp().isAfter(since)).toList();
    }

    public List<AuditEntry> getLogForEntity(String entityType, String entityId) {
        return log.stream()
            .filter(e -> e.entityType().equals(entityType) && e.entityId().equals(entityId))
            .toList();
    }

    public record AuditEntry(String id, String action, String entityType, String entityId, String userId, Map<String, Object> details, Instant timestamp) {}
}
