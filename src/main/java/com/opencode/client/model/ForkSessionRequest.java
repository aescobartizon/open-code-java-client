package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Optional request body for POST /session/{id}/fork.
 * All fields are optional; if none are set, an empty body is sent.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForkSessionRequest {

    /** Optional message ID to fork from a specific message. */
    @JsonProperty("messageID")
    private String messageId;
}
