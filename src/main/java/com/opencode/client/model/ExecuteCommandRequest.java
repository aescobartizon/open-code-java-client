package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Request body for executing a slash command.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteCommandRequest {

    @JsonProperty("messageID")
    private String messageId;

    @JsonProperty("agent")
    private String agent;

    @JsonProperty("model")
    private String model;

    /** The slash command to execute (e.g. "init", "undo"). */
    @JsonProperty("command")
    private String command;

    /** Arguments to the command. */
    @JsonProperty("arguments")
    private Object arguments;
}
