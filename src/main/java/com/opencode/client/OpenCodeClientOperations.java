package com.opencode.client;

import com.opencode.client.model.*;

import java.util.List;
import java.util.Map;

/**
 * Contract for the OpenCode Server HTTP API client.
 *
 * <p>Defines every synchronous operation exposed by the server.
 * The default implementation is {@link OpenCodeClient}.
 *
 * <p>Declaring dependencies against this interface instead of the concrete class
 * makes unit-testing trivial — just mock or stub {@code OpenCodeClientOperations}.
 *
 * <p>Example with Mockito:
 * <pre>{@code
 * OpenCodeClientOperations client = Mockito.mock(OpenCodeClientOperations.class);
 * when(client.getHealth()).thenReturn(HealthResponse.builder().healthy(true).build());
 * }</pre>
 */
public interface OpenCodeClientOperations {

    // =========================================================================
    // Global
    // =========================================================================

    /**
     * Returns the server health status.
     * <p>GET /global/health</p>
     */
    HealthResponse getHealth();

    // =========================================================================
    // Project
    // =========================================================================

    /**
     * Lists all projects registered in OpenCode.
     * <p>GET /project</p>
     */
    List<Project> listProjects();

    /**
     * Returns the current project.
     * <p>GET /project/current</p>
     */
    Project getCurrentProject();

    // =========================================================================
    // Path & VCS
    // =========================================================================

    /**
     * Returns the current working path information.
     * <p>GET /path</p>
     */
    PathInfo getPath();

    /**
     * Returns VCS information for the current project.
     * <p>GET /vcs</p>
     */
    VcsInfo getVcs();

    // =========================================================================
    // Instance
    // =========================================================================

    /**
     * Disposes the current OpenCode instance.
     * <p>POST /instance/dispose</p>
     *
     * @return {@code true} if disposed successfully
     */
    Boolean disposeInstance();

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Returns the current server configuration.
     * <p>GET /config</p>
     */
    AppConfig getConfig();

    /**
     * Updates the server configuration.
     * <p>PATCH /config</p>
     *
     * @param config partial config object; only provided fields are updated
     * @return the updated configuration
     */
    AppConfig updateConfig(AppConfig config);

    /**
     * Lists providers and their default models from config.
     * <p>GET /config/providers</p>
     *
     * @return raw map containing {@code providers} and {@code default} keys
     */
    Map<String, Object> getConfigProviders();

    // =========================================================================
    // Provider
    // =========================================================================

    /**
     * Lists all providers (connected and available).
     * <p>GET /provider</p>
     *
     * @return raw map containing {@code all}, {@code default}, and {@code connected} keys
     */
    Map<String, Object> listProviders();

    /**
     * Returns authentication methods available for each provider.
     * <p>GET /provider/auth</p>
     *
     * @return map of providerID {@literal ->} list of auth methods
     */
    Map<String, Object> getProviderAuth();

    /**
     * Initiates OAuth authorization for a provider.
     * <p>POST /provider/{id}/oauth/authorize</p>
     *
     * @param providerId the provider ID
     * @return authorization object (contains redirect URL etc.)
     */
    Map<String, Object> authorizeProvider(String providerId);

    /**
     * Handles the OAuth callback for a provider.
     * <p>POST /provider/{id}/oauth/callback</p>
     *
     * @param providerId the provider ID
     * @param params     callback parameters (code, state, etc.)
     * @return {@code true} if successful
     */
    Boolean oauthCallback(String providerId, Map<String, Object> params);

    // =========================================================================
    // Sessions
    // =========================================================================

    /**
     * Lists all sessions.
     * <p>GET /session</p>
     */
    List<Session> listSessions();

    /**
     * Creates a new session.
     * <p>POST /session</p>
     *
     * @param request creation request (title and/or parentId are optional)
     */
    Session createSession(CreateSessionRequest request);

    /**
     * Returns the status of all sessions.
     * <p>GET /session/status</p>
     *
     * @return map of sessionID {@literal ->} {@link SessionStatus}
     */
    Map<String, SessionStatus> getSessionStatuses();

    /**
     * Gets a session by ID.
     * <p>GET /session/{id}</p>
     */
    Session getSession(String sessionId);

    /**
     * Updates a session's title.
     * <p>PATCH /session/{id}</p>
     */
    Session updateSession(String sessionId, UpdateSessionRequest request);

    /**
     * Deletes a session.
     * <p>DELETE /session/{id}</p>
     *
     * @return {@code true} if deleted successfully
     */
    Boolean deleteSession(String sessionId);

    /**
     * Gets child sessions for a session.
     * <p>GET /session/{id}/children</p>
     */
    List<Session> getSessionChildren(String sessionId);

    /**
     * Returns the to-do list for a session.
     * <p>GET /session/{id}/todo</p>
     */
    List<Todo> getSessionTodos(String sessionId);

    /**
     * Analyzes the application and creates an AGENTS.md file.
     * <p>POST /session/{id}/init</p>
     *
     * @return {@code true} if successful
     */
    Boolean initSession(String sessionId, InitSessionRequest request);

    /**
     * Forks a session without specifying a message ID.
     * <p>POST /session/{id}/fork</p>
     */
    Session forkSession(String sessionId);

    /**
     * Forks a session at a specific message.
     * <p>POST /session/{id}/fork</p>
     *
     * @param request optional messageID to fork from
     */
    Session forkSession(String sessionId, ForkSessionRequest request);

    /**
     * Aborts a running session.
     * <p>POST /session/{id}/abort</p>
     *
     * @return {@code true} if aborted
     */
    Boolean abortSession(String sessionId);

    /**
     * Shares a session (makes it publicly accessible).
     * <p>POST /session/{id}/share</p>
     */
    Session shareSession(String sessionId);

    /**
     * Unshares a session.
     * <p>DELETE /session/{id}/share</p>
     */
    Session unshareSession(String sessionId);

    /**
     * Returns the diff for a session.
     * <p>GET /session/{id}/diff</p>
     *
     * @param messageId optional message ID to diff up to; may be {@code null}
     */
    List<FileDiff> getSessionDiff(String sessionId, String messageId);

    /**
     * Summarizes a session using the given model.
     * <p>POST /session/{id}/summarize</p>
     *
     * @param providerId the provider ID (e.g. {@code "openai"})
     * @param modelId    the model ID (e.g. {@code "gpt-4o"})
     * @return {@code true} if the operation was accepted
     */
    Boolean summarizeSession(String sessionId, String providerId, String modelId);

    /**
     * Reverts a message (undoes changes made by that message).
     * <p>POST /session/{id}/revert</p>
     */
    Boolean revertSession(String sessionId, RevertRequest request);

    /**
     * Restores all reverted messages in a session.
     * <p>POST /session/{id}/unrevert</p>
     */
    Boolean unrevertSession(String sessionId);

    /**
     * Responds to a permission request in a session.
     * <p>POST /session/{id}/permissions/{permissionID}</p>
     *
     * @param sessionId    the session ID
     * @param permissionId the permission request ID
     * @param request      the response (allow/deny + optional remember flag)
     */
    Boolean respondToPermission(String sessionId, String permissionId, PermissionRequest request);

    // =========================================================================
    // Messages
    // =========================================================================

    /**
     * Lists messages in a session.
     * <p>GET /session/{id}/message</p>
     */
    List<MessageWithParts> listMessages(String sessionId);

    /**
     * Sends a message to a session and waits for the assistant's response.
     * <p>POST /session/{id}/message</p>
     *
     * @param sessionId the session ID
     * @param request   the message request
     * @return the assistant response with all parts
     */
    MessageWithParts sendMessage(String sessionId, SendMessageRequest request);

    /**
     * Gets a specific message by ID.
     * <p>GET /session/{id}/message/{messageID}</p>
     */
    MessageWithParts getMessage(String sessionId, String messageId);

    /**
     * Sends a message asynchronously (fire-and-forget; does not wait for assistant reply).
     * <p>POST /session/{id}/prompt_async</p>
     */
    void sendMessageAsync(String sessionId, SendMessageRequest request);

    /**
     * Executes a slash command in a session.
     * <p>POST /session/{id}/command</p>
     */
    MessageWithParts executeCommand(String sessionId, ExecuteCommandRequest request);

    /**
     * Runs a shell command in a session.
     * <p>POST /session/{id}/shell</p>
     */
    MessageWithParts shellCommand(String sessionId, ShellRequest request);

    // =========================================================================
    // Commands
    // =========================================================================

    /**
     * Lists all available slash commands.
     * <p>GET /command</p>
     */
    List<Command> listCommands();

    // =========================================================================
    // Files
    // =========================================================================

    /**
     * Searches for text in files using a regex pattern.
     * <p>GET /find?pattern={pattern}</p>
     *
     * @param pattern the regex pattern to search for
     * @return list of match objects
     */
    List<Map<String, Object>> findInFiles(String pattern);

    /**
     * Finds files and directories by name (fuzzy search).
     * <p>GET /find/file?query={query}</p>
     */
    List<String> findFiles(String query);

    /**
     * Finds workspace symbols by query.
     * <p>GET /find/symbol?query={query}</p>
     */
    List<Map<String, Object>> findSymbols(String query);

    /**
     * Lists files and directories at a given path.
     * <p>GET /file?path={path}</p>
     *
     * @param path the directory path to list; may be {@code null} for project root
     */
    List<FileNode> listFiles(String path);

    /**
     * Reads the content of a file.
     * <p>GET /file/content?path={path}</p>
     *
     * @param path the absolute file path
     */
    FileContent getFileContent(String path);

    /**
     * Returns git status for all tracked files.
     * <p>GET /file/status</p>
     */
    List<Map<String, Object>> getFileStatus();

    // =========================================================================
    // LSP / Formatters / MCP
    // =========================================================================

    /**
     * Returns the status of all configured LSP servers.
     * <p>GET /lsp</p>
     */
    List<LspStatus> getLspStatus();

    /**
     * Returns the status of all configured code formatters.
     * <p>GET /formatter</p>
     */
    List<FormatterStatus> getFormatterStatus();

    /**
     * Returns the status of all configured MCP servers.
     * <p>GET /mcp</p>
     *
     * @return map of server name {@literal ->} MCP status object
     */
    Map<String, Object> getMcpStatus();

    /**
     * Adds an MCP server dynamically.
     * <p>POST /mcp</p>
     *
     * @param request name and config for the new MCP server
     * @return the resulting MCP status object
     */
    Map<String, Object> addMcpServer(McpAddRequest request);

    // =========================================================================
    // Agents
    // =========================================================================

    /**
     * Lists all available agents.
     * <p>GET /agent</p>
     */
    List<Agent> listAgents();

    // =========================================================================
    // Logging
    // =========================================================================

    /**
     * Writes a log entry to the OpenCode server.
     * <p>POST /log</p>
     */
    Boolean log(LogRequest request);

    // =========================================================================
    // TUI
    // =========================================================================

    /**
     * Appends text to the TUI prompt.
     * <p>POST /tui/append-prompt</p>
     */
    Boolean tuiAppendPrompt(TuiAppendPromptRequest request);

    /**
     * Opens the help dialog in the TUI.
     * <p>POST /tui/open-help</p>
     */
    Boolean tuiOpenHelp();

    /**
     * Opens the session selector in the TUI.
     * <p>POST /tui/open-sessions</p>
     */
    Boolean tuiOpenSessions();

    /**
     * Opens the theme selector in the TUI.
     * <p>POST /tui/open-themes</p>
     */
    Boolean tuiOpenThemes();

    /**
     * Opens the model selector in the TUI.
     * <p>POST /tui/open-models</p>
     */
    Boolean tuiOpenModels();

    /**
     * Submits the current prompt in the TUI.
     * <p>POST /tui/submit-prompt</p>
     */
    Boolean tuiSubmitPrompt();

    /**
     * Clears the TUI prompt.
     * <p>POST /tui/clear-prompt</p>
     */
    Boolean tuiClearPrompt();

    /**
     * Executes a command through the TUI.
     * <p>POST /tui/execute-command</p>
     */
    Boolean tuiExecuteCommand(TuiExecuteCommandRequest request);

    /**
     * Shows a toast notification in the TUI.
     * <p>POST /tui/show-toast</p>
     */
    Boolean tuiShowToast(TuiShowToastRequest request);

    /**
     * Responds to a TUI control request.
     * <p>POST /tui/control/response</p>
     */
    Boolean tuiControlResponse(TuiControlResponse response);

    /**
     * Waits for the next TUI control request.
     * <p>GET /tui/control/next</p>
     *
     * @return the control request object
     */
    Map<String, Object> tuiControlNext();

    // =========================================================================
    // Auth
    // =========================================================================

    /**
     * Sets authentication credentials for a provider.
     * <p>PUT /auth/{id}</p>
     *
     * @param providerId  the provider ID
     * @param credentials the credentials map (schema depends on provider)
     * @return {@code true} if set successfully
     */
    Boolean setAuth(String providerId, Map<String, Object> credentials);
}
