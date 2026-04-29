package com.smartcampus.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Observability Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to
 * provide consistent, structured logging for every HTTP interaction without
 * cluttering individual resource methods.
 *
 * REPORT ANSWER (Part 5.5): Why use JAX-RS filters for cross-cutting concerns?
 * Inserting Logger.info() calls inside every resource method violates the
 * Single Responsibility Principle and the DRY (Don't Repeat Yourself) principle.
 * Cross-cutting concerns — logging, authentication, CORS headers, rate limiting
 * — affect every request uniformly and should be handled in one place.
 * JAX-RS filters provide exactly this:
 *   1. CONSISTENCY: Every request/response is logged identically, with no risk
 *      of a developer forgetting to add logging to a new endpoint.
 *   2. MAINTAINABILITY: Changing the log format or adding a request-ID field
 *      requires editing one class, not dozens of resource methods.
 *   3. SEPARATION OF CONCERNS: Resource classes remain focused on business
 *      logic. The filter intercepts the pipeline invisibly.
 *   4. TESTABILITY: The filter can be unit-tested independently of any resource.
 *   5. PERFORMANCE: The @Provider annotation means Jersey registers the filter
 *      once; there is no overhead of per-method reflective dispatch.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    // Stored in request context so the response filter can reference request details
    private static final String START_TIME_KEY = "requestStartTime";

    // ContainerRequestFilter – fires BEFORE the resource method is invoked 

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        long startTime = System.currentTimeMillis();
        // Store start time so response filter can compute duration
        requestContext.setProperty(START_TIME_KEY, startTime);

        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();
        String agent  = requestContext.getHeaderString("User-Agent");

        LOGGER.info(String.format(
                "[REQUEST]  --> %s %s | User-Agent: %s",
                method,
                uri,
                agent != null ? agent : "unknown"
        ));
    }

    // ContainerResponseFilter – fires AFTER the resource method has returned

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        int    status    = responseContext.getStatus();
        String method    = requestContext.getMethod();
        String uri       = requestContext.getUriInfo().getRequestUri().toString();
        Long   startTime = (Long) requestContext.getProperty(START_TIME_KEY);
        long   duration  = startTime != null ? System.currentTimeMillis() - startTime : -1;

        // Choose log level based on status: 5xx = SEVERE, 4xx = WARNING, 2xx/3xx = INFO
        String logLevel;
        if (status >= 500) {
            logLevel = "ERROR";
        } else if (status >= 400) {
            logLevel = "WARN ";
        } else {
            logLevel = "INFO ";
        }

        LOGGER.info(String.format(
                "[RESPONSE] <-- %s %s | Status: %d | Duration: %dms | Level: %s",
                method,
                uri,
                status,
                duration,
                logLevel
        ));
    }
}
