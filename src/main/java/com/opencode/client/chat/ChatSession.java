package com.opencode.client.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Metadata for a {@link ChatClient} session, persisted as JSON under
 * {@code resources/sessions/<clientId>/session.json}.
 *
 * <p>Only the session metadata is persisted (not the full message history).
 * Message history is always fetched live from the OpenCode server.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatSession {

    /** Unique identifier assigned by {@link ChatClientFactory} at creation time. */
    @JsonProperty("clientId")
    private String clientId;

    /** Session ID assigned by the OpenCode server. */
    @JsonProperty("sessionId")
    private String sessionId;

    /** Human-readable title used when creating the server session. */
    @JsonProperty("title")
    private String title;

    /** Instant when this {@code ChatSession} was first created (ISO-8601). */
    @JsonProperty("createdAt")
    private Instant createdAt;

    /** Instant of the last successful message exchange (updated on every send). */
    @JsonProperty("lastActivityAt")
    private Instant lastActivityAt;
}
