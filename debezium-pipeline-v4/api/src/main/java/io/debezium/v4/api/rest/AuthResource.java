package io.debezium.v4.api.rest;

import io.debezium.v4.api.auth.AuthenticationService;
import io.debezium.v4.api.auth.User;
import io.debezium.v4.api.auth.UserType;
import io.debezium.v4.api.auth.Role;
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
        return Response.ok(ApiResponse.ok(Map.of("sessionId", result.sessionId(), "user", result.user()), result.message())).build();
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
            "userType", session.get().userType().name(),
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
        var user = User.builder()
            .username(req.username()).email(req.email())
            .passwordHash(authService.hashPassword(req.password()))
            .type(UserType.HUMAN)
            .addRole(Role.PIPELINE_VIEWER)
            .tenantId(req.tenantId() != null ? req.tenantId() : "default").build();
        authService.createUser(user);
        return Response.status(201).entity(ApiResponse.ok(null, "User registered")).build();
    }

    // --- Service User Management ---

    @POST
    @Path("/service")
    @Operation(summary = "Create a service user")
    public Response createServiceUser(ServiceUserRequest req) {
        var user = io.debezium.v4.api.auth.User.builder()
            .id(UUID.randomUUID().toString())
            .username(req.username())
            .email(req.email() != null ? req.email() : req.username() + "@service.debezium.ai")
            .passwordHash("")
            .type(UserType.SERVICE)
            .addRole(req.role() != null ? io.debezium.v4.api.auth.Role.fromString(req.role()) : io.debezium.v4.api.auth.Role.PIPELINE_OPERATOR)
            .tenantId(req.tenantId() != null ? req.tenantId() : "default")
            .enabled(true)
            .build();
        authService.createUser(user);
        String apiKey = authService.generateApiKey(user.username());
        return Response.status(201).entity(ApiResponse.ok(Map.of(
            "user", user,
            "apiKey", apiKey
        ), "Service user created")).build();
    }

    @GET
    @Path("/service")
    @Operation(summary = "List all service users")
    public Response listServiceUsers() {
        return Response.ok(ApiResponse.ok(authService.listServiceUsers())).build();
    }

    @POST
    @Path("/service/{username}/api-key")
    @Operation(summary = "Generate a new API key for a service user")
    public Response generateApiKey(@PathParam("username") String username) {
        try {
            String apiKey = authService.generateApiKey(username);
            return Response.ok(ApiResponse.ok(Map.of("apiKey", apiKey), "API key generated")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @POST
    @Path("/service/{username}/revoke")
    @Operation(summary = "Revoke all API keys for a service user")
    public Response revokeApiKeys(@PathParam("username") String username) {
        var keys = authService.listApiKeys(username);
        for (String key : keys) authService.revokeApiKey(key);
        return Response.ok(ApiResponse.ok(null, "API keys revoked for: " + username)).build();
    }

    @GET
    @Path("/service/{username}/api-keys")
    @Operation(summary = "List API keys for a service user")
    public Response listApiKeys(@PathParam("username") String username) {
        var keys = authService.listApiKeys(username);
        return Response.ok(ApiResponse.ok(Map.of("username", username, "apiKeys", keys))).build();
    }

    public record LoginRequest(String username, String password) {}
    public record SSORequest(String authCode, String redirectUri) {}
    public record UserRegistration(String username, String email, String password, String tenantId) {}
    public record ServiceUserRequest(String username, String email, String role, String tenantId) {}
}
