package com.smartcampus.resources;

import com.smartcampus.application.DataStore;
import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sensor Resource - manages /api/v1/sensors
 *
 * Supports:
 *   GET  /api/v1/sensors              – list all sensors (optional ?type= filter)
 *   POST /api/v1/sensors              – register a new sensor (validates roomId)
 *   GET  /api/v1/sensors/{sensorId}   – get a specific sensor
 *   DELETE /api/v1/sensors/{sensorId} – remove a sensor
 *   /api/v1/sensors/{sensorId}/readings – sub-resource locator -> SensorReadingResource
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * REPORT ANSWERS
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Q (Part 3.1): @Consumes(APPLICATION_JSON) – what if client sends wrong format?
 * JAX-RS performs content negotiation before invoking the method. If a client
 * sends a Content-Type of text/plain or application/xml, Jersey cannot find a
 * MessageBodyReader capable of deserialising the body into a Sensor object for
 * that media type. Jersey immediately returns HTTP 415 Unsupported Media Type
 * without ever calling the method body. The GlobalExceptionMapper will NOT be
 * triggered because JAX-RS intercepts the mismatch at the framework level
 * before dispatching.
 *
 * Q (Part 3.2): @QueryParam vs path segment for filtering
 * Query parameters (?type=CO2) are semantically correct for filtering because
 * they do not identify a new resource — they refine a representation of an
 * existing collection. Path segments (/sensors/type/CO2) imply a distinct
 * addressable resource, violating REST's resource-naming principle. Query
 * parameters are also optional by design (omitting ?type returns all sensors),
 * they compose naturally with multiple filters (?type=CO2&status=ACTIVE), and
 * they keep the base path clean and cacheable. Path segments for filters also
 * complicate routing tables and can clash with actual resource IDs.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors – list all sensors with optional ?type= filter
    // -------------------------------------------------------------------------

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(store.getSensors().values());

        // Apply type filter if provided
        if (type != null && !type.isBlank()) {
            sensorList = sensorList.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", sensorList.size());
        if (type != null && !type.isBlank()) {
            response.put("filter", Map.of("type", type));
        }
        response.put("sensors", sensorList);
        response.put("_links", Map.of("self", "/api/v1/sensors"));

        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sensors – register a new sensor
    // Validates that the specified roomId actually exists
    // -------------------------------------------------------------------------

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'id' is required."))
                    .build();
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'type' is required."))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'roomId' is required."))
                    .build();
        }

        // Conflict: sensor ID already exists
        if (store.sensorExists(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("CONFLICT", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Dependency validation: roomId must reference an existing room
        if (!store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the referenced roomId '"
                    + sensor.getRoomId() + "' does not exist in the system. "
                    + "Create the room first before assigning sensors to it."
            );
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.putSensor(sensor);

        // Link sensor ID to its room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.addSensorId(sensor.getId());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);
        response.put("_links", Map.of(
                "self",     "/api/v1/sensors/" + sensor.getId(),
                "readings", "/api/v1/sensors/" + sensor.getId() + "/readings",
                "room",     "/api/v1/rooms/" + sensor.getRoomId()
        ));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId} – get a specific sensor
    // -------------------------------------------------------------------------

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("NOT_FOUND", "Sensor '" + sensorId + "' does not exist."))
                    .build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensor", sensor);
        response.put("_links", Map.of(
                "self",     "/api/v1/sensors/" + sensorId,
                "readings", "/api/v1/sensors/" + sensorId + "/readings",
                "room",     "/api/v1/rooms/" + sensor.getRoomId()
        ));

        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId} – remove a sensor
    // Also unlinks the sensor from its room
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("NOT_FOUND", "Sensor '" + sensorId + "' does not exist."))
                    .build();
        }

        // Unlink from room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.removeSensorId(sensorId);
        }

        store.deleteSensor(sensorId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sensor '" + sensorId + "' has been successfully deleted.");
        response.put("_links", Map.of("sensors", "/api/v1/sensors"));

        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings
    //
    // Returns a SensorReadingResource instance bound to this sensorId.
    // JAX-RS does NOT invoke any HTTP method here — it delegates routing to the
    // returned resource object. This is the Sub-Resource Locator pattern.
    //
    // REPORT ANSWER (Part 4.1):
    // The Sub-Resource Locator pattern improves maintainability by decomposing
    // a complex API into focused, single-responsibility classes. Placing all
    // nested paths in one god-class creates an unmanageable method list and
    // prevents independent testing. By delegating /readings to
    // SensorReadingResource, each class can be developed, tested, and versioned
    // in isolation. It also enables lazy instantiation: the sub-resource is only
    // created when that path segment is matched, saving resources for unused routes.
    // -------------------------------------------------------------------------

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor existence before delegating
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            // Throw unchecked — GlobalExceptionMapper will handle it, or we can
            // let SensorReadingResource handle 404 gracefully.
            throw new NotFoundException("Sensor '" + sensorId + "' does not exist.");
        }
        return new SensorReadingResource(sensorId);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Map<String, Object> errorBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
