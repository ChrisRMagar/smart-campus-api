package com.smartcampus.resources;

import com.smartcampus.application.DataStore;
import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sub-Resource for sensor readings: /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated by the SensorResource sub-resource locator.
 * It operates in the context of a specific sensor whose ID is injected at
 * construction time.
 *
 * Supports:
 *   GET  /api/v1/sensors/{sensorId}/readings          – full reading history
 *   POST /api/v1/sensors/{sensorId}/readings          – append a new reading
 *   GET  /api/v1/sensors/{sensorId}/readings/{readId} – get a single reading
 *
 * Side-effect on POST: updates the parent sensor's currentValue field to
 * keep the API's data consistent.
 *
 * Business Rule: A sensor with status "MAINTENANCE" cannot accept new readings
 * (throws SensorUnavailableException -> HTTP 403 Forbidden).
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }
    
    // GET /readings – return full history for this sensor
    
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadings(sensorId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensorId", sensorId);
        response.put("count", readings.size());
        response.put("readings", readings);
        response.put("_links", Map.of(
                "self",   "/api/v1/sensors/" + sensorId + "/readings",
                "sensor", "/api/v1/sensors/" + sensorId
        ));

        return Response.ok(response).build();
    }

    // POST /readings – append a new reading for this sensor
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Guard: sensor in MAINTENANCE cannot record new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE and "
                    + "cannot accept new readings. Change its status to ACTIVE first."
            );
        }

        // Assign a UUID if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Set timestamp to now if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.addReading(sensorId, reading);

        // SIDE EFFECT: update the parent sensor's currentValue for consistency
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("reading", reading);
        response.put("sensorCurrentValue", sensor.getCurrentValue());
        response.put("_links", Map.of(
                "self",     "/api/v1/sensors/" + sensorId + "/readings/" + reading.getId(),
                "readings", "/api/v1/sensors/" + sensorId + "/readings",
                "sensor",   "/api/v1/sensors/" + sensorId
        ));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // GET /readings/{readingId} – retrieve a single reading by ID
    
    @GET
    @Path("/{readingId}")
    public Response getReading(@PathParam("readingId") String readingId) {
        List<SensorReading> readings = store.getReadings(sensorId);

        SensorReading found = readings.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .orElse(null);

        if (found == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", "Reading '" + readingId + "' not found for sensor '" + sensorId + "'.");
            error.put("timestamp", System.currentTimeMillis());
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reading", found);
        response.put("_links", Map.of(
                "self",     "/api/v1/sensors/" + sensorId + "/readings/" + readingId,
                "readings", "/api/v1/sensors/" + sensorId + "/readings",
                "sensor",   "/api/v1/sensors/" + sensorId
        ));

        return Response.ok(response).build();
    }
}
