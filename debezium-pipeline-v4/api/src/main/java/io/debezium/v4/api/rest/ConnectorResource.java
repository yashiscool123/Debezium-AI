package io.debezium.v4.api.rest;

import io.debezium.v4.core.spi.ConnectorPlugin;
import io.debezium.v4.plugins.registry.PluginRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/connectors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Connectors", description = "Connector Type Registry")
public class ConnectorResource {

    @Inject PluginRegistry pluginRegistry;

    @GET
    @Operation(summary = "List all available connector types")
    public Response list() {
        List<Map<String, Object>> connectors = pluginRegistry.getPlugins(ConnectorPlugin.class).stream()
            .map(p -> Map.<String,Object>of("name", p.name(), "version", p.version(),
                "connectorClass", p.getConnectorClass(), "type", p.type(),
                "requiredConfig", p.getRequiredConfig(), "optionalConfig", p.getOptionalConfig()))
            .collect(Collectors.toList());
        return Response.ok(connectors).build();
    }

    @GET
    @Path("/{name}")
    @Operation(summary = "Get connector details")
    public Response get(@PathParam("name") String name) {
        return pluginRegistry.getPlugin(name, ConnectorPlugin.class)
            .map(p -> Response.ok(Map.of("name", p.name(), "version", p.version(),
                "connectorClass", p.getConnectorClass(), "configSchema", p.getConfigSchema())))
            .orElse(Response.status(404))
            .build();
    }
}
