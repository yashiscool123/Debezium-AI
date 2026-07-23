package io.debezium.v4.api.rest;

import io.debezium.v4.core.model.*;
import io.debezium.v4.core.engine.PipelineEngine;
import io.debezium.v4.core.validator.PipelineValidator;
import io.debezium.v4.ai.mapping.MappingEngine;
import io.debezium.v4.monitoring.MetricsCollector;
import io.debezium.v4.plugins.registry.PluginRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
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
    @Inject MappingEngine mappingEngine;
    @Inject MetricsCollector metrics;
    @Inject PluginRegistry pluginRegistry;

    @POST
    @Operation(summary = "Create a new pipeline")
    @APIResponse(responseCode = "201", description = "Pipeline created")
    public Response create(PipelineDefinition pipeline) {
        PipelineDefinition created = engine.create(pipeline);
        metrics.record("pipeline.created", 1);
        return Response.status(201).entity(created).build();
    }

    @GET
    @Operation(summary = "List all pipelines")
    public Response list(@QueryParam("tenant") String tenant) {
        List<PipelineDefinition> list = tenant != null ? engine.list(tenant) : engine.listAll();
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
    public Response deploy(@PathParam("id") String id) {
        try {
            PipelineEngine.PipelineInstance instance = engine.deploy(id);
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
}
