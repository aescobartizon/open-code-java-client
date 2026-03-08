package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a log entry request.
 *
 * <p>The {@code level} field must be lowercase (e.g. {@code "info"}, {@code "debug"},
 * {@code "warn"}, {@code "error"}) as required by the OpenCode server API.
 * Null fields are omitted from serialization to avoid server validation errors.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogRequest {

    @JsonProperty("service")
    private String service;

    /**
     * Log level — must be lowercase: {@code "info"}, {@code "debug"}, {@code "warn"}, {@code "error"}.
     */
    @JsonProperty("level")
    private String level;

    @JsonProperty("message")
    private String message;

    @JsonProperty("extra")
    private Object extra;
}
