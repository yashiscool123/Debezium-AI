package io.debezium.v4.api.rest;

import io.debezium.v4.monitoring.MetricsCollector;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Metrics", description = "Pipeline Metrics and Monitoring")
public class MetricsResource {

    @Inject MetricsCollector metrics;

    @GET
    @Operation(summary = "Get all pipeline metrics")
    public Response getMetrics() {
        return Response.ok(metrics.getAllMetrics()).build();
    }

    @GET
    @Path("/{name}")
    @Operation(summary = "Get metric by name")
    public Response getMetric(@PathParam("name") String name) {
        return Optional.ofNullable(metrics.getMetric(name))
            .map(Response::ok)
            .orElse(Response.status(404))
            .build();
    }

    @GET
    @Path("/summary")
    @Operation(summary = "Get summary statistics")
    public Response getSummary() {
        Map<String, Object> summary = metrics.getSummary();
        return Response.ok(summary).build();
    }

    @POST
    @Path("/alerts")
    @Operation(summary = "Evaluate alert rules")
    public Response evaluateAlerts() {
        Map<String, Object> alerts = metrics.evaluateAlerts();
        return Response.ok(alerts).build();
    }
}
