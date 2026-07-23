package io.debezium.v4.core.engine;

import io.debezium.v4.core.model.*;
import io.debezium.v4.core.security.SecretManager;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class PipelineEngine {

    @Inject SecretManager secretManager;

    private final Map<String, PipelineDefinition> pipelines = new ConcurrentHashMap<>();
    private final Map<String, PipelineInstance> instances = new ConcurrentHashMap<>();

    public PipelineDefinition create(PipelineDefinition definition) {
        String id = UUID.randomUUID().toString();
        PipelineMetadata meta = PipelineMetadata.builder()
            .status(PipelineDefinition.PipelineStatus.DRAFT)
            .createdBy(definition.pipelineMetadata().createdBy())
            .build();
        PipelineDefinition encrypted = encryptDefinition(definition);
        PipelineDefinition created = new PipelineDefinition(
            id, encrypted.name(), encrypted.description(), encrypted.version(),
            encrypted.tenantId(), encrypted.serviceUserId(), encrypted.runAsServiceUser(),
            encrypted.source(), encrypted.target(), encrypted.tableMappings(),
            encrypted.transformations(), encrypted.dataQuality(), encrypted.deployment(),
            encrypted.monitoring(), encrypted.tags(), encrypted.metadata(), meta);
        pipelines.put(id, created);
        return created;
    }

    public Optional<PipelineDefinition> get(String id) { return Optional.ofNullable(pipelines.get(id)); }

    public List<PipelineDefinition> list(String tenantId) {
        return pipelines.values().stream()
            .filter(p -> p.tenantId().equals(tenantId))
            .collect(Collectors.toList());
    }

    public List<PipelineDefinition> listByServiceUser(String serviceUserId) {
        return pipelines.values().stream()
            .filter(p -> p.serviceUserId() != null && p.serviceUserId().equals(serviceUserId))
            .collect(Collectors.toList());
    }

    public List<PipelineDefinition> listAll() { return List.copyOf(pipelines.values()); }

    public Optional<PipelineDefinition> update(String id, PipelineDefinition updated) {
        return get(id).map(existing -> {
            PipelineDefinition encrypted = encryptDefinition(updated);
            PipelineDefinition merged = new PipelineDefinition(
                id, encrypted.name(), encrypted.description(), encrypted.version(),
                existing.tenantId(), encrypted.serviceUserId(), encrypted.runAsServiceUser(),
                encrypted.source(), encrypted.target(), encrypted.tableMappings(),
                encrypted.transformations(), encrypted.dataQuality(), encrypted.deployment(),
                encrypted.monitoring(), encrypted.tags(), encrypted.metadata(),
                new PipelineMetadata(existing.pipelineMetadata().status(), existing.pipelineMetadata().createdAt(),
                    Instant.now(), existing.pipelineMetadata().deployedAt(), existing.pipelineMetadata().createdBy(),
                    existing.pipelineMetadata().versionNumber() + 1, id, encrypted.pipelineMetadata().checksum()));
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
        return deploy(id, null);
    }

    public PipelineInstance deploy(String id, String runAsUserId) {
        PipelineDefinition def = pipelines.get(id);
        if (def == null) throw new IllegalArgumentException("Pipeline not found: " + id);
        PipelineDefinition decrypted = decryptDefinition(def);
        PipelineInstance instance = new PipelineInstance(id, decrypted, PipelineInstance.Status.DEPLOYING,
            Instant.now(), null, runAsUserId, Map.of());
        instances.put(id, instance);
        return instance;
    }

    public Optional<PipelineInstance> getInstance(String id) { return Optional.ofNullable(instances.get(id)); }

    public void updateInstanceStatus(String id, PipelineInstance.Status status) {
        getInstance(id).ifPresent(inst -> {
            PipelineInstance updated = new PipelineInstance(inst.id(), inst.definition(), status, inst.startedAt(),
                status == PipelineInstance.Status.RUNNING ? Instant.now() : inst.deployedAt(),
                inst.runAsUserId(), inst.metrics());
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
                existing.description(), "1.0.0", existing.tenantId(), existing.serviceUserId(),
                existing.runAsServiceUser(), existing.source(), existing.target(),
                existing.tableMappings(), existing.transformations(), existing.dataQuality(),
                existing.deployment(), existing.monitoring(), existing.tags(), existing.metadata(), meta);
        }).orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + id));
    }

    public int reEncryptAll() {
        int count = 0;
        for (var entry : pipelines.entrySet()) {
            PipelineDefinition def = entry.getValue();
            PipelineDefinition reEncrypted = encryptDefinition(def);
            pipelines.put(entry.getKey(), reEncrypted);
            count++;
        }
        return count;
    }

    private PipelineDefinition encryptDefinition(PipelineDefinition def) {
        SourceSpec encryptedSource = encryptSource(def.source());
        TargetSpec encryptedTarget = encryptTarget(def.target());
        return new PipelineDefinition(
            def.id(), def.name(), def.description(), def.version(), def.tenantId(),
            def.serviceUserId(), def.runAsServiceUser(),
            encryptedSource, encryptedTarget, def.tableMappings(),
            def.transformations(), def.dataQuality(), def.deployment(),
            def.monitoring(), def.tags(), def.metadata(), def.pipelineMetadata()
        );
    }

    private PipelineDefinition decryptDefinition(PipelineDefinition def) {
        SourceSpec decryptedSource = decryptSource(def.source());
        TargetSpec decryptedTarget = decryptTarget(def.target());
        return new PipelineDefinition(
            def.id(), def.name(), def.description(), def.version(), def.tenantId(),
            def.serviceUserId(), def.runAsServiceUser(),
            decryptedSource, decryptedTarget, def.tableMappings(),
            def.transformations(), def.dataQuality(), def.deployment(),
            def.monitoring(), def.tags(), def.metadata(), def.pipelineMetadata()
        );
    }

    private SourceSpec encryptSource(SourceSpec source) {
        if (source == null) return null;
        ConnectorConfig encryptedConnector = source.connector() != null
            ? source.connector().withEncryptedSecrets(secretManager).withEncryptedConfig(secretManager)
            : null;
        return new SourceSpec(source.type(), encryptedConnector, source.schema(),
            source.snapshot(), source.properties());
    }

    private SourceSpec decryptSource(SourceSpec source) {
        if (source == null) return null;
        ConnectorConfig decryptedConnector = source.connector() != null
            ? source.connector().withDecryptedSecrets(secretManager).withDecryptedConfig(secretManager)
            : null;
        return new SourceSpec(source.type(), decryptedConnector, source.schema(),
            source.snapshot(), source.properties());
    }

    private TargetSpec encryptTarget(TargetSpec target) {
        if (target == null) return null;
        ConnectorConfig encryptedConnector = target.connector() != null
            ? target.connector().withEncryptedSecrets(secretManager).withEncryptedConfig(secretManager)
            : null;
        return new TargetSpec(target.type(), encryptedConnector, target.topics(),
            target.sink(), target.properties());
    }

    private TargetSpec decryptTarget(TargetSpec target) {
        if (target == null) return null;
        ConnectorConfig decryptedConnector = target.connector() != null
            ? target.connector().withDecryptedSecrets(secretManager).withDecryptedConfig(secretManager)
            : null;
        return new TargetSpec(target.type(), decryptedConnector, target.topics(),
            target.sink(), target.properties());
    }

    public record PipelineInstance(String id, PipelineDefinition definition, Status status,
        Instant startedAt, Instant deployedAt, String runAsUserId, Map<String,Object> metrics) {
        public enum Status { PENDING, DEPLOYING, RUNNING, DEGRADED, FAILED, STOPPED }
    }
}
