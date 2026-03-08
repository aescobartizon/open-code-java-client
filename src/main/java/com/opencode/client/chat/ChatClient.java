package com.opencode.client.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencode.client.OpenCodeClientOperations;
import com.opencode.client.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * High-level chat client that wraps an {@link OpenCodeClientOperations} instance and manages
 * a single OpenCode server session identified by a {@link ChatSession}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>A {@code ChatClient} is always obtained via {@link ChatClientFactory} — never constructed
 *       directly.</li>
 *   <li>The factory ensures that the {@link ChatSession} is persisted on disk before this object
 *       is handed to the caller.</li>
 *   <li>Call {@link #close()} when the conversation is finished to release the background thread
 *       used by the async send facility.</li>
 * </ol>
 *
 * <h3>Sending messages</h3>
 * <ul>
 *   <li>{@link #send(String)} — synchronous, blocks until the assistant replies.</li>
 *   <li>{@link #sendAsync(String, Consumer)} — fires {@code POST /session/:id/prompt_async},
 *       returns immediately, then polls the message list in a background thread and invokes the
 *       {@code onMessage} callback once the new assistant reply has appeared.</li>
 * </ul>
 *
 * <h3>Session persistence</h3>
 * The {@link ChatSession} metadata (clientId, sessionId, title, timestamps) is persisted as JSON
 * in the directory managed by the {@link ChatClientFactory}.  After every {@link #send} or
 * successful {@link #sendAsync} callback the {@code lastActivityAt} timestamp is updated and
 * written back to disk.
 */
@Slf4j
public class ChatClient implements AutoCloseable {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** How often the async poller checks for new messages (milliseconds). */
    private static final long POLL_INTERVAL_MS = 1_000;

    /** Maximum time the async poller waits before giving up (milliseconds). */
    private static final long POLL_TIMEOUT_MS = 120_000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final OpenCodeClientOperations api;
    private ChatSession session;
    private final Path sessionFile;

    /** Single-thread executor used for all async polling tasks. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-async-" + Thread.currentThread().getId());
        t.setDaemon(true);
        return t;
    });

    // -----------------------------------------------------------------------
    // Package-private constructor (use ChatClientFactory)
    // -----------------------------------------------------------------------

    /**
     * Creates a {@code ChatClient}.
     *
     * @param api         the underlying OpenCode API client
     * @param session     the session metadata (already persisted by the factory)
     * @param sessionFile path to the JSON file where {@code session} is persisted
     */
    ChatClient(OpenCodeClientOperations api, ChatSession session, Path sessionFile) {
        this.api = api;
        this.session = session;
        this.sessionFile = sessionFile;
    }

    // -----------------------------------------------------------------------
    // Public API — synchronous
    // -----------------------------------------------------------------------

    /**
     * Sends a plain-text message to the session and waits for the assistant reply.
     *
     * <p>This method blocks until the OpenCode server returns the assistant's
     * {@link MessageWithParts}.
     *
     * @param text the user message text (must not be blank)
     * @return the assistant reply
     * @throws IllegalArgumentException if {@code text} is null or blank
     */
    public MessageWithParts send(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        log.debug("[{}] send() sessionId={}", session.getClientId(), session.getSessionId());

        SendMessageRequest request = buildRequest(text);
        MessageWithParts reply = api.sendMessage(session.getSessionId(), request);

        touchActivity();
        return reply;
    }

    /**
     * Sends a plain-text message asynchronously via {@code POST /session/:id/prompt_async}.
     *
     * <p>Returns immediately after the fire-and-forget POST.  A background thread then polls
     * {@code GET /session/:id/message} until a new assistant reply appears (or the polling
     * timeout of {@value #POLL_TIMEOUT_MS} ms is reached), after which {@code onMessage} is
     * called with the assistant reply.
     *
     * <p>If polling times out or an error occurs the {@code onMessage} callback is never
     * invoked and the error is logged at {@code WARN} level.
     *
     * @param text      the user message text (must not be blank)
     * @param onMessage callback invoked on the background thread with the assistant reply
     * @throws IllegalArgumentException if {@code text} is null or blank
     */
    public void sendAsync(String text, Consumer<MessageWithParts> onMessage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }

        // Capture the current message count before firing the async call
        int countBefore;
        try {
            countBefore = api.listMessages(session.getSessionId()).size();
        } catch (Exception e) {
            log.warn("[{}] sendAsync() could not get baseline message count: {}",
                    session.getClientId(), e.getMessage());
            countBefore = 0;
        }
        final int baseline = countBefore;

        log.debug("[{}] sendAsync() sessionId={} baseline={}", session.getClientId(),
                session.getSessionId(), baseline);

        // Fire-and-forget POST
        SendMessageRequest request = buildRequest(text);
        api.sendMessageAsync(session.getSessionId(), request);

        // Poll in background
        executor.submit(() -> pollForReply(baseline, onMessage));
    }

    /**
     * Returns all messages in this session (fetched live from the server).
     *
     * @return list of messages with their parts; never {@code null}
     */
    public List<MessageWithParts> listMessages() {
        return api.listMessages(session.getSessionId());
    }

    /**
     * Returns the current session metadata snapshot.
     *
     * @return a copy of the current {@link ChatSession}
     */
    public ChatSession getSession() {
        return session;
    }

    /**
     * Returns the unique client identifier assigned by the factory.
     *
     * @return the clientId string
     */
    public String getClientId() {
        return session.getClientId();
    }

    /**
     * Shuts down the background executor.  Any in-flight async poll will be interrupted.
     * After calling {@code close()} this instance should no longer be used.
     */
    @Override
    public void close() {
        log.debug("[{}] close()", session.getClientId());
        executor.shutdownNow();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static SendMessageRequest buildRequest(String text) {
        return SendMessageRequest.builder()
                .parts(List.of(Part.builder().type("text").text(text).build()))
                .build();
    }

    /**
     * Polls the message list until a new assistant reply appears, then fires the callback.
     */
    private void pollForReply(int baseline, Consumer<MessageWithParts> onMessage) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        log.debug("[{}] pollForReply() baseline={}", session.getClientId(), baseline);

        while (System.currentTimeMillis() < deadline) {
            if (Thread.currentThread().isInterrupted()) {
                log.debug("[{}] pollForReply() interrupted", session.getClientId());
                return;
            }
            try {
                List<MessageWithParts> messages = api.listMessages(session.getSessionId());
                // Look for a new assistant message beyond the baseline
                if (messages.size() > baseline) {
                    // Find the last assistant message added since the baseline
                    MessageWithParts reply = messages.stream()
                            .skip(baseline)
                            .filter(m -> m.getInfo() != null
                                    && "assistant".equals(m.getInfo().getRole()))
                            .reduce((first, second) -> second) // last one
                            .orElse(null);

                    if (reply != null) {
                        log.debug("[{}] pollForReply() got reply msgId={}",
                                session.getClientId(), reply.getInfo().getId());
                        touchActivity();
                        onMessage.accept(reply);
                        return;
                    }
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("[{}] pollForReply() error: {}", session.getClientId(), e.getMessage());
                return;
            }
        }
        log.warn("[{}] pollForReply() timed out after {}ms", session.getClientId(), POLL_TIMEOUT_MS);
    }

    /** Updates {@code lastActivityAt} and persists the session file. */
    private void touchActivity() {
        session.setLastActivityAt(Instant.now());
        persistSession();
    }

    /** Writes the current {@link ChatSession} to {@link #sessionFile}. */
    private void persistSession() {
        try {
            Files.createDirectories(sessionFile.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(sessionFile.toFile(), session);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist ChatSession to " + sessionFile, e);
        }
    }
}
