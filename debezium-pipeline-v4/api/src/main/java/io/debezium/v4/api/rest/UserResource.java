package io.debezium.v4.api.rest;

import io.debezium.v4.api.auth.*;
import io.debezium.v4.api.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User and role management")
public class UserResource {

    @Inject AuthenticationService authService;

    @GET
    @Operation(summary = "List all users")
    public Response list() {
        return Response.ok(ApiResponse.ok(authService.listUsers())).build();
    }

    @GET
    @Path("/{username}")
    @Operation(summary = "Get user by username")
    public Response get(@PathParam("username") String username) {
        return authService.getUser(username)
            .map(u -> Response.ok(ApiResponse.ok(u)))
            .orElse(Response.status(404))
            .build();
    }

    @POST
    @Operation(summary = "Create a new user")
    public Response create(User user) {
        return Response.status(201).entity(ApiResponse.ok(authService.createUser(user))).build();
    }

    @PUT
    @Path("/{username}")
    @Operation(summary = "Update user")
    public Response update(@PathParam("username") String username, User user) {
        return Response.ok(ApiResponse.ok(authService.updateUser(username, user))).build();
    }

    @DELETE
    @Path("/{username}")
    @Operation(summary = "Delete user")
    public Response delete(@PathParam("username") String username) {
        authService.deleteUser(username);
        return Response.noContent().build();
    }

    @GET
    @Path("/roles")
    @Operation(summary = "List all roles and their default permissions")
    public Response listRoles() {
        Map<String, Object> roles = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            roles.put(role.name(), Map.of(
                "displayName", role.displayName(),
                "description", role.description(),
                "permissions", Role.defaultPermissions(role).stream().map(Enum::name).toList()
            ));
        }
        return Response.ok(ApiResponse.ok(roles)).build();
    }

    @GET
    @Path("/permissions")
    @Operation(summary = "List all permissions")
    public Response listPermissions() {
        return Response.ok(ApiResponse.ok(Arrays.asList(Permission.values()))).build();
    }
}
