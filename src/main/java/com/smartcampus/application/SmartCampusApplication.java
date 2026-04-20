package com.smartcampus.application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 *
 * The @ApplicationPath annotation establishes the versioned base URI for all
 * API resources. Every resource path declared in the project is relative to
 * this root, e.g., /api/v1/rooms, /api/v1/sensors, etc.
 
 * JAX-RS Resource Lifecycle (report answer):
 * By default, JAX-RS creates a NEW resource class instance for EVERY incoming
 * HTTP request (per-request scope). This means resource classes are NOT
 * singletons. Consequences for in-memory data management:
 *   - Instance fields on resource classes are discarded after each request, so
 *     mutable data MUST NOT be stored as instance fields.
 *   - All shared state is held in the DataStore singleton, which lives for the
 *     entire JVM lifetime.
 *   - Because multiple requests can arrive concurrently, the DataStore uses
 *     ConcurrentHashMap and synchronizedList to prevent race conditions and
 *     data loss without requiring explicit synchronized blocks.
 */
@ApplicationPath("/")
public class SmartCampusApplication extends Application {
    // Jersey auto-scans the package; no manual registration needed when using the Grizzly launcher.
}
