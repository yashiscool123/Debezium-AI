package io.debezium.v4.api.rest;

import io.debezium.v4.core.export.PipelineExporter;
import io.debezium.v4.api.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/export")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Export/Import", description = "Pipeline export, import, and release management")
public class ExportResource {

    @Inject PipelineExporter exporter;

    @POST
    @Path("/pipelines")
    @Operation(summary = "Export pipelines as a release package")
    public Response exportPipelines(ExportRequest req) {
        String json = exporter.exportToJson(req.pipelineIds(), req.sourceEnvironment(), req.targetEnvironment());
        return Response.ok(ApiResponse.ok(json)).build();
    }

    @POST
    @Path("/release")
    @Operation(summary = "Create a release package")
    public Response createRelease(ReleaseRequest req) {
        var pkg = exporter.exportRelease(req.pipelineIds(), req.version(), req.releaseNotes());
        return Response.ok(ApiResponse.ok(pkg, "Release " + req.version() + " created")).build();
    }

    @POST
    @Path("/environment")
    @Operation(summary = "Export all pipelines from source to target environment")
    public Response exportEnvironment(EnvRequest req) {
        String json = exporter.exportEnvironment(req.sourceEnvironment(), req.targetEnvironment());
        return Response.ok(ApiResponse.ok(json)).build();
    }

    @POST
    @Path("/import")
    @Operation(summary = "Import pipelines from a JSON export")
    public Response importPipelines(ImportRequest req) {
        var imported = exporter.importFromJson(req.json(), req.targetEnvironment());
        return Response.ok(ApiResponse.ok(Map.of("imported", imported.size(), "pipelines", imported))).build();
    }

    @GET
    @Path("/formats")
    @Operation(summary = "List supported export formats")
    public Response listFormats() {
        return Response.ok(ApiResponse.ok(Map.of(
            "formats", List.of("json", "yaml", "zip"),
            "environments", List.of("DEV", "QA", "STAGING", "PROD")
        ))).build();
    }

    public record ExportRequest(List<String> pipelineIds, String sourceEnvironment, String targetEnvironment) {}
    public record ReleaseRequest(List<String> pipelineIds, String version, String releaseNotes) {}
    public record EnvRequest(String sourceEnvironment, String targetEnvironment) {}
    public record ImportRequest(String json, String targetEnvironment) {}
}
