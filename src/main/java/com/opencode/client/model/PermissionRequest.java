package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Request body for POST /session/{id}/permissions/{permissionID}.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionRequest {

    /** The response to the permission request (e.g. "allow", "deny"). */
    @JsonProperty("response")
    private String response;

    /** If true, remember this permission decision for future requests. */
    @JsonProperty("remember")
    private Boolean remember;
}
