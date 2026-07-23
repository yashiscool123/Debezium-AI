package io.debezium.v4.api.rest;

import io.debezium.v4.core.model.*;
import io.debezium.v4.core.spi.DeploymentPlugin;
import io.debezium.v4.plugins.registry.PluginRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/deployments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Deployments", description = "Deployment Artifact Generation")
public class DeploymentResource {

    @Inject PluginRegistry pluginRegistry;

    @POST
    @Path("/generate")
    @Operation(summary = "Generate deployment artifacts for a pipeline")
    public Response generateDeployment(PipelineDefinition pipeline, @QueryParam("format") String format) {
        String fmt = format != null ? format : "strimzi";
        Map<String, Object> config = Map.of(
            "pipeline", pipeline,
            "format", fmt,
            "timestamp", System.currentTimeMillis()
        );

        return pluginRegistry.getPlugin(fmt + "-deployment", DeploymentPlugin.class)
            .map(plugin -> {
                String artifacts = plugin.generate(config);
                return Response.ok(Map.of(
                    "format", fmt,
                    "artifacts", artifacts,
                    "pipelineId", pipeline.id()
                )).build();
            })
            .orElseGet(() -> {
                // Generate YAML directly
                String yaml = generateYaml(pipeline);
                return Response.ok(Map.of("format", fmt, "artifacts", yaml)).build();
            });
    }

    @GET
    @Path("/formats")
    @Operation(summary = "List available deployment formats")
    public Response getFormats() {
        List<String> formats = pluginRegistry.getPlugins(DeploymentPlugin.class).stream()
            .map(p -> p.name().replace("-deployment", ""))
            .collect(Collectors.toList());
        if (formats.isEmpty()) {
            formats = List.of("strimzi", "docker-compose", "helm", "kubernetes", "terraform");
        }
        return Response.ok(formats).build();
    }

    private String generateYaml(PipelineDefinition pipeline) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Debezium Pipeline v4 - Generated Artifact\n");
        yaml.append("# Pipeline: ").append(pipeline.name()).append("\n");
        yaml.append("# Version: ").append(pipeline.version()).append("\n");
        yaml.append("apiVersion: kafka.strimzi.io/v1beta2\n");
        yaml.append("kind: KafkaConnector\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(pipeline.name().toLowerCase().replace(" ", "-")).append("\n");
        yaml.append("  labels:\n");
        yaml.append("    strimzi.io/cluster: ").append(pipeline.deployment() != null ? pipeline.deployment().connectClusterName() : "debezium-connect").append("\n");
        yaml.append("spec:\n");
        yaml.append("  class: ").append(pipeline.source().connector().connectorClass()).append("\n");
        yaml.append("  tasksMax: 1\n");
        yaml.append("  config:\n");
        if (pipeline.source().connector().config() != null) {
            pipeline.source().connector().config().forEach((k, v) -> yaml.append("    ").append(k).append(": \"").append(v).append("\"\n"));
        }
        return yaml.toString();
    }
}
