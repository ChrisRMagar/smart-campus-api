package com.smartcampus.exceptions;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps SensorUnavailableException -> HTTP 403 Forbidden.
 *
 * Returned when a POST reading is attempted on a sensor that is in MAINTENANCE
 * mode. 403 is appropriate because the resource exists but the server is
 * refusing to perform the action due to the sensor's current state.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    private static final Logger LOGGER =
            Logger.getLogger(SensorUnavailableExceptionMapper.class.getName());

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        LOGGER.warning("403 Forbidden – Sensor unavailable: " + exception.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "SENSOR_UNAVAILABLE");
        body.put("message", exception.getMessage());
        body.put("hint", "Update the sensor status to 'ACTIVE' before recording readings.");
        body.put("timestamp", System.currentTimeMillis());

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
