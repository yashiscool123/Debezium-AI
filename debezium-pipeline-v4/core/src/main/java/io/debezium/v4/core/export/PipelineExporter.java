package io.debezium.v4.core.export;

import io.debezium.v4.core.model.ConnectorConfig;
import io.debezium.v4.core.model.PipelineDefinition;
import io.debezium.v4.core.engine.PipelineEngine;
import io.debezium.v4.core.security.SecretManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class PipelineExporter {

    @Inject PipelineEngine engine;
    @Inject SecretManager secretManager;

    public record ExportPackage(
        String id,
        String name,
        String description,
        String version,
        String sourceEnvironment,
        String targetEnvironment,
        List<ExportedPipeline> pipelines,
        List<ExportedConfig> configurations,
        List<ExportedMapping> mappings,
        Map<String, Object> metadata,
        Instant exportedAt
    ) {}

    public record ExportedPipeline(
        String id,
        String name,
        String version,
        PipelineDefinition definition,
        Map<String, Object> deploymentArtifacts,
        String status
    ) {}

    public record ExportedConfig(
        String key,
        String value,
        boolean secret,
        String environment
    ) {}

    public record ExportedMapping(
        String id,
        String sourceTable,
        String targetTable,
        List<ExportedColumnMapping> columns,
        double confidence
    ) {}

    public record ExportedColumnMapping(
        String sourceColumn,
        String targetColumn,
        String sourceType,
        String targetType,
        String transformation
    ) {}

    public ExportPackage exportPipelines(List<String> pipelineIds, String sourceEnv, String targetEnv) {
        List<ExportedPipeline> pipelines = new ArrayList<>();
        for (String id : pipelineIds) {
            engine.get(id).ifPresent(p -> {
                PipelineDefinition sanitized = maskPipelineSecrets(p);
                pipelines.add(new ExportedPipeline(
                    sanitized.id(), sanitized.name(), sanitized.version(),
                    sanitized, Map.of("format", "strimzi"), "EXPORTED"
                ));
            });
        }
        return new ExportPackage(
            UUID.randomUUID().toString(),
            "pipeline-export-" + Instant.now().toEpochMilli(),
            "Export from " + sourceEnv + " to " + targetEnv,
            "1.0.0",
            sourceEnv,
            targetEnv,
            pipelines,
            List.of(),
            List.of(),
            exportMetadata(),
            Instant.now()
        );
    }

    public String exportToJson(List<String> pipelineIds, String sourceEnv, String targetEnv) {
        try {
            var pkg = exportPipelines(pipelineIds, sourceEnv, targetEnv);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter();
            return mapper.writeValueAsString(pkg);
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<PipelineDefinition> importFromJson(String json, String environment) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var pkg = mapper.readValue(json, ExportPackage.class);
            List<PipelineDefinition> imported = new ArrayList<>();
            for (var ep : pkg.pipelines()) {
                var updated = applyEnvironmentOverrides(ep.definition(), environment);
                PipelineDefinition created = engine.create(updated);
                imported.add(created);
            }
            return imported;
        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    private PipelineDefinition maskPipelineSecrets(PipelineDefinition p) {
        SourceSpec maskedSource = null;
        if (p.source() != null) {
            ConnectorConfig maskedSrcConnector = null;
            if (p.source().connector() != null) {
                maskedSrcConnector = p.source().connector()
                    .withMaskedSecrets(secretManager)
                    .withMaskedConfig(secretManager);
            }
            maskedSource = new SourceSpec(
                p.source().type(), maskedSrcConnector, p.source().schema(),
                p.source().snapshot(), p.source().properties()
            );
        }
        TargetSpec maskedTarget = null;
        if (p.target() != null) {
            ConnectorConfig maskedTgtConnector = null;
            if (p.target().connector() != null) {
                maskedTgtConnector = p.target().connector()
                    .withMaskedSecrets(secretManager)
                    .withMaskedConfig(secretManager);
            }
            maskedTarget = new TargetSpec(
                p.target().type(), maskedTgtConnector, p.target().topics(),
                p.target().sink(), p.target().properties()
            );
        }
        return new PipelineDefinition(
            p.id(), p.name(), p.description(), p.version(), p.tenantId(),
            p.serviceUserId(), p.runAsServiceUser(),
            maskedSource, maskedTarget, p.tableMappings(), p.transformations(),
            p.dataQuality(), p.deployment(), p.monitoring(), p.tags(), p.metadata(),
            p.pipelineMetadata()
        );
    }

    private PipelineDefinition applyEnvironmentOverrides(PipelineDefinition p, String env) {
        var deployment = p.deployment();
        if (deployment != null) {
            var updatedDeployment = new io.debezium.v4.core.model.DeploymentSpec(
                deployment.type(), env, env + "-" + deployment.namespace(),
                deployment.connectClusterName(), deployment.replicas(),
                deployment.resources(), deployment.scaling(), deployment.network(),
                deployment.environment(), deployment.labels(), deployment.annotations()
            );
            return new PipelineDefinition(
                p.id(), p.name(), p.description(), p.version(), p.tenantId(),
                p.serviceUserId(), p.runAsServiceUser(),
                p.source(), p.target(), p.tableMappings(), p.transformations(),
                p.dataQuality(), updatedDeployment, p.monitoring(), p.tags(), p.metadata(),
                p.pipelineMetadata()
            );
        }
        return p;
    }

    public String exportEnvironment(String sourceEnv, String targetEnv) {
        var all = engine.listAll();
        var ids = all.stream().map(PipelineDefinition::id).toList();
        return exportToJson(ids, sourceEnv, targetEnv);
    }

    public ExportPackage exportRelease(List<String> pipelineIds, String version, String releaseNotes) {
        var pkg = exportPipelines(pipelineIds, "DEV", "PROD");
        return new ExportPackage(
            pkg.id(),
            "release-" + version,
            releaseNotes,
            version,
            "DEV",
            "PROD",
            pkg.pipelines(),
            pkg.configurations(),
            pkg.mappings(),
            Map.of("type", "release", "buildTimestamp", Instant.now().toString(), "releaseVersion", version),
            Instant.now()
        );
    }

    private Map<String, Object> exportMetadata() {
        return Map.of(
            "exportedAt", Instant.now().toString(),
            "debeziumVersion", "3.7.0-SNAPSHOT",
            "toolVersion", "4.0.0-SNAPSHOT",
            "formatVersion", "1.0"
        );
    }
}
