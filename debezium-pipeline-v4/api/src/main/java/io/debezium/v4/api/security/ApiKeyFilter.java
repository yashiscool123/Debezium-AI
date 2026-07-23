package io.debezium.v4.api.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/v4/health", "/v4/openapi", "/q/health", "/q/metrics");

    @ConfigProperty(name = "debezium.api.key")
    String apiKey;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String path = request.getUriInfo().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return;

        String key = request.getHeaderString("X-API-Key");
        if (key == null) key = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (key != null && key.startsWith("Bearer ")) key = key.substring(7);

        if (apiKey != null && !apiKey.equals(key)) {
            request.abortWith(Response.status(401)
                .entity(Map.of("error", "Invalid or missing API key"))
                .build());
        }
    }
}
