package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * Represents a message in an OpenCode session.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionID")
    private String sessionId;

    @JsonProperty("role")
    private String role;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("updated")
    private Long updated;

    /** Model info — may be a plain string or a nested object depending on server version. */
    @JsonProperty("model")
    private Object model;

    @JsonProperty("providerID")
    private String providerId;

    @JsonProperty("system")
    private String system;
}
