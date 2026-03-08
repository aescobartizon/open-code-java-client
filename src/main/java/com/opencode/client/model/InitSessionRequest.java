package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Request body for POST /session/{id}/init.
 * Analyzes the application and creates an AGENTS.md file.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitSessionRequest {

    @JsonProperty("messageID")
    private String messageId;

    @JsonProperty("providerID")
    private String providerId;

    @JsonProperty("modelID")
    private String modelId;
}
