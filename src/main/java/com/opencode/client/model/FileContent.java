package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents the content of a file returned by GET /file/content.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileContent {

    @JsonProperty("path")
    private String path;

    @JsonProperty("content")
    private String content;

    @JsonProperty("encoding")
    private String encoding;
}
