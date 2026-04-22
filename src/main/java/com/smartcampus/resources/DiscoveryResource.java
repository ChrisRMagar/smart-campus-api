package com.smartcampus.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery / Root resource - GET /api/v1
 *
 * Provides essential API metadata including version, contact information, and
 * hypermedia links to primary resource collections.
 *
 * HATEOAS (report answer):
 * Hypermedia As The Engine Of Application State is considered a hallmark of
 * mature REST design because it makes the API self-describing. Instead of
 * forcing client developers to memorise every URL from static documentation,
 * each response embeds the URIs required to take the next logical action. This
 * decouples the client from hard-coded paths: if a URL changes, the server
 * updates the link and all HATEOAS-aware clients automatically adapt without
 * code changes. It also lowers the on-boarding barrier because a developer can
 * navigate the entire API starting from a single well-known root URL, just like
 * browsing a website by following hyperlinks.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("api", "Smart Campus Sensor & Room Management API");
        metadata.put("version", "1.0.0");
        metadata.put("description",
                "RESTful API for managing campus rooms, sensors, and sensor readings.");
        metadata.put("contact", Map.of(
                "name",  "Smart Campus Admin",
                "email", "smartcampus@westminster.ac.uk",
                "university", "University of Westminster"
        ));
        metadata.put("status", "OPERATIONAL");
        metadata.put("timestamp", System.currentTimeMillis());

        // Hypermedia links (HATEOAS) - clients can discover all entry points here
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",     "/api/v1");
        links.put("rooms",    "/api/v1/rooms");
        links.put("sensors",  "/api/v1/sensors");
        metadata.put("_links", links);

        // Primary resource collection shortcuts
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        metadata.put("resources", resources);

        return Response.ok(metadata).build();
    }
}
