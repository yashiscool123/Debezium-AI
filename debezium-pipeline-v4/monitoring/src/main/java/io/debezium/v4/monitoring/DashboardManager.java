package io.debezium.v4.monitoring;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DashboardManager {

    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>();

    public Dashboard create(Dashboard d) {
        Dashboard withMeta = new Dashboard(
            UUID.randomUUID().toString(), d.name(), d.description(),
            d.widgets(), d.refreshIntervalSeconds(),
            Instant.now(), Instant.now()
        );
        dashboards.put(withMeta.id(), withMeta);
        return withMeta;
    }

    public Optional<Dashboard> get(String id) { return Optional.ofNullable(dashboards.get(id)); }
    public List<Dashboard> list() { return List.copyOf(dashboards.values()); }
    public Optional<Dashboard> update(String id, Dashboard d) {
        return Optional.ofNullable(dashboards.computeIfPresent(id, (k, v) -> new Dashboard(
            id, d.name(), d.description(), d.widgets(),
            d.refreshIntervalSeconds(), v.createdAt(), Instant.now()
        )));
    }
    public Dashboard delete(String id) { return dashboards.remove(id); }

    public record Dashboard(
        String id, String name, String description,
        List<Widget> widgets, int refreshIntervalSeconds,
        Instant createdAt, Instant updatedAt
    ) {}

    public record Widget(
        String id, String title, WidgetType type,
        int width, int height, int x, int y,
        String metricName, Map<String, Object> options
    ) {}

    public enum WidgetType {
        COUNTER, TIME_SERIES, TABLE, PIE_CHART, BAR_CHART,
        HEAT_MAP, TOPOLOGY, LOG_VIEWER, STATUS_INDICATOR
    }
}
