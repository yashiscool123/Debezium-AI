package io.debezium.v4.monitoring;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class HealthIndicator {

    private final Map<String, HealthCheck> checks = new ConcurrentHashMap<>();
    private final Map<String, HealthResult> results = new ConcurrentHashMap<>();

    public void register(String name, HealthCheck check) {
        checks.put(name, check);
    }

    public void unregister(String name) {
        checks.remove(name);
        results.remove(name);
    }

    public HealthResult check(String name) {
        HealthCheck check = checks.get(name);
        if (check == null) return new HealthResult(name, Status.UNKNOWN, "No check registered", Instant.now(), Map.of());
        try {
            HealthResult result = check.check();
            results.put(name, result);
            return result;
        } catch (Exception e) {
            HealthResult result = new HealthResult(name, Status.DOWN, e.getMessage(), Instant.now(), Map.of());
            results.put(name, result);
            return result;
        }
    }

    public Map<String, Object> checkAll() {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allUp = true;

        for (Map.Entry<String, HealthCheck> entry : checks.entrySet()) {
            HealthResult r = check(entry.getKey());
            details.put(entry.getKey(), Map.of("status", r.status().name(), "message", r.message(), "timestamp", r.timestamp().toString()));
            if (r.status() != Status.UP) allUp = false;
        }

        report.put("status", allUp ? "UP" : "DEGRADED");
        report.put("overall", allUp ? Status.UP : Status.DEGRADED);
        report.put("checks", details);
        report.put("timestamp", Instant.now().toString());
        return report;
    }

    @FunctionalInterface
    public interface HealthCheck {
        HealthResult check();
    }

    public record HealthResult(String name, Status status, String message, Instant timestamp, Map<String, Object> details) {}
    public enum Status { UP, DOWN, DEGRADED, UNKNOWN }
}
