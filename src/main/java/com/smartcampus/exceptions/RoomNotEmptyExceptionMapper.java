package com.smartcampus.exceptions;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps RoomNotEmptyException -> HTTP 409 Conflict.
 *
 * Triggered when a client attempts to delete a room that still has sensors.
 * Returns a descriptive JSON body so the client knows exactly why the
 * operation was rejected and what to do next.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    private static final Logger LOGGER = Logger.getLogger(RoomNotEmptyExceptionMapper.class.getName());

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        LOGGER.warning("409 Conflict – Room deletion blocked: " + exception.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("error", "ROOM_NOT_EMPTY");
        body.put("message", exception.getMessage());
        body.put("hint", "Use DELETE /api/v1/sensors/{sensorId} to remove sensors, "
                + "or reassign them, before deleting the room.");
        body.put("timestamp", System.currentTimeMillis());

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
