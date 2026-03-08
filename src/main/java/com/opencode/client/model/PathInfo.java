package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents the current working path returned by GET /path.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathInfo {

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("root")
    private String root;

    @JsonProperty("config")
    private String config;
}
