package com.opencode.client.exception;

/**
 * Thrown when the server responds with 4xx status code.
 */
public class OpenCodeClientHttpException extends OpenCodeClientException {

    public OpenCodeClientHttpException(String message, int statusCode) {
        super(message, statusCode);
    }

    public OpenCodeClientHttpException(String message, int statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }

    public boolean isNotFound() {
        return getStatusCode() == 404;
    }

    public boolean isUnauthorized() {
        return getStatusCode() == 401;
    }

    public boolean isBadRequest() {
        return getStatusCode() == 400;
    }
}
