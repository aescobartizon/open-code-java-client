package com.opencode.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents VCS (version control) information for the current project.
 * Returned by GET /vcs.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcsInfo {

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("commit")
    private String commit;

    @JsonProperty("dirty")
    private Boolean dirty;

    @JsonProperty("root")
    private String root;
}
