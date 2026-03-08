package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents the running status of a session.
 * Used in the response of GET /session/status.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStatus {

    /** Session ID. */
    @JsonProperty("sessionID")
    private String sessionId;

    /**
     * Current status of the session.
     * Known values: {@code "idle"}, {@code "running"}, {@code "error"}.
     */
    @JsonProperty("status")
    private String status;
}
