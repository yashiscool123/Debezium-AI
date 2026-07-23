package io.debezium.v4.api.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @ConfigProperty(name = "debezium.security.hsts.enabled", defaultValue = "true")
    boolean hstsEnabled;

    @ConfigProperty(name = "debezium.security.hsts.max-age", defaultValue = "31536000")
    String hstsMaxAge;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
        response.getHeaders().putSingle("X-Frame-Options", "DENY");
        response.getHeaders().putSingle("X-XSS-Protection", "0");
        response.getHeaders().putSingle("Cache-Control", "no-store, no-cache, must-revalidate");
        response.getHeaders().putSingle("Pragma", "no-cache");

        if (hstsEnabled && request.getUriInfo().getRequestUri().getScheme().equalsIgnoreCase("https")) {
            response.getHeaders().putSingle("Strict-Transport-Security",
                "max-age=" + hstsMaxAge + "; includeSubDomains");
        }
    }
}
