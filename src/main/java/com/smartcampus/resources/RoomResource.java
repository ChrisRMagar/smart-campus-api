package com.smartcampus.resources;

import com.smartcampus.application.DataStore;
import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Room Resource - manages /api/v1/rooms
 *
 * Supports:
 *   GET  /api/v1/rooms          – list all rooms
 *   POST /api/v1/rooms          – create a new room
 *   GET  /api/v1/rooms/{roomId} – get a specific room
 *   DELETE /api/v1/rooms/{roomId} – delete a room (blocked if sensors present)
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * REPORT ANSWERS
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Q (Part 2.1): IDs vs full objects in list responses
 * Returning only IDs minimises payload size (good for large collections and
 * mobile clients on limited bandwidth), but forces every client to fire N
 * additional GET requests to retrieve details — the "N+1 problem". Returning
 * full objects costs more bandwidth per request but drastically reduces
 * round-trips, improving perceived latency. Best practice is to return full
 * objects for moderate-sized collections (as implemented here) and add
 * pagination. For very large datasets, consider a summary projection (id + name
 * only) with a "details" link per item (HATEOAS).
 *
 * Q (Part 2.2): Is DELETE idempotent?
 * YES. In this implementation the DELETE operation is idempotent. RFC 9110
 * requires that a DELETE request produce the same server state regardless of
 * how many times it is applied. The first DELETE on an existing room removes it
 * and returns 200 OK. Every subsequent DELETE for the same non-existent roomId
 * returns 404 Not Found — the server state (room absent) is identical after
 * every call. The HTTP status code may differ (200 vs 404) but idempotency
 * refers to the resource state, not the response code. No data corruption or
 * duplicate deletions can occur.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // -------------------------------------------------------------------------
    // GET /api/v1/rooms – list all rooms
    // -------------------------------------------------------------------------

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", roomList.size());
        response.put("rooms", roomList);
        response.put("_links", Map.of("self", "/api/v1/rooms"));

        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/rooms – create a new room
    // -------------------------------------------------------------------------

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        // Validate required fields
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'id' is required."))
                    .build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'name' is required."))
                    .build();
        }
        if (room.getCapacity() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("VALIDATION_ERROR", "Field 'capacity' must be a positive integer."))
                    .build();
        }

        // Conflict: room ID already exists
        if (store.roomExists(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("CONFLICT", "A room with id '" + room.getId() + "' already exists."))
                    .build();
        }

        store.putRoom(room);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);
        response.put("_links", Map.of(
                "self",  "/api/v1/rooms/" + room.getId(),
                "rooms", "/api/v1/rooms"
        ));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/rooms/{roomId} – get a specific room
    // -------------------------------------------------------------------------

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("NOT_FOUND", "Room '" + roomId + "' does not exist."))
                    .build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("room", room);
        response.put("_links", Map.of(
                "self",    "/api/v1/rooms/" + roomId,
                "rooms",   "/api/v1/rooms",
                "sensors", "/api/v1/sensors"
        ));

        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId} – delete a room
    // Business rule: cannot delete a room that still has sensors assigned
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("NOT_FOUND", "Room '" + roomId + "' does not exist."))
                    .build();
        }

        // Safety check: reject deletion if any sensors are still linked to this room
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted because it still has "
                    + room.getSensorIds().size() + " sensor(s) assigned: "
                    + room.getSensorIds() + ". Please reassign or delete all sensors first."
            );
        }

        store.deleteRoom(roomId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Room '" + roomId + "' has been successfully deleted.");
        response.put("_links", Map.of("rooms", "/api/v1/rooms"));

        return Response.ok(response).build();
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
