package io.debezium.v4.core.engine;

import io.debezium.v4.core.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PipelineEngine {
    private final Map<String, PipelineDefinition> pipelines = new ConcurrentHashMap<>();
    private final Map<String, PipelineInstance> instances = new ConcurrentHashMap<>();

    public PipelineDefinition create(PipelineDefinition definition) {
        String id = UUID.randomUUID().toString();
        PipelineMetadata meta = PipelineMetadata.builder()
            .status(PipelineDefinition.PipelineStatus.DRAFT)
            .createdBy(definition.pipelineMetadata().createdBy())
            .build();
        PipelineDefinition created = new PipelineDefinition(
            id, definition.name(), definition.description(), definition.version(),
            definition.tenantId(), definition.source(), definition.target(),
            definition.tableMappings(), definition.transformations(), definition.deployment(),
            definition.monitoring(), definition.tags(), definition.metadata(), meta);
        pipelines.put(id, created);
        return created;
    }

    public Optional<PipelineDefinition> get(String id) { return Optional.ofNullable(pipelines.get(id)); }

    public List<PipelineDefinition> list(String tenantId) {
        return pipelines.values().stream()
            .filter(p -> p.tenantId().equals(tenantId))
            .collect(Collectors.toList());
    }

    public List<PipelineDefinition> listAll() { return List.copyOf(pipelines.values()); }

    public Optional<PipelineDefinition> update(String id, PipelineDefinition updated) {
        return get(id).map(existing -> {
            PipelineDefinition merged = new PipelineDefinition(
                id, updated.name(), updated.description(), updated.version(),
                existing.tenantId(), updated.source(), updated.target(),
                updated.tableMappings(), updated.transformations(), updated.deployment(),
                updated.monitoring(), updated.tags(), updated.metadata(),
                new PipelineMetadata(existing.pipelineMetadata().status(), existing.pipelineMetadata().createdAt(),
                    Instant.now(), existing.pipelineMetadata().deployedAt(), existing.pipelineMetadata().createdBy(),
                    existing.pipelineMetadata().versionNumber() + 1, id, updated.pipelineMetadata().checksum()));
            pipelines.put(id, merged);
            return merged;
        });
    }

    public PipelineDefinition delete(String id) {
        PipelineDefinition removed = pipelines.remove(id);
        instances.remove(id);
        return removed;
    }

    public PipelineInstance deploy(String id) {
        PipelineDefinition def = pipelines.get(id);
        if (def == null) throw new IllegalArgumentException("Pipeline not found: " + id);
        PipelineInstance instance = new PipelineInstance(id, def, PipelineInstance.Status.DEPLOYING, Instant.now(), null, Map.of());
        instances.put(id, instance);
        return instance;
    }

    public Optional<PipelineInstance> getInstance(String id) { return Optional.ofNullable(instances.get(id)); }

    public void updateInstanceStatus(String id, PipelineInstance.Status status) {
        getInstance(id).ifPresent(inst -> {
            PipelineInstance updated = new PipelineInstance(inst.id(), inst.definition(), status, inst.startedAt(),
                status == PipelineInstance.Status.RUNNING ? Instant.now() : inst.deployedAt(), inst.metrics());
            instances.put(id, updated);
        });
    }

    public PipelineDefinition duplicate(String id) {
        return get(id).map(existing -> {
            PipelineMetadata meta = PipelineMetadata.builder()
                .status(PipelineDefinition.PipelineStatus.DRAFT)
                .createdBy(existing.pipelineMetadata().createdBy())
                .versionNumber(1)
                .build();
            return new PipelineDefinition(UUID.randomUUID().toString(), existing.name() + " (copy)",
                existing.description(), "1.0.0", existing.tenantId(), existing.source(),
                existing.target(), existing.tableMappings(), existing.transformations(),
                existing.deployment(), existing.monitoring(), existing.tags(), existing.metadata(), meta);
        }).orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + id));
    }

    public record PipelineInstance(String id, PipelineDefinition definition, Status status, Instant startedAt, Instant deployedAt, Map<String,Object> metrics) {
        public enum Status { PENDING, DEPLOYING, RUNNING, DEGRADED, FAILED, STOPPED }
    }
}
