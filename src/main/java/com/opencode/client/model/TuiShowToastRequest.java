package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Request body for POST /tui/show-toast.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TuiShowToastRequest {

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    /** Toast variant, e.g. {@code "info"}, {@code "success"}, {@code "warning"}, {@code "error"}. */
    @JsonProperty("variant")
    private String variant;
}
