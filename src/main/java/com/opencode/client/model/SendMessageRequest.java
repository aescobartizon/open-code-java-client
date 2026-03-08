package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Request body for sending a message to a session.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendMessageRequest {

    /** Optional client-generated message ID. */
    @JsonProperty("messageID")
    private String messageId;

    /** Model override (format: providerID/modelID). */
    @JsonProperty("model")
    private String model;

    /** Agent to use. */
    @JsonProperty("agent")
    private String agent;

    /** If true, no assistant reply is generated. */
    @JsonProperty("noReply")
    private Boolean noReply;

    /** System prompt override. */
    @JsonProperty("system")
    private String system;

    /** Message parts (required). */
    @JsonProperty("parts")
    private List<Part> parts;
}
