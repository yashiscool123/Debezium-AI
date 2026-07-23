package io.debezium.v4.api.auth;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RBACFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/v4/auth/login", "/v4/auth/sso", "/v4/auth/register",
        "/v4/health", "/q/health", "/q/metrics", "/v4/openapi", "/v4/auth/logout",
        "/v4/auth/service/api-key");

    private static final Map<String, Permission> PATH_PERMISSIONS = Map.ofEntries(
        Map.entry("POST:/v4/pipelines", Permission.PIPELINE_CREATE),
        Map.entry("GET:/v4/pipelines", Permission.PIPELINE_READ),
        Map.entry("PUT:/v4/pipelines/", Permission.PIPELINE_UPDATE),
        Map.entry("DELETE:/v4/pipelines/", Permission.PIPELINE_DELETE),
        Map.entry("POST:/v4/pipelines/.*/deploy", Permission.PIPELINE_DEPLOY),
        Map.entry("POST:/v4/pipelines/.*/start", Permission.PIPELINE_START),
        Map.entry("POST:/v4/pipelines/.*/stop", Permission.PIPELINE_STOP),
        Map.entry("POST:/v4/pipelines/.*/run-as", Permission.PIPELINE_RUN_AS),
        Map.entry("POST:/v4/validate", Permission.PIPELINE_READ),
        Map.entry("POST:/v4/pipelines/.*/duplicate", Permission.PIPELINE_CREATE),
        Map.entry("POST:/v4/mappings", Permission.MAPPING_CREATE),
        Map.entry("POST:/v4/mappings/suggest", Permission.MAPPING_CREATE),
        Map.entry("POST:/v4/mappings/embed", Permission.MAPPING_READ),
        Map.entry("POST:/v4/mappings/similarity", Permission.MAPPING_READ),
        Map.entry("GET:/v4/connectors", Permission.CONNECTOR_READ),
        Map.entry("GET:/v4/connectors/", Permission.CONNECTOR_READ),
        Map.entry("GET:/v4/deployments", Permission.DEPLOYMENT_READ),
        Map.entry("POST:/v4/deployments/generate", Permission.DEPLOYMENT_CREATE),
        Map.entry("GET:/v4/metrics", Permission.METRICS_READ),
        Map.entry("GET:/v4/metrics/", Permission.METRICS_READ),
        Map.entry("POST:/v4/metrics/alerts", Permission.METRICS_WRITE),
        Map.entry("GET:/v4/audit", Permission.AUDIT_READ),
        Map.entry("GET:/v4/users", Permission.USER_READ),
        Map.entry("POST:/v4/users", Permission.USER_CREATE),
        Map.entry("PUT:/v4/users/", Permission.USER_UPDATE),
        Map.entry("DELETE:/v4/users/", Permission.USER_DELETE),
        Map.entry("GET:/v4/export", Permission.EXPORT),
        Map.entry("POST:/v4/import", Permission.IMPORT),
        Map.entry("GET:/v4/settings", Permission.SETTINGS_READ),
        Map.entry("PUT:/v4/settings", Permission.SETTINGS_WRITE),
        Map.entry("POST:/v4/auth/service", Permission.USER_CREATE),
        Map.entry("GET:/v4/auth/service/", Permission.USER_READ),
        Map.entry("POST:/v4/auth/service/.*/api-key", Permission.USER_READ)
    );

    @Inject
    AuthenticationService authService;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String path = request.getUriInfo().getPath();
        String method = request.getMethod();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return;

        String authHeader = request.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            request.abortWith(Response.status(401).entity(Map.of("error", "Missing or invalid Bearer token")).build());
            return;
        }

        String token = authHeader.substring(7);

        // Support service user API key authentication
        var session = authService.validateSession(token);
        if (session.isEmpty()) {
            var apiKeyResult = authService.authenticateWithApiKey(token);
            if (!apiKeyResult.success()) {
                request.abortWith(Response.status(401).entity(Map.of("error", "Session expired or invalid API key")).build());
                return;
            }
            session = authService.validateSession(apiKeyResult.sessionId());
        }

        if (session.isEmpty()) {
            request.abortWith(Response.status(401).entity(Map.of("error", "Session expired or invalid")).build());
            return;
        }

        String key = method + ":" + path;
        Permission required = PATH_PERMISSIONS.get(key);

        if (required == null) {
            for (var entry : PATH_PERMISSIONS.entrySet()) {
                if (key.matches(entry.getKey().replace(".*", ".+"))) {
                    required = entry.getValue();
                    break;
                }
            }
        }

        if (required != null && !session.get().permissions().contains(required) && !session.get().permissions().contains(Permission.ALL)) {
            request.abortWith(Response.status(403).entity(Map.of("error", "Insufficient permissions", "required", required.name())).build());
        }
    }
}
