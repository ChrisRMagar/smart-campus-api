package com.smartcampus.exceptions;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps LinkedResourceNotFoundException -> HTTP 422 Unprocessable Entity.
 *
 * Used when a client supplies a valid JSON payload but references a related
 * resource (e.g., roomId) that does not exist. 422 is chosen over 404 because
 * the endpoint URI is valid — the semantic problem is inside the request body.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final Logger LOGGER =
            Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        LOGGER.warning("422 Unprocessable Entity – Bad reference in payload: "
                + exception.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", "UNPROCESSABLE_ENTITY");
        body.put("message", exception.getMessage());
        body.put("hint", "Verify that the referenced resource exists before "
                + "submitting. Check GET /api/v1/rooms for valid room IDs.");
        body.put("timestamp", System.currentTimeMillis());

        // 422 is not in the standard JAX-RS Status enum, so use fromStatusCode
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
