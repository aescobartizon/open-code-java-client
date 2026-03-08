package com.opencode.client.exception;

/**
 * Base exception for all OpenCode client errors.
 */
public class OpenCodeClientException extends RuntimeException {

    private final int statusCode;

    public OpenCodeClientException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public OpenCodeClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public OpenCodeClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public OpenCodeClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
