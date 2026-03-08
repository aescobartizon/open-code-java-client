package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents the health status of the OpenCode server.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthResponse {

    /** Whether the server is healthy. */
    @JsonProperty("healthy")
    private boolean healthy;

    /** Server version string. */
    @JsonProperty("version")
    private String version;
}
