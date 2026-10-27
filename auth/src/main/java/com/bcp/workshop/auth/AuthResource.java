package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Authentication service unavailable";

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null) {
            return badRequest("Request is null");
        }
        if (isBlank(request.username)) {
            return badRequest("Username is required");
        }
        if (isBlank(request.password)) {
            return badRequest("Password is required");
        }

        try {
            return authService.authenticate(request.username, request.password)
                    .map(response -> Response.ok(response).build())
                    .orElseGet(() -> unauthorized(request.username));
        } catch (Exception error) {
            LOG.error("Authentication failed because of an internal error", error);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", INTERNAL_ERROR_MESSAGE))
                    .build();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("message", message)).build();
    }

    private Response unauthorized(String username) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("message", "Invalid user " + username + " or password"))
                .build();
    }
}
