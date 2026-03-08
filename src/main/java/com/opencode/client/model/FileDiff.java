package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a file diff produced by a session.
 * Returned by GET /session/{id}/diff.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDiff {

    @JsonProperty("file")
    private String file;

    @JsonProperty("added")
    private Integer added;

    @JsonProperty("removed")
    private Integer removed;

    @JsonProperty("patch")
    private String patch;
}
