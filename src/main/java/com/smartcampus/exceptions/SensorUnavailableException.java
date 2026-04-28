package com.smartcampus.exceptions;

/**
 * Thrown when a POST reading is attempted on a sensor whose status is
 * "MAINTENANCE" (physically disconnected / unavailable).
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
