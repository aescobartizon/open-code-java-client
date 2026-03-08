package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents the OpenCode server configuration. Returned by GET /config and PATCH /config.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    @JsonProperty("theme")
    private String theme;

    @JsonProperty("autoshare")
    private Boolean autoshare;

    @JsonProperty("autoupdate")
    private Boolean autoupdate;

    /** Keybinds map (action -> key). */
    @JsonProperty("keybinds")
    private Map<String, Object> keybinds;

    /** Model configuration per provider. */
    @JsonProperty("models")
    private Map<String, Object> models;

    /** MCP server configurations. */
    @JsonProperty("mcp")
    private Map<String, Object> mcp;

    /** Agent configurations. */
    @JsonProperty("agents")
    private Map<String, Object> agents;
}
