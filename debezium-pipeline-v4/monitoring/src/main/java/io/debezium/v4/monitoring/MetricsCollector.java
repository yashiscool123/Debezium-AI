package io.debezium.v4.monitoring;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class MetricsCollector {

    private final Map<String, MetricValue> metrics = new ConcurrentHashMap<>();
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final List<Alert> alertHistory = Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_ALERT_HISTORY = 1000;

    public void record(String name, long value) {
        metrics.compute(name, (k, v) -> {
            if (v == null) return new MetricValue(name, value, value, value, value, 1, Instant.now());
            return new MetricValue(name, Math.min(v.min(), value), Math.max(v.max(), value),
                v.sum() + value, v.count() + 1, Instant.now());
        });
    }

    public void gauge(String name, long value) {
        metrics.put(name, new MetricValue(name, value, value, value, 1, Instant.now()));
    }

    public MetricValue getMetric(String name) {
        return metrics.get(name);
    }

    public Map<String, MetricValue> getAllMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalMetrics", metrics.size());
        summary.put("timestamp", Instant.now().toString());

        Map<String, Object> aggregates = new LinkedHashMap<>();
        metrics.forEach((name, mv) -> {
            aggregates.put(name, Map.of(
                "current", mv.value(),
                "min", mv.min(),
                "max", mv.max(),
                "avg", mv.count() > 0 ? (double) mv.sum() / mv.count() : 0,
                "count", mv.count()
            ));
        });
        summary.put("aggregates", aggregates);
        summary.put("recentAlerts", alertHistory.stream()
            .filter(a -> a.timestamp().isAfter(Instant.now().minusSeconds(3600)))
            .count());
        return summary;
    }

    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.name(), rule);
    }

    public void removeAlertRule(String name) {
        alertRules.remove(name);
    }

    public Map<String, Object> evaluateAlerts() {
        Map<String, Object> results = new LinkedHashMap<>();
        List<Alert> triggered = new ArrayList<>();

        for (AlertRule rule : alertRules.values()) {
            MetricValue mv = metrics.get(rule.metricName());
            if (mv == null) continue;

            boolean fired = switch (rule.operator()) {
                case GREATER_THAN -> mv.value() > rule.threshold();
                case LESS_THAN -> mv.value() < rule.threshold();
                case EQUAL_TO -> mv.value() == rule.threshold();
                case CHANGED -> mv.count() > 1;
            };

            if (fired) {
                Alert alert = new Alert(rule.name(), rule.metricName(), mv.value(),
                    rule.threshold(), rule.severity(), Instant.now());
                triggered.add(alert);
                alertHistory.add(alert);
                if (alertHistory.size() > MAX_ALERT_HISTORY) {
                    alertHistory.removeFirst();
                }
            }
        }

        results.put("alertsTriggered", triggered.size());
        results.put("alerts", triggered);
        results.put("timestamp", Instant.now().toString());
        return results;
    }

    public List<Alert> getAlertHistory() {
        return List.copyOf(alertHistory);
    }

    public void reset() {
        metrics.clear();
        alertRules.clear();
        alertHistory.clear();
    }

    public record MetricValue(String name, long min, long max, long sum, long count, Instant timestamp) {}
    public record AlertRule(String name, String metricName, Operator operator, long threshold, Severity severity) {}
    public record Alert(String ruleName, String metricName, long currentValue, long threshold, Severity severity, Instant timestamp) {}

    public enum Operator { GREATER_THAN, LESS_THAN, EQUAL_TO, CHANGED }
    public enum Severity { INFO, WARNING, CRITICAL }
}
