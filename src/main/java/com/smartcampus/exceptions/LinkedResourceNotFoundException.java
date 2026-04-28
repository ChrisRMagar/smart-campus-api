package com.smartcampus.exceptions;

/**
 * Thrown when a POST references a related resource (e.g., roomId) that does
 * not exist in the system.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 *
 * REPORT ANSWER (Part 5.2): Why 422 rather than 404?
 * HTTP 404 Not Found means the *requested resource URI* was not found — the
 * URL itself is unresolvable. HTTP 422 Unprocessable Entity means the server
 * understood the request format and the URI is valid, but the *business logic
 * of the payload* cannot be satisfied. When a client POSTs to /api/v1/sensors
 * with a valid JSON body but an unknown roomId, the endpoint /api/v1/sensors
 * exists and the JSON is syntactically correct, so 404 is semantically wrong.
 * The problem is a broken referential integrity inside the payload, making 422
 * the precise choice: "the entity is understood but contains a semantic error."
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
