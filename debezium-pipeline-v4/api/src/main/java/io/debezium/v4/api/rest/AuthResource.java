package io.debezium.v4.api.rest;

import io.debezium.v4.api.auth.AuthenticationService;
import io.debezium.v4.api.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication, SSO, and session management")
public class AuthResource {

    @Inject AuthenticationService authService;

    @POST
    @Path("/login")
    @Operation(summary = "Login with username and password")
    public Response login(LoginRequest req) {
        var result = authService.login(req.username(), req.password());
        if (!result.success()) return Response.status(401).entity(ApiResponse.error(result.message())).build();
        return Response.ok(ApiResponse.ok(Map.of("sessionId", result.sessionId().orElse(""), "user", result.user().orElse(null)), result.message())).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout and invalidate session")
    public Response logout(@HeaderParam("Authorization") String auth) {
        if (auth != null && auth.startsWith("Bearer ")) authService.logout(auth.substring(7));
        return Response.ok(ApiResponse.ok(null, "Logged out")).build();
    }

    @GET
    @Path("/session")
    @Operation(summary = "Validate current session")
    public Response validateSession(@HeaderParam("Authorization") String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            return Response.status(401).entity(ApiResponse.error("No session")).build();
        var session = authService.validateSession(auth.substring(7));
        if (session.isEmpty()) return Response.status(401).entity(ApiResponse.error("Session expired")).build();
        return Response.ok(ApiResponse.ok(Map.of(
            "sessionId", session.get().sessionId(),
            "username", session.get().username(),
            "roles", session.get().roles(),
            "permissions", session.get().permissions(),
            "tenantId", session.get().tenantId(),
            "expiresAt", session.get().expiresAt().toString()
        ))).build();
    }

    @POST
    @Path("/sso/{provider}")
    @Operation(summary = "SSO login via OIDC provider")
    public Response ssoLogin(@PathParam("provider") String provider, SSORequest req) {
        var result = authService.ssoLogin(new AuthenticationService.SSOAuthRequest(provider, req.authCode(), req.redirectUri()));
        if (!result.success()) return Response.status(401).entity(ApiResponse.error(result.message())).build();
        return Response.ok(ApiResponse.ok(Map.of("sessionId", result.sessionId(), "provider", result.provider()))).build();
    }

    @GET
    @Path("/sso/providers")
    @Operation(summary = "List configured SSO providers")
    public Response listProviders() {
        return Response.ok(ApiResponse.ok(Map.of("providers", List.of("oidc", "keycloak", "google", "github")))).build();
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register a new user")
    public Response register(UserRegistration req) {
        var user = io.debezium.v4.api.auth.User.builder()
            .username(req.username()).email(req.email())
            .passwordHash(req.password())
            .addRole(io.debezium.v4.api.auth.Role.PIPELINE_VIEWER)
            .tenantId(req.tenantId() != null ? req.tenantId() : "default").build();
        authService.createUser(user);
        return Response.status(201).entity(ApiResponse.ok(null, "User registered")).build();
    }

    public record LoginRequest(String username, String password) {}
    public record SSORequest(String authCode, String redirectUri) {}
    public record UserRegistration(String username, String email, String password, String tenantId) {}
}
