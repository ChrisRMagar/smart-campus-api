package com.smartcampus.exceptions;

/**
 * Thrown when a DELETE is attempted on a room that still has sensors assigned.
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
