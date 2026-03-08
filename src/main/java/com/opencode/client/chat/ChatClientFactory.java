package com.opencode.client.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencode.client.OpenCodeClientOperations;
import com.opencode.client.model.CreateSessionRequest;
import com.opencode.client.model.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that creates and manages {@link ChatClient} instances.
 *
 * <h3>Client identity</h3>
 * Every call to {@link #getClient(String)} returns a {@code ChatClient} whose <em>unique
 * identifier</em> is the string you supply.  A new identifier is generated automatically via
 * {@link #newClient()} or {@link #newClient(String)}.
 *
 * <h3>Persistence root</h3>
 * Session files live under:
 * <pre>
 *   &lt;sessionsRoot&gt;/&lt;clientId&gt;/session.json
 * </pre>
 * The default {@code sessionsRoot} resolves to the {@code sessions/} directory inside the
 * classpath resource root of the running application.  You may override it at construction time
 * via {@link #ChatClientFactory(OpenCodeClientOperations, Path)}.
 *
 * <h3>Session reuse policy</h3>
 * <ul>
 *   <li>If the {@code <clientId>/session.json} file exists on disk, the factory loads it and
 *       reuses the existing OpenCode server session (the same {@code sessionId}).</li>
 *   <li>If the file does not exist, the factory creates a new session on the server and
 *       persists its metadata to disk before returning the {@code ChatClient}.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The factory itself is thread-safe; it uses a {@link ConcurrentHashMap} to cache live
 * {@link ChatClient} references so that the same clientId is never backed by two separate
 * instances within the same JVM process.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * // Build an OpenCodeClient (or get it from Spring context)
 * OpenCodeClientOperations api = new OpenCodeClient(webClient, props);
 *
 * // Create the factory (sessions persisted under src/main/resources/sessions/)
 * ChatClientFactory factory = new ChatClientFactory(api);
 *
 * // Create a brand-new chat client — a unique ID is generated automatically
 * ChatClient chat = factory.newClient();
 * System.out.println("clientId: " + chat.getClientId());
 *
 * // Send a message and wait for the reply
 * MessageWithParts reply = chat.send("Explain dependency injection in one sentence.");
 *
 * // Or send asynchronously
 * chat.sendAsync("Give me a Java hello-world", msg -> System.out.println(msg));
 *
 * // Re-open an existing client by its ID later (loads session from disk)
 * ChatClient sameChat = factory.getClient(chat.getClientId());
 *
 * chat.close();
 * }</pre>
 */
@Slf4j
public class ChatClientFactory {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Name of the persisted session file inside each clientId directory. */
    public static final String SESSION_FILENAME = "session.json";

    /** Default title prefix used when creating a new session on the server. */
    private static final String DEFAULT_TITLE_PREFIX = "chat-";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final OpenCodeClientOperations api;
    private final Path sessionsRoot;

    /** Live cache: clientId → ChatClient. */
    private final Map<String, ChatClient> cache = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a factory with the default sessions root directory.
     *
     * <p>The default root is resolved as:
     * <ol>
     *   <li>If the class is loaded from a JAR, the {@code sessions/} directory next to the JAR.</li>
     *   <li>Otherwise the {@code src/main/resources/sessions/} directory relative to the
     *       working directory (typical IDE / Maven run).</li>
     * </ol>
     *
     * @param api the underlying OpenCode API client (must not be {@code null})
     */
    public ChatClientFactory(OpenCodeClientOperations api) {
        this(api, resolveDefaultSessionsRoot());
    }

    /**
     * Creates a factory with an explicit sessions root directory.
     *
     * @param api          the underlying OpenCode API client (must not be {@code null})
     * @param sessionsRoot directory where {@code <clientId>/session.json} files are stored;
     *                     created if it does not exist
     */
    public ChatClientFactory(OpenCodeClientOperations api, Path sessionsRoot) {
        this.api = Objects.requireNonNull(api, "api must not be null");
        this.sessionsRoot = Objects.requireNonNull(sessionsRoot, "sessionsRoot must not be null");
        try {
            Files.createDirectories(sessionsRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create sessions root: " + sessionsRoot, e);
        }
        log.info("ChatClientFactory ready — sessions root: {}", sessionsRoot.toAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Creates a brand-new {@link ChatClient} with an auto-generated unique identifier.
     *
     * <p>The identifier is a random UUID.  A new session is created on the OpenCode server
     * and its metadata persisted to disk under {@code <sessionsRoot>/<clientId>/session.json}.
     *
     * @return the new {@code ChatClient}
     */
    public ChatClient newClient() {
        return newClient(UUID.randomUUID().toString());
    }

    /**
     * Creates a brand-new {@link ChatClient} with the given {@code clientId}.
     *
     * <p>If the {@code <clientId>} directory already exists on disk (i.e. the session was used
     * previously) a new server session is created anyway and the old file is overwritten.
     * Use {@link #getClient(String)} to resume an existing session.
     *
     * @param clientId the unique identifier for this client (must not be blank)
     * @return the new {@code ChatClient}
     */
    public ChatClient newClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }

        log.debug("newClient() clientId={}", clientId);

        // Create session on the server
        String title = DEFAULT_TITLE_PREFIX + clientId;
        Session serverSession = api.createSession(
                CreateSessionRequest.builder().title(title).build());

        Instant now = Instant.now();
        ChatSession chatSession = ChatSession.builder()
                .clientId(clientId)
                .sessionId(serverSession.getId())
                .title(title)
                .createdAt(now)
                .lastActivityAt(now)
                .build();

        Path sessionFile = sessionFile(clientId);
        persist(chatSession, sessionFile);

        ChatClient client = new ChatClient(api, chatSession, sessionFile);
        cache.put(clientId, client);

        log.info("newClient() created clientId={} sessionId={}", clientId, serverSession.getId());
        return client;
    }

    /**
     * Returns a {@link ChatClient} for the given {@code clientId}.
     *
     * <ul>
     *   <li>If the client is already in the in-memory cache, it is returned immediately.</li>
     *   <li>If a {@code session.json} file exists on disk for that {@code clientId}, it is
     *       loaded and the existing OpenCode server session is reused.</li>
     *   <li>If no file exists, a new server session is created (equivalent to
     *       {@link #newClient(String)}).</li>
     * </ul>
     *
     * @param clientId the unique identifier (must not be blank)
     * @return the {@code ChatClient} for that identifier
     */
    public ChatClient getClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }

        // Return cached instance if available
        ChatClient cached = cache.get(clientId);
        if (cached != null) {
            log.debug("getClient() cache hit clientId={}", clientId);
            return cached;
        }

        Path sessionFile = sessionFile(clientId);
        if (Files.exists(sessionFile)) {
            // Load from disk and reuse the existing server session
            ChatSession chatSession = load(sessionFile);
            log.info("getClient() loaded from disk clientId={} sessionId={}",
                    clientId, chatSession.getSessionId());
            ChatClient client = new ChatClient(api, chatSession, sessionFile);
            cache.put(clientId, client);
            return client;
        }

        // No session on disk — create a new one
        log.info("getClient() no session found for clientId={}, creating new", clientId);
        return newClient(clientId);
    }

    /**
     * Lists all clientIds for which a {@code session.json} file exists under
     * {@link #getSessionsRoot()}.
     *
     * @return array of clientId strings; empty if no sessions have been persisted yet
     */
    public String[] listClientIds() {
        try (var stream = Files.list(sessionsRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve(SESSION_FILENAME)))
                    .map(dir -> dir.getFileName().toString())
                    .toArray(String[]::new);
        } catch (IOException e) {
            log.warn("listClientIds() failed to list {}: {}", sessionsRoot, e.getMessage());
            return new String[0];
        }
    }

    /**
     * Returns the root directory under which all session subdirectories are stored.
     *
     * @return absolute {@link Path} to the sessions root
     */
    public Path getSessionsRoot() {
        return sessionsRoot;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Path sessionFile(String clientId) {
        return sessionsRoot.resolve(clientId).resolve(SESSION_FILENAME);
    }

    private static void persist(ChatSession session, Path file) {
        try {
            Files.createDirectories(file.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), session);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist ChatSession to " + file, e);
        }
    }

    private static ChatSession load(Path file) {
        try {
            return MAPPER.readValue(file.toFile(), ChatSession.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load ChatSession from " + file, e);
        }
    }

    /**
     * Resolves the default sessions root.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Locate the {@code resources/} directory on the classpath.</li>
     *   <li>Return {@code <resourcesDir>/sessions/}.</li>
     *   <li>Fall back to {@code src/main/resources/sessions/} relative to the current working
     *       directory (works well in typical Maven / IDE environments).</li>
     * </ol>
     */
    private static Path resolveDefaultSessionsRoot() {
        // Try to locate the resources directory via the classpath
        try {
            URI resourcesUri = ChatClientFactory.class
                    .getClassLoader()
                    .getResource(".")
                    .toURI();
            Path classesDir = Paths.get(resourcesUri);

            // In Maven: target/classes or target/test-classes — step up two levels to project root
            Path projectRoot = classesDir;
            // Walk up until we find a directory that is NOT named "classes" or "test-classes"
            for (int i = 0; i < 5; i++) {
                String name = projectRoot.getFileName() != null
                        ? projectRoot.getFileName().toString() : "";
                if (name.equals("classes") || name.equals("test-classes")
                        || name.equals("target")) {
                    projectRoot = projectRoot.getParent();
                } else {
                    break;
                }
            }

            Path candidate = projectRoot.resolve("src").resolve("main").resolve("resources")
                    .resolve("sessions");
            log.debug("resolveDefaultSessionsRoot() candidate={}", candidate.toAbsolutePath());
            return candidate;
        } catch (Exception e) {
            // Fallback: relative to CWD
            Path fallback = Paths.get("src", "main", "resources", "sessions");
            log.warn("resolveDefaultSessionsRoot() fallback to {}", fallback.toAbsolutePath());
            return fallback;
        }
    }
}
