package com.opencode.client;

import com.opencode.client.config.OpenCodeClientProperties;
import com.opencode.client.exception.OpenCodeClientException;
import com.opencode.client.exception.OpenCodeClientHttpException;
import com.opencode.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Java client for the OpenCode Server HTTP API.
 *
 * <p>All methods are synchronous (blocking). Use {@link #getWebClient()} for reactive access.
 *
 * <p>This client is designed to be used as a Spring bean in Spring Boot applications.
 * The Spring Boot auto-configuration ({@code OpenCodeClientAutoConfiguration}) creates it
 * automatically when properties are set.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private OpenCodeClient client;
 *
 * HealthResponse health = client.getHealth();
 * Session session = client.createSession(CreateSessionRequest.builder().title("My session").build());
 * }</pre>
 */
@Slf4j
public class OpenCodeClient implements OpenCodeClientOperations {

    private final WebClient webClient;
    private final OpenCodeClientProperties properties;

    /**
     * Creates a new {@code OpenCodeClient}.
     *
     * @param webClient  the configured {@link WebClient}
     * @param properties the client properties
     */
    public OpenCodeClient(WebClient webClient, OpenCodeClientProperties properties) {
        this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    // ===========================================================================================
    // Global
    // ===========================================================================================

    /**
     * Returns the server health status.
     * <p>GET /global/health</p>
     */
    public HealthResponse getHealth() {
        log.debug("GET /global/health");
        return get("/global/health", HealthResponse.class);
    }

    // ===========================================================================================
    // Project
    // ===========================================================================================

    /**
     * Lists all projects registered in OpenCode.
     * <p>GET /project</p>
     */
    public List<Project> listProjects() {
        log.debug("GET /project");
        return getList("/project", new ParameterizedTypeReference<List<Project>>() {});
    }

    /**
     * Returns the current project.
     * <p>GET /project/current</p>
     */
    public Project getCurrentProject() {
        log.debug("GET /project/current");
        return get("/project/current", Project.class);
    }

    // ===========================================================================================
    // Path & VCS
    // ===========================================================================================

    /**
     * Returns the current working path information.
     * <p>GET /path</p>
     */
    public PathInfo getPath() {
        log.debug("GET /path");
        return get("/path", PathInfo.class);
    }

    /**
     * Returns VCS information for the current project.
     * <p>GET /vcs</p>
     */
    public VcsInfo getVcs() {
        log.debug("GET /vcs");
        return get("/vcs", VcsInfo.class);
    }

    // ===========================================================================================
    // Instance
    // ===========================================================================================

    /**
     * Disposes the current OpenCode instance.
     * <p>POST /instance/dispose</p>
     *
     * @return true if disposed successfully
     */
    public Boolean disposeInstance() {
        log.debug("POST /instance/dispose");
        return post("/instance/dispose", null, Boolean.class);
    }

    // ===========================================================================================
    // Config
    // ===========================================================================================

    /**
     * Returns the current server configuration.
     * <p>GET /config</p>
     */
    public AppConfig getConfig() {
        log.debug("GET /config");
        return get("/config", AppConfig.class);
    }

    /**
     * Updates the server configuration.
     * <p>PATCH /config</p>
     *
     * @param config partial config object (only provided fields are updated)
     * @return the updated configuration
     */
    public AppConfig updateConfig(AppConfig config) {
        log.debug("PATCH /config");
        return patch("/config", config, AppConfig.class);
    }

    /**
     * Lists providers and their default models from config.
     * <p>GET /config/providers</p>
     *
     * @return raw map containing {@code providers} and {@code default} keys
     */
    public Map<String, Object> getConfigProviders() {
        log.debug("GET /config/providers");
        return getMap("/config/providers");
    }

    // ===========================================================================================
    // Provider
    // ===========================================================================================

    /**
     * Lists all providers (connected and available).
     * <p>GET /provider</p>
     *
     * @return raw map containing {@code all}, {@code default}, and {@code connected} keys
     */
    public Map<String, Object> listProviders() {
        log.debug("GET /provider");
        return getMap("/provider");
    }

    /**
     * Returns authentication methods available for each provider.
     * <p>GET /provider/auth</p>
     *
     * @return map of providerID -> list of auth methods
     */
    public Map<String, Object> getProviderAuth() {
        log.debug("GET /provider/auth");
        return getMap("/provider/auth");
    }

    /**
     * Initiates OAuth authorization for a provider.
     * <p>POST /provider/{id}/oauth/authorize</p>
     *
     * @param providerId the provider ID
     * @return authorization object (contains redirect URL etc.)
     */
    public Map<String, Object> authorizeProvider(String providerId) {
        log.debug("POST /provider/{}/oauth/authorize", providerId);
        return postMap("/provider/" + providerId + "/oauth/authorize", null);
    }

    /**
     * Handles the OAuth callback for a provider.
     * <p>POST /provider/{id}/oauth/callback</p>
     *
     * @param providerId the provider ID
     * @param params     callback parameters (code, state, etc.)
     * @return true if successful
     */
    public Boolean oauthCallback(String providerId, Map<String, Object> params) {
        log.debug("POST /provider/{}/oauth/callback", providerId);
        return post("/provider/" + providerId + "/oauth/callback", params, Boolean.class);
    }

    // ===========================================================================================
    // Sessions
    // ===========================================================================================

    /**
     * Lists all sessions.
     * <p>GET /session</p>
     */
    public List<Session> listSessions() {
        log.debug("GET /session");
        return getList("/session", new ParameterizedTypeReference<List<Session>>() {});
    }

    /**
     * Creates a new session.
     * <p>POST /session</p>
     *
     * @param request the creation request (title and/or parentId are optional)
     */
    public Session createSession(CreateSessionRequest request) {
        log.debug("POST /session title={}", request.getTitle());
        return post("/session", request, Session.class);
    }

    /**
     * Returns the status of all sessions.
     * <p>GET /session/status</p>
     *
     * @return map of sessionID -> SessionStatus
     */
    public Map<String, SessionStatus> getSessionStatuses() {
        log.debug("GET /session/status");
        return webClient.get()
                .uri("/session/status")
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /session/status failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, SessionStatus>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /session/status"));
    }

    /**
     * Gets a session by ID.
     * <p>GET /session/{id}</p>
     */
    public Session getSession(String sessionId) {
        log.debug("GET /session/{}", sessionId);
        return get("/session/" + sessionId, Session.class);
    }

    /**
     * Updates a session's title.
     * <p>PATCH /session/{id}</p>
     */
    public Session updateSession(String sessionId, UpdateSessionRequest request) {
        log.debug("PATCH /session/{}", sessionId);
        return patch("/session/" + sessionId, request, Session.class);
    }

    /**
     * Deletes a session.
     * <p>DELETE /session/{id}</p>
     *
     * @return true if deleted successfully
     */
    public Boolean deleteSession(String sessionId) {
        log.debug("DELETE /session/{}", sessionId);
        return delete("/session/" + sessionId, Boolean.class);
    }

    /**
     * Gets child sessions for a session.
     * <p>GET /session/{id}/children</p>
     */
    public List<Session> getSessionChildren(String sessionId) {
        log.debug("GET /session/{}/children", sessionId);
        return getList("/session/" + sessionId + "/children",
                new ParameterizedTypeReference<List<Session>>() {});
    }

    /**
     * Returns the to-do list for a session.
     * <p>GET /session/{id}/todo</p>
     */
    public List<Todo> getSessionTodos(String sessionId) {
        log.debug("GET /session/{}/todo", sessionId);
        return getList("/session/" + sessionId + "/todo",
                new ParameterizedTypeReference<List<Todo>>() {});
    }

    /**
     * Analyzes the application and creates an AGENTS.md file.
     * <p>POST /session/{id}/init</p>
     *
     * @return true if successful
     */
    public Boolean initSession(String sessionId, InitSessionRequest request) {
        log.debug("POST /session/{}/init", sessionId);
        return post("/session/" + sessionId + "/init", request, Boolean.class);
    }

    /**
     * Forks a session, optionally at a specific message.
     * <p>POST /session/{id}/fork</p>
     */
    public Session forkSession(String sessionId) {
        log.debug("POST /session/{}/fork", sessionId);
        return post("/session/" + sessionId + "/fork", null, Session.class);
    }

    /**
     * Forks a session at a specific message.
     * <p>POST /session/{id}/fork</p>
     *
     * @param request optional messageID to fork from
     */
    public Session forkSession(String sessionId, ForkSessionRequest request) {
        log.debug("POST /session/{}/fork messageID={}", sessionId, request.getMessageId());
        return post("/session/" + sessionId + "/fork", request, Session.class);
    }

    /**
     * Aborts a running session.
     * <p>POST /session/{id}/abort</p>
     *
     * @return true if aborted
     */
    public Boolean abortSession(String sessionId) {
        log.debug("POST /session/{}/abort", sessionId);
        return post("/session/" + sessionId + "/abort", null, Boolean.class);
    }

    /**
     * Shares a session (makes it publicly accessible).
     * <p>POST /session/{id}/share</p>
     */
    public Session shareSession(String sessionId) {
        log.debug("POST /session/{}/share", sessionId);
        return post("/session/" + sessionId + "/share", null, Session.class);
    }

    /**
     * Unshares a session.
     * <p>DELETE /session/{id}/share</p>
     */
    public Session unshareSession(String sessionId) {
        log.debug("DELETE /session/{}/share", sessionId);
        return delete("/session/" + sessionId + "/share", Session.class);
    }

    /**
     * Returns the diff for a session.
     * <p>GET /session/{id}/diff</p>
     *
     * @param messageId optional message ID to diff up to (may be null)
     */
    public List<FileDiff> getSessionDiff(String sessionId, String messageId) {
        String uri = "/session/" + sessionId + "/diff" + (messageId != null ? "?messageID=" + messageId : "");
        log.debug("GET /session/{}/diff", sessionId);
        return getList(uri, new ParameterizedTypeReference<List<FileDiff>>() {});
    }

    /**
     * Summarizes a session using the given model.
     * <p>POST /session/{id}/summarize</p>
     */
    public Boolean summarizeSession(String sessionId, String providerId, String modelId) {
        log.debug("POST /session/{}/summarize", sessionId);
        var body = new java.util.HashMap<String, String>();
        body.put("providerID", providerId);
        body.put("modelID", modelId);
        return post("/session/" + sessionId + "/summarize", body, Boolean.class);
    }

    /**
     * Reverts a message (undoes changes made by that message).
     * <p>POST /session/{id}/revert</p>
     */
    public Boolean revertSession(String sessionId, RevertRequest request) {
        log.debug("POST /session/{}/revert messageID={}", sessionId, request.getMessageId());
        return post("/session/" + sessionId + "/revert", request, Boolean.class);
    }

    /**
     * Restores all reverted messages in a session.
     * <p>POST /session/{id}/unrevert</p>
     */
    public Boolean unrevertSession(String sessionId) {
        log.debug("POST /session/{}/unrevert", sessionId);
        return post("/session/" + sessionId + "/unrevert", null, Boolean.class);
    }

    /**
     * Responds to a permission request in a session.
     * <p>POST /session/{id}/permissions/{permissionID}</p>
     *
     * @param permissionId the permission request ID
     * @param request      the response (allow/deny + optional remember flag)
     */
    public Boolean respondToPermission(String sessionId, String permissionId, PermissionRequest request) {
        log.debug("POST /session/{}/permissions/{}", sessionId, permissionId);
        return post("/session/" + sessionId + "/permissions/" + permissionId, request, Boolean.class);
    }

    // ===========================================================================================
    // Messages
    // ===========================================================================================

    /**
     * Lists messages in a session.
     * <p>GET /session/{id}/message</p>
     */
    public List<MessageWithParts> listMessages(String sessionId) {
        log.debug("GET /session/{}/message", sessionId);
        return getList("/session/" + sessionId + "/message",
                new ParameterizedTypeReference<List<MessageWithParts>>() {});
    }

    /**
     * Sends a message to a session and waits for the assistant's response.
     * <p>POST /session/{id}/message</p>
     *
     * @param sessionId the session ID
     * @param request   the message request
     * @return the assistant response with all parts
     */
    public MessageWithParts sendMessage(String sessionId, SendMessageRequest request) {
        log.debug("POST /session/{}/message", sessionId);
        return post("/session/" + sessionId + "/message", request, MessageWithParts.class);
    }

    /**
     * Gets a specific message by ID.
     * <p>GET /session/{id}/message/{messageID}</p>
     */
    public MessageWithParts getMessage(String sessionId, String messageId) {
        log.debug("GET /session/{}/message/{}", sessionId, messageId);
        return get("/session/" + sessionId + "/message/" + messageId, MessageWithParts.class);
    }

    /**
     * Sends a message asynchronously (no wait for response).
     * <p>POST /session/{id}/prompt_async</p>
     */
    public void sendMessageAsync(String sessionId, SendMessageRequest request) {
        log.debug("POST /session/{}/prompt_async", sessionId);
        webClient.post()
                .uri("/session/" + sessionId + "/prompt_async")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .doOnError(WebClientResponseException.class, e -> {
                    throw new OpenCodeClientHttpException(
                            "Error sending async message: " + e.getMessage(), e.getStatusCode().value(), e);
                })
                .block(properties.getResponseTimeout());
    }

    /**
     * Executes a slash command in a session.
     * <p>POST /session/{id}/command</p>
     */
    public MessageWithParts executeCommand(String sessionId, ExecuteCommandRequest request) {
        log.debug("POST /session/{}/command command={}", sessionId, request.getCommand());
        return post("/session/" + sessionId + "/command", request, MessageWithParts.class);
    }

    /**
     * Runs a shell command in a session.
     * <p>POST /session/{id}/shell</p>
     */
    public MessageWithParts shellCommand(String sessionId, ShellRequest request) {
        log.debug("POST /session/{}/shell command={}", sessionId, request.getCommand());
        return post("/session/" + sessionId + "/shell", request, MessageWithParts.class);
    }

    // ===========================================================================================
    // Commands
    // ===========================================================================================

    /**
     * Lists all available slash commands.
     * <p>GET /command</p>
     */
    public List<Command> listCommands() {
        log.debug("GET /command");
        return getList("/command", new ParameterizedTypeReference<List<Command>>() {});
    }

    // ===========================================================================================
    // Files
    // ===========================================================================================

    /**
     * Searches for text in files using a regex pattern.
     * <p>GET /find?pattern={pattern}</p>
     *
     * @param pattern the regex pattern to search for
     * @return list of match objects
     */
    public List<Map<String, Object>> findInFiles(String pattern) {
        log.debug("GET /find pattern={}", pattern);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/find").queryParam("pattern", pattern).build())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /find failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /find"));
    }

    /**
     * Finds files and directories by name (fuzzy search).
     * <p>GET /find/file?query={query}</p>
     *
     * @param query search string
     * @return list of matching file/directory paths
     */
    public List<String> findFiles(String query) {
        log.debug("GET /find/file query={}", query);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/find/file").queryParam("query", query).build())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /find/file failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /find/file"));
    }

    /**
     * Finds workspace symbols by query.
     * <p>GET /find/symbol?query={query}</p>
     *
     * @param query search string
     * @return list of symbol objects
     */
    public List<Map<String, Object>> findSymbols(String query) {
        log.debug("GET /find/symbol query={}", query);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/find/symbol").queryParam("query", query).build())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /find/symbol failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /find/symbol"));
    }

    /**
     * Lists files and directories at a given path.
     * <p>GET /file?path={path}</p>
     *
     * @param path the directory path to list (may be null for project root)
     */
    public List<FileNode> listFiles(String path) {
        log.debug("GET /file path={}", path);
        return webClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/file");
                    if (path != null) b = b.queryParam("path", path);
                    return b.build();
                })
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /file failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<List<FileNode>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /file"));
    }

    /**
     * Reads the content of a file.
     * <p>GET /file/content?path={path}</p>
     *
     * @param path the absolute file path
     */
    public FileContent getFileContent(String path) {
        log.debug("GET /file/content path={}", path);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/file/content").queryParam("path", path).build())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /file/content failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(FileContent.class)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /file/content"));
    }

    /**
     * Returns git status for all tracked files.
     * <p>GET /file/status</p>
     */
    public List<Map<String, Object>> getFileStatus() {
        log.debug("GET /file/status");
        return webClient.get()
                .uri("/file/status")
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET /file/status failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /file/status"));
    }

    // ===========================================================================================
    // LSP / Formatters / MCP
    // ===========================================================================================

    /**
     * Returns the status of all configured LSP servers.
     * <p>GET /lsp</p>
     */
    public List<LspStatus> getLspStatus() {
        log.debug("GET /lsp");
        return getList("/lsp", new ParameterizedTypeReference<List<LspStatus>>() {});
    }

    /**
     * Returns the status of all configured code formatters.
     * <p>GET /formatter</p>
     */
    public List<FormatterStatus> getFormatterStatus() {
        log.debug("GET /formatter");
        return getList("/formatter", new ParameterizedTypeReference<List<FormatterStatus>>() {});
    }

    /**
     * Returns the status of all configured MCP servers.
     * <p>GET /mcp</p>
     *
     * @return map of server name -> MCP status object
     */
    public Map<String, Object> getMcpStatus() {
        log.debug("GET /mcp");
        return getMap("/mcp");
    }

    /**
     * Adds an MCP server dynamically.
     * <p>POST /mcp</p>
     *
     * @param request name and config for the new MCP server
     * @return the resulting MCP status object
     */
    public Map<String, Object> addMcpServer(McpAddRequest request) {
        log.debug("POST /mcp name={}", request.getName());
        return postMap("/mcp", request);
    }

    // ===========================================================================================
    // Agents
    // ===========================================================================================

    /**
     * Lists all available agents.
     * <p>GET /agent</p>
     */
    public List<Agent> listAgents() {
        log.debug("GET /agent");
        return getList("/agent", new ParameterizedTypeReference<List<Agent>>() {});
    }

    // ===========================================================================================
    // Logging
    // ===========================================================================================

    /**
     * Writes a log entry to the OpenCode server.
     * <p>POST /log</p>
     */
    public Boolean log(LogRequest request) {
        log.debug("POST /log service={} level={}", request.getService(), request.getLevel());
        return post("/log", request, Boolean.class);
    }

    // ===========================================================================================
    // TUI
    // ===========================================================================================

    /**
     * Appends text to the TUI prompt.
     * <p>POST /tui/append-prompt</p>
     */
    public Boolean tuiAppendPrompt(TuiAppendPromptRequest request) {
        log.debug("POST /tui/append-prompt");
        return post("/tui/append-prompt", request, Boolean.class);
    }

    /**
     * Opens the help dialog in the TUI.
     * <p>POST /tui/open-help</p>
     */
    public Boolean tuiOpenHelp() {
        log.debug("POST /tui/open-help");
        return post("/tui/open-help", null, Boolean.class);
    }

    /**
     * Opens the session selector in the TUI.
     * <p>POST /tui/open-sessions</p>
     */
    public Boolean tuiOpenSessions() {
        log.debug("POST /tui/open-sessions");
        return post("/tui/open-sessions", null, Boolean.class);
    }

    /**
     * Opens the theme selector in the TUI.
     * <p>POST /tui/open-themes</p>
     */
    public Boolean tuiOpenThemes() {
        log.debug("POST /tui/open-themes");
        return post("/tui/open-themes", null, Boolean.class);
    }

    /**
     * Opens the model selector in the TUI.
     * <p>POST /tui/open-models</p>
     */
    public Boolean tuiOpenModels() {
        log.debug("POST /tui/open-models");
        return post("/tui/open-models", null, Boolean.class);
    }

    /**
     * Submits the current prompt in the TUI.
     * <p>POST /tui/submit-prompt</p>
     */
    public Boolean tuiSubmitPrompt() {
        log.debug("POST /tui/submit-prompt");
        return post("/tui/submit-prompt", null, Boolean.class);
    }

    /**
     * Clears the TUI prompt.
     * <p>POST /tui/clear-prompt</p>
     */
    public Boolean tuiClearPrompt() {
        log.debug("POST /tui/clear-prompt");
        return post("/tui/clear-prompt", null, Boolean.class);
    }

    /**
     * Executes a command through the TUI.
     * <p>POST /tui/execute-command</p>
     */
    public Boolean tuiExecuteCommand(TuiExecuteCommandRequest request) {
        log.debug("POST /tui/execute-command command={}", request.getCommand());
        return post("/tui/execute-command", request, Boolean.class);
    }

    /**
     * Shows a toast notification in the TUI.
     * <p>POST /tui/show-toast</p>
     */
    public Boolean tuiShowToast(TuiShowToastRequest request) {
        log.debug("POST /tui/show-toast message={}", request.getMessage());
        return post("/tui/show-toast", request, Boolean.class);
    }

    /**
     * Responds to a TUI control request.
     * <p>POST /tui/control/response</p>
     */
    public Boolean tuiControlResponse(TuiControlResponse response) {
        log.debug("POST /tui/control/response");
        return post("/tui/control/response", response, Boolean.class);
    }

    /**
     * Waits for the next TUI control request.
     * <p>GET /tui/control/next</p>
     *
     * @return the control request object
     */
    public Map<String, Object> tuiControlNext() {
        log.debug("GET /tui/control/next");
        return getMap("/tui/control/next");
    }

    // ===========================================================================================
    // Auth
    // ===========================================================================================

    /**
     * Sets authentication credentials for a provider.
     * <p>PUT /auth/{id}</p>
     *
     * @param providerId the provider ID
     * @param credentials the credentials map (schema depends on provider)
     * @return true if set successfully
     */
    public Boolean setAuth(String providerId, Map<String, Object> credentials) {
        log.debug("PUT /auth/{}", providerId);
        return webClient.put()
                .uri("/auth/" + providerId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(credentials)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new OpenCodeClientHttpException(
                                        "PUT /auth/" + providerId + " failed [" + response.statusCode().value() + "]: " + b,
                                        response.statusCode().value()))))
                .bodyToMono(Boolean.class)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from /auth/" + providerId));
    }

    // ===========================================================================================
    // Internal helpers
    // ===========================================================================================

    /**
     * Exposes the underlying {@link WebClient} for reactive/streaming use-cases.
     */
    public WebClient getWebClient() {
        return webClient;
    }

    /**
     * Returns the configured properties.
     */
    public OpenCodeClientProperties getProperties() {
        return properties;
    }

    private <T> T get(String uri, Class<T> responseType) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET " + uri + " failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(responseType)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private <T> List<T> getList(String uri, ParameterizedTypeReference<List<T>> typeRef) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET " + uri + " failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(typeRef)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private Map<String, Object> getMap(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new OpenCodeClientHttpException(
                                        "GET " + uri + " failed [" + response.statusCode().value() + "]: " + body,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        var requestSpec = webClient.post().uri(uri);

        org.springframework.web.reactive.function.client.WebClient.ResponseSpec responseSpec;
        if (body != null) {
            responseSpec = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve();
        } else {
            responseSpec = requestSpec.retrieve();
        }

        return responseSpec
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new OpenCodeClientHttpException(
                                        "POST " + uri + " failed [" + response.statusCode().value() + "]: " + b,
                                        response.statusCode().value()))))
                .bodyToMono(responseType)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private Map<String, Object> postMap(String uri, Object body) {
        var requestSpec = webClient.post().uri(uri);

        org.springframework.web.reactive.function.client.WebClient.ResponseSpec responseSpec;
        if (body != null) {
            responseSpec = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve();
        } else {
            responseSpec = requestSpec.retrieve();
        }

        return responseSpec
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new OpenCodeClientHttpException(
                                        "POST " + uri + " failed [" + response.statusCode().value() + "]: " + b,
                                        response.statusCode().value()))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private <T> T patch(String uri, Object body, Class<T> responseType) {
        return webClient.patch()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new OpenCodeClientHttpException(
                                        "PATCH " + uri + " failed [" + response.statusCode().value() + "]: " + b,
                                        response.statusCode().value()))))
                .bodyToMono(responseType)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }

    private <T> T delete(String uri, Class<T> responseType) {
        return webClient.delete()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new OpenCodeClientHttpException(
                                        "DELETE " + uri + " failed [" + response.statusCode().value() + "]: " + b,
                                        response.statusCode().value()))))
                .bodyToMono(responseType)
                .blockOptional(properties.getResponseTimeout())
                .orElseThrow(() -> new OpenCodeClientException("Timeout waiting for response from " + uri));
    }
}
