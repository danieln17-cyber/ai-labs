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
    private static final Logger LOGGER = Logger.getLogger(AuthResource.class);
    private static final String NULL_REQUEST_MESSAGE = "Request is null";
    private static final String INTERNAL_ERROR_MESSAGE = "Authentication failed";

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null) {
            return errorResponse(Response.Status.BAD_REQUEST, NULL_REQUEST_MESSAGE);
        }

        try {
            AuthService.AuthenticationResult result =
                    authService.authenticate(request.username, request.password);
            if (result.failure() == AuthService.Failure.NONE) {
                return Response.ok(result.response()).build();
            }
            return errorResponse(statusFor(result.failure()), result.message());
        } catch (Exception error) {
            LOGGER.error("Authentication request failed", error);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
        }
    }

    private Response.Status statusFor(AuthService.Failure failure) {
        return switch (failure) {
            case MISSING_USERNAME, MISSING_PASSWORD -> Response.Status.BAD_REQUEST;
            case INVALID_CREDENTIALS -> Response.Status.UNAUTHORIZED;
            case NONE -> throw new IllegalArgumentException("Successful authentication has no error status");
        };
    }

    private Response errorResponse(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }
}
