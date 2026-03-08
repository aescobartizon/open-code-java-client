package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * A message part (text, tool call, tool result, etc.).
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {

    @JsonProperty("type")
    private String type;

    /** Text content for text parts. */
    @JsonProperty("text")
    private String text;

    /** Tool name for tool_call or tool_result parts. */
    @JsonProperty("toolName")
    private String toolName;

    /** Tool call id. */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Tool input (for tool_call parts). */
    @JsonProperty("input")
    private Object input;

    /** Tool output (for tool_result parts). */
    @JsonProperty("output")
    private Object output;

    /** Error, if any (for tool_result parts). */
    @JsonProperty("error")
    private String error;
}
