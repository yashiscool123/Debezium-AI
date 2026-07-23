package io.debezium.v4.api.rest;

import io.debezium.v4.core.model.*;
import io.debezium.v4.core.engine.DataQualityEngine;
import io.debezium.v4.core.engine.PipelineEngine;
import io.debezium.v4.core.validator.PipelineValidator;
import io.debezium.v4.ai.mapping.MappingEngine;
import io.debezium.v4.monitoring.MetricsCollector;
import io.debezium.v4.plugins.registry.PluginRegistry;
import io.debezium.v4.api.auth.AuthenticationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import jakarta.ws.rs.DefaultValue;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/v4/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Pipelines", description = "CDC Pipeline Management API v4")
public class PipelineResource {

    @Inject PipelineEngine engine;
    @Inject PipelineValidator validator;
    @Inject DataQualityEngine dqEngine;
    @Inject MappingEngine mappingEngine;
    @Inject MetricsCollector metrics;
    @Inject PluginRegistry pluginRegistry;
    @Inject AuthenticationService authService;

    @POST
    @Operation(summary = "Create a new pipeline")
    @APIResponse(responseCode = "201", description = "Pipeline created")
    public Response create(PipelineDefinition pipeline, @HeaderParam("Authorization") String auth) {
        PipelineDefinition created = engine.create(pipeline);
        metrics.record("pipeline.created", 1);
        return Response.status(201).entity(created).build();
    }

    @GET
    @Operation(summary = "List all pipelines")
    public Response list(@QueryParam("tenant") String tenant, @QueryParam("serviceUser") String serviceUser) {
        List<PipelineDefinition> list;
        if (serviceUser != null) {
            list = engine.listByServiceUser(serviceUser);
        } else if (tenant != null) {
            list = engine.list(tenant);
        } else {
            list = engine.listAll();
        }
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get pipeline by ID")
    public Response get(@PathParam("id") String id) {
        return engine.get(id)
            .map(Response::ok)
            .orElse(Response.status(404))
            .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update pipeline")
    public Response update(@PathParam("id") String id, PipelineDefinition pipeline) {
        return engine.update(id, pipeline)
            .map(Response::ok)
            .orElse(Response.status(404))
            .build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete pipeline")
    public Response delete(@PathParam("id") String id) {
        PipelineDefinition removed = engine.delete(id);
        if (removed != null) {
            metrics.record("pipeline.deleted", 1);
            return Response.noContent().build();
        }
        return Response.status(404).build();
    }

    @POST
    @Path("/{id}/deploy")
    @Operation(summary = "Deploy pipeline")
    public Response deploy(@PathParam("id") String id, @HeaderParam("Authorization") String auth) {
        try {
            String runAsUserId = resolveRunAsUser(auth);
            PipelineEngine.PipelineInstance instance = engine.deploy(id, runAsUserId);
            metrics.record("pipeline.deployed", 1);
            return Response.ok(instance).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/run-as/{serviceUserId}")
    @Operation(summary = "Deploy pipeline as a specific service user")
    public Response deployAsServiceUser(@PathParam("id") String id, @PathParam("serviceUserId") String serviceUserId) {
        try {
            PipelineEngine.PipelineInstance instance = engine.deploy(id, serviceUserId);
            metrics.record("pipeline.deployed", 1);
            return Response.ok(instance).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}/instance")
    @Operation(summary = "Get pipeline instance status")
    public Response getInstance(@PathParam("id") String id) {
        return engine.getInstance(id)
            .map(Response::ok)
            .orElse(Response.status(404))
            .build();
    }

    @POST
    @Path("/validate")
    @Operation(summary = "Validate pipeline configuration")
    public Response validate(PipelineDefinition pipeline) {
        PipelineValidator.ValidationResult result = validator.validate(pipeline);
        metrics.record("pipeline.validated", 1);
        return Response.ok(result).build();
    }

    @POST
    @Path("/encrypt-all")
    @Operation(summary = "Re-encrypt all pipeline secrets at rest")
    public Response reEncryptAll() {
        int count = engine.reEncryptAll();
        return Response.ok(Map.of("reEncrypted", count)).build();
    }

    // --- Data Quality ---

    @POST
    @Path("/{id}/quality/check")
    @Operation(summary = "Run data quality checks on pipeline")
    public Response runQualityCheck(@PathParam("id") String id, @HeaderParam("Authorization") String auth) {
        return engine.get(id).map(p -> {
            var config = p.dataQuality();
            if (config == null || !config.enabled()) {
                return Response.ok(Map.of("message", "Data quality checks not enabled for this pipeline",
                    "results", List.of())).build();
            }
            List<Map<String, String>> sampleRecords = generateSampleRecords(p);
            var results = dqEngine.runQualityCheck(id, config, sampleRecords);
            return Response.ok(Map.of("pipelineId", id, "results", results, "total", results.size())).build();
        }).orElse(Response.status(404).entity(Map.of("error", "Pipeline not found")).build());
    }

    @GET
    @Path("/{id}/quality/results")
    @Operation(summary = "Get data quality check results")
    public Response getQualityResults(@PathParam("id") String id,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("failed") @DefaultValue("false") boolean failedOnly) {
        List<QualityCheckResult> results = failedOnly
            ? dqEngine.getFailedResults(id)
            : dqEngine.getLatestResults(id, limit);
        return Response.ok(Map.of(
            "pipelineId", id,
            "results", results,
            "total", results.size(),
            "passRate", dqEngine.getPassRate(id),
            "totalEvaluated", dqEngine.getTotalRowsEvaluated(id),
            "totalFailures", dqEngine.getTotalFailures(id)
        )).build();
    }

    @DELETE
    @Path("/{id}/quality/results")
    @Operation(summary = "Clear data quality check results")
    public Response clearQualityResults(@PathParam("id") String id) {
        dqEngine.clearResults(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/duplicate")
    @Operation(summary = "Duplicate pipeline")
    public Response duplicate(@PathParam("id") String id) {
        try {
            PipelineDefinition copy = engine.duplicate(id);
            return Response.status(201).entity(copy).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    private List<Map<String, String>> generateSampleRecords(PipelineDefinition p) {
        List<Map<String, String>> records = new ArrayList<>();
        Set<String> columnNames = new LinkedHashSet<>();
        if (p.tableMappings() != null) {
            for (var mapping : p.tableMappings()) {
                if (mapping.columnMappings() != null) {
                    for (var col : mapping.columnMappings()) {
                        if (col.sourceColumn() != null) columnNames.add(col.sourceColumn());
                    }
                }
            }
        }
        if (columnNames.isEmpty()) {
            columnNames.add("id"); columnNames.add("name"); columnNames.add("status"); columnNames.add("value");
        }
        List<String> cols = new ArrayList<>(columnNames);
        for (int i = 1; i <= 10; i++) {
            Map<String, String> record = new HashMap<>();
            for (int j = 0; j < cols.size(); j++) {
                String colName = cols.get(j);
                if (i % 3 == 0 && j == 0) {
                    record.put(colName, "");
                } else if (i % 5 == 0 && j == 1) {
                    record.put(colName, "INVALID_" + i);
                } else {
                    record.put(colName, colName + "_" + i);
                }
            }
            records.add(record);
        }
        return records;
    }

    private String resolveRunAsUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        var session = authService.validateSession(token);
        if (session.isPresent()) return session.get().userId();
        var apiKeyResult = authService.authenticateWithApiKey(token);
        if (apiKeyResult.success() && apiKeyResult.user() != null) return apiKeyResult.user().id();
        return null;
    }
}
