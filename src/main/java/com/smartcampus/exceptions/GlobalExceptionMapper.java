package com.smartcampus.exceptions;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global catch-all Exception Mapper – HTTP 500 Internal Server Error.
 *
 * Intercepts ANY uncaught Throwable that escapes all other mappers, preventing
 * raw Java stack traces from reaching the client. This is the "safety net".
 *
 * IMPORTANT: WebApplicationException subclasses (like NotFoundException) carry
 * their own status code and must NOT be overridden to 500, so they are
 * re-thrown / passed through here.
 *
 * REPORT ANSWER (Part 5.4): Cybersecurity risk of exposing stack traces
 * A stack trace exposes:
 *   1. Internal class names and package structure, helping attackers map the
 *      codebase and identify frameworks/versions with known CVEs.
 *   2. File names and line numbers, enabling precise targeted exploits.
 *   3. Third-party library versions (visible in stack frames), making it
 *      trivial to search public vulnerability databases (NVD, CVE) for
 *      matching exploits.
 *   4. Server-side logic flow, revealing business logic that could be abused.
 *   5. Internal IP addresses or hostnames present in some exception messages,
 *      aiding network reconnaissance.
 * By returning a generic 500 message, none of this information leaks. The
 * full detail is still written to the server log (visible only to operators).
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {

        // Pass through JAX-RS exceptions (404, 405, 415, etc.) — they have their
        // own semantically correct status codes and should not be swallowed as 500.
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            Response original = wae.getResponse();
            // Wrap the response in a consistent JSON format
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status",    original.getStatus());
            body.put("error",     Response.Status.fromStatusCode(original.getStatus()) != null
                    ? Response.Status.fromStatusCode(original.getStatus()).name()
                    : "HTTP_ERROR");
            body.put("message",   exception.getMessage() != null
                    ? exception.getMessage()
                    : "An HTTP error occurred.");
            body.put("timestamp", System.currentTimeMillis());

            return Response.status(original.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }

        // Log the full exception server-side for operator visibility
        LOGGER.log(Level.SEVERE, "Unexpected error – returning 500 to client: "
                + exception.getClass().getName() + ": " + exception.getMessage(), exception);

        // Return a safe, generic response to the client – no stack trace exposed
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  500);
        body.put("error",   "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred on the server. "
                + "The operations team has been notified. Please try again later.");
        body.put("timestamp", System.currentTimeMillis());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
