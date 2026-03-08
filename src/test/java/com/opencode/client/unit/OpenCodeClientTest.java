package com.opencode.client.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.opencode.client.OpenCodeClient;
import com.opencode.client.config.OpenCodeClientProperties;
import com.opencode.client.exception.OpenCodeClientHttpException;
import com.opencode.client.model.*;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link OpenCodeClient} using WireMock to mock the HTTP server.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Correct HTTP method and path for each operation</li>
 *   <li>Request body serialization</li>
 *   <li>Response deserialization</li>
 *   <li>Error handling (4xx, 5xx)</li>
 * </ul>
 */
@DisplayName("OpenCodeClient - Unit Tests (WireMock)")
class OpenCodeClientTest {

    private static WireMockServer wireMock;
    private static ObjectMapper objectMapper;
    private OpenCodeClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        objectMapper = new ObjectMapper();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        OpenCodeClientProperties props = new OpenCodeClientProperties();
        props.setBaseUrl("http://localhost:" + wireMock.port());
        props.setConnectTimeout(Duration.ofSeconds(5));
        props.setResponseTimeout(Duration.ofSeconds(10));

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        WebClient webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        client = new OpenCodeClient(webClient, props);
    }

    // ========================================================================
    // Health
    // ========================================================================

    @Test
    @DisplayName("getHealth() should return healthy=true when server is up")
    void getHealth_returnsHealthyResponse() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/global/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                HealthResponse.builder().healthy(true).version("1.0.0").build()))));

        HealthResponse health = client.getHealth();

        assertThat(health).isNotNull();
        assertThat(health.isHealthy()).isTrue();
        assertThat(health.getVersion()).isEqualTo("1.0.0");

        wireMock.verify(getRequestedFor(urlEqualTo("/global/health")));
    }

    @Test
    @DisplayName("getHealth() should throw OpenCodeClientHttpException on 503")
    void getHealth_throwsOnServerError() {
        wireMock.stubFor(get(urlEqualTo("/global/health"))
                .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        assertThatThrownBy(() -> client.getHealth())
                .isInstanceOf(OpenCodeClientHttpException.class)
                .extracting(e -> ((OpenCodeClientHttpException) e).getStatusCode())
                .isEqualTo(503);
    }

    // ========================================================================
    // Path & VCS
    // ========================================================================

    @Test
    @DisplayName("getPath() should GET /path and return PathInfo")
    void getPath_returnsPathInfo() throws Exception {
        PathInfo expected = PathInfo.builder()
                .cwd("/workspace/project")
                .root("/workspace")
                .config("/home/user/.config/opencode")
                .build();

        wireMock.stubFor(get(urlEqualTo("/path"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expected))));

        PathInfo result = client.getPath();

        assertThat(result).isNotNull();
        assertThat(result.getCwd()).isEqualTo("/workspace/project");
        assertThat(result.getRoot()).isEqualTo("/workspace");
        assertThat(result.getConfig()).isEqualTo("/home/user/.config/opencode");
        wireMock.verify(getRequestedFor(urlEqualTo("/path")));
    }

    @Test
    @DisplayName("getVcs() should GET /vcs and return VcsInfo")
    void getVcs_returnsVcsInfo() throws Exception {
        VcsInfo expected = VcsInfo.builder()
                .branch("main")
                .commit("abc123def456")
                .dirty(false)
                .root("/workspace/project")
                .build();

        wireMock.stubFor(get(urlEqualTo("/vcs"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expected))));

        VcsInfo result = client.getVcs();

        assertThat(result).isNotNull();
        assertThat(result.getBranch()).isEqualTo("main");
        assertThat(result.getCommit()).isEqualTo("abc123def456");
        assertThat(result.getDirty()).isFalse();
        wireMock.verify(getRequestedFor(urlEqualTo("/vcs")));
    }

    // ========================================================================
    // Instance
    // ========================================================================

    @Test
    @DisplayName("disposeInstance() should POST /instance/dispose with no body")
    void disposeInstance_postsToDisposeAndReturnsTrue() {
        wireMock.stubFor(post(urlEqualTo("/instance/dispose"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.disposeInstance();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/instance/dispose")));
    }

    // ========================================================================
    // Config
    // ========================================================================

    @Test
    @DisplayName("getConfig() should GET /config and return AppConfig")
    void getConfig_returnsAppConfig() throws Exception {
        AppConfig expected = AppConfig.builder()
                .theme("dark")
                .autoshare(false)
                .autoupdate(true)
                .build();

        wireMock.stubFor(get(urlEqualTo("/config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expected))));

        AppConfig result = client.getConfig();

        assertThat(result).isNotNull();
        assertThat(result.getTheme()).isEqualTo("dark");
        assertThat(result.getAutoshare()).isFalse();
        assertThat(result.getAutoupdate()).isTrue();
        wireMock.verify(getRequestedFor(urlEqualTo("/config")));
    }

    @Test
    @DisplayName("updateConfig() should PATCH /config and return updated AppConfig")
    void updateConfig_patchesAndReturnsConfig() throws Exception {
        AppConfig request = AppConfig.builder().theme("light").build();
        AppConfig updated = AppConfig.builder().theme("light").autoshare(false).build();

        wireMock.stubFor(patch(urlEqualTo("/config"))
                .withRequestBody(matchingJsonPath("$.theme", equalTo("light")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(updated))));

        AppConfig result = client.updateConfig(request);

        assertThat(result.getTheme()).isEqualTo("light");
        wireMock.verify(patchRequestedFor(urlEqualTo("/config")));
    }

    @Test
    @DisplayName("getConfigProviders() should GET /config/providers and return map")
    void getConfigProviders_returnsMap() throws Exception {
        Map<String, Object> body = Map.of("providers", List.of(), "default", "openai");

        wireMock.stubFor(get(urlEqualTo("/config/providers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.getConfigProviders();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("default");
        assertThat(result.get("default")).isEqualTo("openai");
        wireMock.verify(getRequestedFor(urlEqualTo("/config/providers")));
    }

    // ========================================================================
    // Provider
    // ========================================================================

    @Test
    @DisplayName("listProviders() should GET /provider and return map")
    void listProviders_returnsMap() throws Exception {
        Map<String, Object> body = Map.of("all", List.of(), "default", "openai", "connected", List.of());

        wireMock.stubFor(get(urlEqualTo("/provider"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.listProviders();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("default");
        wireMock.verify(getRequestedFor(urlEqualTo("/provider")));
    }

    @Test
    @DisplayName("getProviderAuth() should GET /provider/auth and return map")
    void getProviderAuth_returnsMap() throws Exception {
        Map<String, Object> body = Map.of("openai", List.of("apikey"), "anthropic", List.of("apikey", "oauth"));

        wireMock.stubFor(get(urlEqualTo("/provider/auth"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.getProviderAuth();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("openai");
        wireMock.verify(getRequestedFor(urlEqualTo("/provider/auth")));
    }

    @Test
    @DisplayName("authorizeProvider() should POST /provider/{id}/oauth/authorize")
    void authorizeProvider_postsAndReturnsMap() throws Exception {
        Map<String, Object> body = Map.of("url", "https://accounts.openai.com/authorize?...");

        wireMock.stubFor(post(urlEqualTo("/provider/openai/oauth/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.authorizeProvider("openai");

        assertThat(result).isNotNull();
        assertThat(result).containsKey("url");
        wireMock.verify(postRequestedFor(urlEqualTo("/provider/openai/oauth/authorize")));
    }

    @Test
    @DisplayName("oauthCallback() should POST /provider/{id}/oauth/callback")
    void oauthCallback_postsAndReturnsTrue() throws Exception {
        Map<String, Object> params = Map.of("code", "auth-code-123", "state", "state-abc");

        wireMock.stubFor(post(urlEqualTo("/provider/openai/oauth/callback"))
                .withRequestBody(matchingJsonPath("$.code", equalTo("auth-code-123")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.oauthCallback("openai", params);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/provider/openai/oauth/callback")));
    }

    // ========================================================================
    // Sessions
    // ========================================================================

    @Test
    @DisplayName("listSessions() should return list of sessions")
    void listSessions_returnsSessionList() throws Exception {
        List<Session> sessions = List.of(
                Session.builder().id("sess-1").title("Test Session 1").build(),
                Session.builder().id("sess-2").title("Test Session 2").build()
        );

        wireMock.stubFor(get(urlEqualTo("/session"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(sessions))));

        List<Session> result = client.listSessions();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("sess-1");
        assertThat(result.get(1).getTitle()).isEqualTo("Test Session 2");
    }

    @Test
    @DisplayName("createSession() should POST correct body and return Session")
    void createSession_postsCorrectBodyAndReturnsSession() throws Exception {
        CreateSessionRequest request = CreateSessionRequest.builder()
                .title("New Session")
                .build();

        Session expectedSession = Session.builder()
                .id("new-sess-id")
                .title("New Session")
                .build();

        wireMock.stubFor(post(urlEqualTo("/session"))
                .withRequestBody(matchingJsonPath("$.title", equalTo("New Session")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedSession))));

        Session result = client.createSession(request);

        assertThat(result.getId()).isEqualTo("new-sess-id");
        assertThat(result.getTitle()).isEqualTo("New Session");
    }

    @Test
    @DisplayName("getSession() should GET /session/{id}")
    void getSession_returnsSession() throws Exception {
        Session session = Session.builder().id("sess-123").title("My Session").build();

        wireMock.stubFor(get(urlEqualTo("/session/sess-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(session))));

        Session result = client.getSession("sess-123");

        assertThat(result.getId()).isEqualTo("sess-123");
        assertThat(result.getTitle()).isEqualTo("My Session");
    }

    @Test
    @DisplayName("getSession() should throw 404 exception when not found")
    void getSession_throwsNotFoundWhenSessionDoesNotExist() {
        wireMock.stubFor(get(urlEqualTo("/session/nonexistent"))
                .willReturn(aResponse().withStatus(404).withBody("Not found")));

        assertThatThrownBy(() -> client.getSession("nonexistent"))
                .isInstanceOf(OpenCodeClientHttpException.class)
                .satisfies(e -> {
                    OpenCodeClientHttpException ex = (OpenCodeClientHttpException) e;
                    assertThat(ex.isNotFound()).isTrue();
                    assertThat(ex.getStatusCode()).isEqualTo(404);
                });
    }

    @Test
    @DisplayName("updateSession() should PATCH /session/{id}")
    void updateSession_patchesSession() throws Exception {
        UpdateSessionRequest request = UpdateSessionRequest.builder().title("Updated Title").build();
        Session updated = Session.builder().id("sess-1").title("Updated Title").build();

        wireMock.stubFor(patch(urlEqualTo("/session/sess-1"))
                .withRequestBody(matchingJsonPath("$.title", equalTo("Updated Title")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(updated))));

        Session result = client.updateSession("sess-1", request);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("deleteSession() should DELETE /session/{id} and return true")
    void deleteSession_deletesAndReturnsTrue() {
        wireMock.stubFor(delete(urlEqualTo("/session/sess-del"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.deleteSession("sess-del");

        assertThat(result).isTrue();
        wireMock.verify(deleteRequestedFor(urlEqualTo("/session/sess-del")));
    }

    @Test
    @DisplayName("abortSession() should POST /session/{id}/abort")
    void abortSession_postsToAbort() {
        wireMock.stubFor(post(urlEqualTo("/session/sess-abc/abort"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.abortSession("sess-abc");

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-abc/abort")));
    }

    @Test
    @DisplayName("shareSession() should POST /session/{id}/share")
    void shareSession_postsToShare() throws Exception {
        Session shared = Session.builder().id("sess-s").share("https://opencode.ai/s/abc123").build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-s/share"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(shared))));

        Session result = client.shareSession("sess-s");

        assertThat(result.getShare()).isEqualTo("https://opencode.ai/s/abc123");
    }

    @Test
    @DisplayName("unshareSession() should DELETE /session/{id}/share")
    void unshareSession_deletesShare() throws Exception {
        Session unshared = Session.builder().id("sess-s").build();

        wireMock.stubFor(delete(urlEqualTo("/session/sess-s/share"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(unshared))));

        Session result = client.unshareSession("sess-s");

        assertThat(result.getId()).isEqualTo("sess-s");
        wireMock.verify(deleteRequestedFor(urlEqualTo("/session/sess-s/share")));
    }

    @Test
    @DisplayName("getSessionStatuses() should GET /session/status and return map")
    void getSessionStatuses_returnsMap() throws Exception {
        SessionStatus status = SessionStatus.builder().sessionId("sess-1").status("idle").build();
        Map<String, SessionStatus> body = Map.of("sess-1", status);

        wireMock.stubFor(get(urlEqualTo("/session/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, SessionStatus> result = client.getSessionStatuses();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("sess-1");
        assertThat(result.get("sess-1").getStatus()).isEqualTo("idle");
        wireMock.verify(getRequestedFor(urlEqualTo("/session/status")));
    }

    @Test
    @DisplayName("getSessionTodos() should GET /session/{id}/todo")
    void getSessionTodos_returnsTodos() throws Exception {
        List<Todo> todos = List.of(
                Todo.builder().id("t1").content("Fix bug").status("pending").priority("high").build()
        );

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/todo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(todos))));

        List<Todo> result = client.getSessionTodos("sess-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Fix bug");
        assertThat(result.get(0).getStatus()).isEqualTo("pending");
        wireMock.verify(getRequestedFor(urlEqualTo("/session/sess-1/todo")));
    }

    @Test
    @DisplayName("getSessionDiff() should GET /session/{id}/diff")
    void getSessionDiff_returnsDiffs() throws Exception {
        List<FileDiff> diffs = List.of(
                FileDiff.builder().file("src/Main.java").added(10).removed(2).patch("@@ -1,2 +1,10 @@").build()
        );

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/diff"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(diffs))));

        List<FileDiff> result = client.getSessionDiff("sess-1", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFile()).isEqualTo("src/Main.java");
        assertThat(result.get(0).getAdded()).isEqualTo(10);
        wireMock.verify(getRequestedFor(urlEqualTo("/session/sess-1/diff")));
    }

    @Test
    @DisplayName("getSessionDiff() with messageId should append ?messageID query param")
    void getSessionDiff_withMessageId_appendsQueryParam() throws Exception {
        List<FileDiff> diffs = List.of(
                FileDiff.builder().file("src/Foo.java").added(5).removed(1).build()
        );

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/diff?messageID=msg-42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(diffs))));

        List<FileDiff> result = client.getSessionDiff("sess-1", "msg-42");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFile()).isEqualTo("src/Foo.java");
        wireMock.verify(getRequestedFor(urlEqualTo("/session/sess-1/diff?messageID=msg-42")));
    }

    @Test
    @DisplayName("revertSession() should POST /session/{id}/revert with messageID")
    void revertSession_postsRevertRequest() throws Exception {
        RevertRequest request = RevertRequest.builder().messageId("msg-99").build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/revert"))
                .withRequestBody(matchingJsonPath("$.messageID", equalTo("msg-99")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.revertSession("sess-1", request);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/revert")));
    }

    @Test
    @DisplayName("unrevertSession() should POST /session/{id}/unrevert with no body")
    void unrevertSession_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/session/sess-1/unrevert"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.unrevertSession("sess-1");

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/unrevert")));
    }

    @Test
    @DisplayName("respondToPermission() should POST /session/{id}/permissions/{permissionID}")
    void respondToPermission_postsPermissionResponse() throws Exception {
        PermissionRequest request = PermissionRequest.builder().response("allow").remember(true).build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/permissions/perm-42"))
                .withRequestBody(matchingJsonPath("$.response", equalTo("allow")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.respondToPermission("sess-1", "perm-42", request);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/permissions/perm-42")));
    }

    @Test
    @DisplayName("forkSession(id) should POST /session/{id}/fork with no body")
    void forkSession_noBody_createsChild() throws Exception {
        Session child = Session.builder().id("child-sess").build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/fork"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(child))));

        Session result = client.forkSession("sess-1");

        assertThat(result.getId()).isEqualTo("child-sess");
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/fork")));
    }

    @Test
    @DisplayName("summarizeSession() should POST /session/{id}/summarize")
    void summarizeSession_postsWithProviderAndModel() {
        wireMock.stubFor(post(urlEqualTo("/session/sess-1/summarize"))
                .withRequestBody(matchingJsonPath("$.providerID", equalTo("openai")))
                .withRequestBody(matchingJsonPath("$.modelID", equalTo("gpt-4o")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.summarizeSession("sess-1", "openai", "gpt-4o");

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/summarize")));
    }

    @Test
    @DisplayName("getSessionChildren() should GET /session/{id}/children")
    void getSessionChildren_returnsChildren() throws Exception {
        List<Session> children = List.of(Session.builder().id("child-1").build());

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/children"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(children))));

        List<Session> result = client.getSessionChildren("sess-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("child-1");
        wireMock.verify(getRequestedFor(urlEqualTo("/session/sess-1/children")));
    }

    // ========================================================================
    // Messages
    // ========================================================================

    @Test
    @DisplayName("listMessages() should GET /session/{id}/message")
    void listMessages_returnsMessages() throws Exception {
        Message msg = Message.builder().id("msg-1").sessionId("sess-1").role("user").build();
        Part part = Part.builder().type("text").text("Hello OpenCode!").build();
        MessageWithParts mwp = MessageWithParts.builder()
                .info(msg)
                .parts(List.of(part))
                .build();

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/message"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(mwp)))));

        List<MessageWithParts> result = client.listMessages("sess-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInfo().getRole()).isEqualTo("user");
        assertThat(result.get(0).getParts()).hasSize(1);
        assertThat(result.get(0).getParts().get(0).getText()).isEqualTo("Hello OpenCode!");
    }

    @Test
    @DisplayName("sendMessage() should POST /session/{id}/message with parts")
    void sendMessage_postsMessageAndReturnsResponse() throws Exception {
        Part userPart = Part.builder().type("text").text("Explain Spring Boot").build();
        SendMessageRequest request = SendMessageRequest.builder()
                .parts(List.of(userPart))
                .build();

        Message assistantMsg = Message.builder()
                .id("msg-resp")
                .sessionId("sess-1")
                .role("assistant")
                .build();
        Part responsePart = Part.builder()
                .type("text")
                .text("Spring Boot is a framework...")
                .build();
        MessageWithParts response = MessageWithParts.builder()
                .info(assistantMsg)
                .parts(List.of(responsePart))
                .build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/message"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));

        MessageWithParts result = client.sendMessage("sess-1", request);

        assertThat(result.getInfo().getRole()).isEqualTo("assistant");
        assertThat(result.getParts().get(0).getText()).contains("Spring Boot");
    }

    @Test
    @DisplayName("getMessage() should GET /session/{id}/message/{messageID}")
    void getMessage_returnsMessage() throws Exception {
        Message msg = Message.builder().id("msg-42").role("user").build();
        MessageWithParts mwp = MessageWithParts.builder()
                .info(msg)
                .parts(List.of())
                .build();

        wireMock.stubFor(get(urlEqualTo("/session/sess-1/message/msg-42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mwp))));

        MessageWithParts result = client.getMessage("sess-1", "msg-42");

        assertThat(result.getInfo().getId()).isEqualTo("msg-42");
    }

    @Test
    @DisplayName("executeCommand() should POST /session/{id}/command")
    void executeCommand_postsCommand() throws Exception {
        ExecuteCommandRequest cmd = ExecuteCommandRequest.builder()
                .command("init")
                .agent("default")
                .build();

        Message msg = Message.builder().id("cmd-resp").role("assistant").build();
        MessageWithParts response = MessageWithParts.builder()
                .info(msg)
                .parts(List.of())
                .build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/command"))
                .withRequestBody(matchingJsonPath("$.command", equalTo("init")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));

        MessageWithParts result = client.executeCommand("sess-1", cmd);

        assertThat(result.getInfo().getId()).isEqualTo("cmd-resp");
    }

    @Test
    @DisplayName("shellCommand() should POST /session/{id}/shell")
    void shellCommand_postsShellRequest() throws Exception {
        ShellRequest request = ShellRequest.builder().command("ls -la").build();

        Message msg = Message.builder().id("shell-resp").role("tool").build();
        MessageWithParts response = MessageWithParts.builder()
                .info(msg)
                .parts(List.of())
                .build();

        wireMock.stubFor(post(urlEqualTo("/session/sess-1/shell"))
                .withRequestBody(matchingJsonPath("$.command", equalTo("ls -la")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));

        MessageWithParts result = client.shellCommand("sess-1", request);

        assertThat(result.getInfo().getId()).isEqualTo("shell-resp");
        wireMock.verify(postRequestedFor(urlEqualTo("/session/sess-1/shell")));
    }

    // ========================================================================
    // Agents
    // ========================================================================

    @Test
    @DisplayName("listAgents() should GET /agent")
    void listAgents_returnsAgents() throws Exception {
        List<Agent> agents = List.of(
                Agent.builder().name("default").description("Default agent").build(),
                Agent.builder().name("custom").description("Custom agent").build()
        );

        wireMock.stubFor(get(urlEqualTo("/agent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(agents))));

        List<Agent> result = client.listAgents();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("default");
    }

    // ========================================================================
    // Commands
    // ========================================================================

    @Test
    @DisplayName("listCommands() should GET /command")
    void listCommands_returnsCommands() throws Exception {
        List<Command> commands = List.of(
                Command.builder().name("init").description("Initialize project").build(),
                Command.builder().name("undo").description("Undo last change").build()
        );

        wireMock.stubFor(get(urlEqualTo("/command"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(commands))));

        List<Command> result = client.listCommands();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("init");
    }

    // ========================================================================
    // Files
    // ========================================================================

    @Test
    @DisplayName("findInFiles() should GET /find?pattern=...")
    void findInFiles_returnsMatches() throws Exception {
        List<Map<String, Object>> matches = List.of(
                Map.of("file", "src/Main.java", "line", 42, "match", "public static void main")
        );

        wireMock.stubFor(get(urlPathEqualTo("/find"))
                .withQueryParam("pattern", equalTo("main"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(matches))));

        List<Map<String, Object>> result = client.findInFiles("main");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("file");
    }

    @Test
    @DisplayName("findFiles() should GET /find/file?query=...")
    void findFiles_returnsPaths() throws Exception {
        List<String> paths = List.of("src/Main.java", "src/util/Helper.java");

        wireMock.stubFor(get(urlPathEqualTo("/find/file"))
                .withQueryParam("query", equalTo("Main"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(paths))));

        List<String> result = client.findFiles("Main");

        assertThat(result).containsExactly("src/Main.java", "src/util/Helper.java");
    }

    @Test
    @DisplayName("findSymbols() should GET /find/symbol?query=...")
    void findSymbols_returnsSymbols() throws Exception {
        List<Map<String, Object>> symbols = List.of(
                Map.of("name", "mainMethod", "kind", "function", "file", "src/Main.java")
        );

        wireMock.stubFor(get(urlPathEqualTo("/find/symbol"))
                .withQueryParam("query", equalTo("main"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(symbols))));

        List<Map<String, Object>> result = client.findSymbols("main");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("mainMethod");
    }

    @Test
    @DisplayName("listFiles() should GET /file with optional path param")
    void listFiles_returnsFileNodes() throws Exception {
        List<FileNode> nodes = List.of(
                FileNode.builder().path("src").name("src").type("directory").build(),
                FileNode.builder().path("pom.xml").name("pom.xml").type("file").size(2048L).build()
        );

        wireMock.stubFor(get(urlEqualTo("/file"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(nodes))));

        List<FileNode> result = client.listFiles(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("directory");
        assertThat(result.get(1).getSize()).isEqualTo(2048L);
        wireMock.verify(getRequestedFor(urlEqualTo("/file")));
    }

    @Test
    @DisplayName("listFiles() with path should GET /file?path=...")
    void listFiles_withPath_appendsQueryParam() throws Exception {
        List<FileNode> nodes = List.of(
                FileNode.builder().path("src/Main.java").name("Main.java").type("file").build()
        );

        wireMock.stubFor(get(urlPathEqualTo("/file"))
                .withQueryParam("path", equalTo("src"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(nodes))));

        List<FileNode> result = client.listFiles("src");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Main.java");
    }

    @Test
    @DisplayName("getFileContent() should GET /file/content?path=...")
    void getFileContent_returnsContent() throws Exception {
        FileContent expected = FileContent.builder()
                .path("src/Main.java")
                .content("public class Main {}")
                .encoding("utf-8")
                .build();

        wireMock.stubFor(get(urlPathEqualTo("/file/content"))
                .withQueryParam("path", equalTo("src/Main.java"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expected))));

        FileContent result = client.getFileContent("src/Main.java");

        assertThat(result.getPath()).isEqualTo("src/Main.java");
        assertThat(result.getContent()).isEqualTo("public class Main {}");
        assertThat(result.getEncoding()).isEqualTo("utf-8");
    }

    @Test
    @DisplayName("getFileStatus() should GET /file/status")
    void getFileStatus_returnsStatusList() throws Exception {
        List<Map<String, Object>> statuses = List.of(
                Map.of("file", "src/Main.java", "status", "modified")
        );

        wireMock.stubFor(get(urlEqualTo("/file/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(statuses))));

        List<Map<String, Object>> result = client.getFileStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("status")).isEqualTo("modified");
        wireMock.verify(getRequestedFor(urlEqualTo("/file/status")));
    }

    // ========================================================================
    // LSP / Formatter / MCP
    // ========================================================================

    @Test
    @DisplayName("getLspStatus() should GET /lsp and return list")
    void getLspStatus_returnsLspStatusList() throws Exception {
        List<LspStatus> statuses = List.of(
                LspStatus.builder().name("jdtls").status("running").build()
        );

        wireMock.stubFor(get(urlEqualTo("/lsp"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(statuses))));

        List<LspStatus> result = client.getLspStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("jdtls");
        assertThat(result.get(0).getStatus()).isEqualTo("running");
        wireMock.verify(getRequestedFor(urlEqualTo("/lsp")));
    }

    @Test
    @DisplayName("getFormatterStatus() should GET /formatter and return list")
    void getFormatterStatus_returnsFormatterStatusList() throws Exception {
        List<FormatterStatus> statuses = List.of(
                FormatterStatus.builder().name("prettier").status("available").build()
        );

        wireMock.stubFor(get(urlEqualTo("/formatter"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(statuses))));

        List<FormatterStatus> result = client.getFormatterStatus();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("prettier");
        wireMock.verify(getRequestedFor(urlEqualTo("/formatter")));
    }

    @Test
    @DisplayName("getMcpStatus() should GET /mcp and return map")
    void getMcpStatus_returnsMap() throws Exception {
        Map<String, Object> body = Map.of("filesystem", Map.of("status", "connected", "tools", 12));

        wireMock.stubFor(get(urlEqualTo("/mcp"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.getMcpStatus();

        assertThat(result).containsKey("filesystem");
        wireMock.verify(getRequestedFor(urlEqualTo("/mcp")));
    }

    @Test
    @DisplayName("addMcpServer() should POST /mcp and return result map")
    void addMcpServer_postsAndReturnsMap() throws Exception {
        McpAddRequest request = McpAddRequest.builder()
                .name("my-mcp")
                .config(Map.of("command", "npx", "args", List.of("-y", "@modelcontextprotocol/server-filesystem")))
                .build();
        Map<String, Object> responseBody = Map.of("name", "my-mcp", "status", "connected");

        wireMock.stubFor(post(urlEqualTo("/mcp"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("my-mcp")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(responseBody))));

        Map<String, Object> result = client.addMcpServer(request);

        assertThat(result).containsEntry("name", "my-mcp");
        wireMock.verify(postRequestedFor(urlEqualTo("/mcp")));
    }

    // ========================================================================
    // TUI
    // ========================================================================

    @Test
    @DisplayName("tuiAppendPrompt() should POST /tui/append-prompt")
    void tuiAppendPrompt_postsText() throws Exception {
        TuiAppendPromptRequest request = TuiAppendPromptRequest.builder().text("Hello TUI!").build();

        wireMock.stubFor(post(urlEqualTo("/tui/append-prompt"))
                .withRequestBody(matchingJsonPath("$.text", equalTo("Hello TUI!")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiAppendPrompt(request);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/append-prompt")));
    }

    @Test
    @DisplayName("tuiOpenHelp() should POST /tui/open-help with no body")
    void tuiOpenHelp_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/open-help"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiOpenHelp();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/open-help")));
    }

    @Test
    @DisplayName("tuiOpenSessions() should POST /tui/open-sessions with no body")
    void tuiOpenSessions_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/open-sessions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiOpenSessions();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/open-sessions")));
    }

    @Test
    @DisplayName("tuiOpenThemes() should POST /tui/open-themes with no body")
    void tuiOpenThemes_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/open-themes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiOpenThemes();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/open-themes")));
    }

    @Test
    @DisplayName("tuiOpenModels() should POST /tui/open-models with no body")
    void tuiOpenModels_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/open-models"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiOpenModels();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/open-models")));
    }

    @Test
    @DisplayName("tuiSubmitPrompt() should POST /tui/submit-prompt with no body")
    void tuiSubmitPrompt_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/submit-prompt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiSubmitPrompt();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/submit-prompt")));
    }

    @Test
    @DisplayName("tuiClearPrompt() should POST /tui/clear-prompt with no body")
    void tuiClearPrompt_postsWithNoBody() {
        wireMock.stubFor(post(urlEqualTo("/tui/clear-prompt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiClearPrompt();

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/clear-prompt")));
    }

    @Test
    @DisplayName("tuiExecuteCommand() should POST /tui/execute-command")
    void tuiExecuteCommand_postsCommand() throws Exception {
        TuiExecuteCommandRequest request = TuiExecuteCommandRequest.builder().command("toggle-sidebar").build();

        wireMock.stubFor(post(urlEqualTo("/tui/execute-command"))
                .withRequestBody(matchingJsonPath("$.command", equalTo("toggle-sidebar")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiExecuteCommand(request);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/execute-command")));
    }

    @Test
    @DisplayName("tuiShowToast() should POST /tui/show-toast")
    void tuiShowToast_postsToastRequest() throws Exception {
        TuiShowToastRequest request = TuiShowToastRequest.builder()
                .title("Success")
                .message("Operation completed")
                .variant("success")
                .build();

        wireMock.stubFor(post(urlEqualTo("/tui/show-toast"))
                .withRequestBody(matchingJsonPath("$.message", equalTo("Operation completed")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiShowToast(request);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/show-toast")));
    }

    @Test
    @DisplayName("tuiControlResponse() should POST /tui/control/response")
    void tuiControlResponse_postsResponse() throws Exception {
        TuiControlResponse response = TuiControlResponse.builder().body("user-input").build();

        wireMock.stubFor(post(urlEqualTo("/tui/control/response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.tuiControlResponse(response);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/tui/control/response")));
    }

    @Test
    @DisplayName("tuiControlNext() should GET /tui/control/next")
    void tuiControlNext_returnsControlRequest() throws Exception {
        Map<String, Object> body = Map.of("type", "input", "prompt", "Enter your name:");

        wireMock.stubFor(get(urlEqualTo("/tui/control/next"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));

        Map<String, Object> result = client.tuiControlNext();

        assertThat(result).containsEntry("type", "input");
        wireMock.verify(getRequestedFor(urlEqualTo("/tui/control/next")));
    }

    // ========================================================================
    // Auth
    // ========================================================================

    @Test
    @DisplayName("setAuth() should PUT /auth/{id} with credentials")
    void setAuth_putsCredentials() throws Exception {
        Map<String, Object> credentials = Map.of("apiKey", "sk-test-1234567890");

        wireMock.stubFor(put(urlEqualTo("/auth/openai"))
                .withRequestBody(matchingJsonPath("$.apiKey", equalTo("sk-test-1234567890")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.setAuth("openai", credentials);

        assertThat(result).isTrue();
        wireMock.verify(putRequestedFor(urlEqualTo("/auth/openai")));
    }

    // ========================================================================
    // Error handling
    // ========================================================================

    @Test
    @DisplayName("HTTP 401 should produce unauthorized exception")
    void unauthorizedRequest_throwsUnauthorizedException() {
        wireMock.stubFor(get(urlEqualTo("/session"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

        assertThatThrownBy(() -> client.listSessions())
                .isInstanceOf(OpenCodeClientHttpException.class)
                .satisfies(e -> {
                    OpenCodeClientHttpException ex = (OpenCodeClientHttpException) e;
                    assertThat(ex.isUnauthorized()).isTrue();
                });
    }

    @Test
    @DisplayName("HTTP 400 should produce bad request exception")
    void badRequest_throwsBadRequestException() {
        wireMock.stubFor(post(urlEqualTo("/session"))
                .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

        assertThatThrownBy(() -> client.createSession(CreateSessionRequest.builder().build()))
                .isInstanceOf(OpenCodeClientHttpException.class)
                .satisfies(e -> {
                    OpenCodeClientHttpException ex = (OpenCodeClientHttpException) e;
                    assertThat(ex.isBadRequest()).isTrue();
                });
    }

    @Test
    @DisplayName("HTTP 500 on PATCH should throw exception with correct status code")
    void serverError_onPatch_throwsException() {
        wireMock.stubFor(patch(urlEqualTo("/config"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThatThrownBy(() -> client.updateConfig(AppConfig.builder().theme("dark").build()))
                .isInstanceOf(OpenCodeClientHttpException.class)
                .extracting(e -> ((OpenCodeClientHttpException) e).getStatusCode())
                .isEqualTo(500);
    }

    @Test
    @DisplayName("HTTP 404 on DELETE should throw not-found exception")
    void notFound_onDelete_throwsException() {
        wireMock.stubFor(delete(urlEqualTo("/session/missing"))
                .willReturn(aResponse().withStatus(404).withBody("Not found")));

        assertThatThrownBy(() -> client.deleteSession("missing"))
                .isInstanceOf(OpenCodeClientHttpException.class)
                .satisfies(e -> {
                    OpenCodeClientHttpException ex = (OpenCodeClientHttpException) e;
                    assertThat(ex.isNotFound()).isTrue();
                });
    }

    // ========================================================================
    // Log
    // ========================================================================

    @Test
    @DisplayName("log() should POST /log with service/level/message")
    void log_postsLogEntry() throws Exception {
        LogRequest logRequest = LogRequest.builder()
                .service("my-service")
                .level("INFO")
                .message("Test log message")
                .build();

        wireMock.stubFor(post(urlEqualTo("/log"))
                .withRequestBody(matchingJsonPath("$.service", equalTo("my-service")))
                .withRequestBody(matchingJsonPath("$.level", equalTo("INFO")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = client.log(logRequest);

        assertThat(result).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/log")));
    }

    // ========================================================================
    // Project
    // ========================================================================

    @Test
    @DisplayName("listProjects() should GET /project")
    void listProjects_returnsProjects() throws Exception {
        List<Project> projects = List.of(
                Project.builder().path("/workspaces/project1").git(true).build()
        );

        wireMock.stubFor(get(urlEqualTo("/project"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(projects))));

        List<Project> result = client.listProjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPath()).isEqualTo("/workspaces/project1");
        assertThat(result.get(0).getGit()).isTrue();
    }

    @Test
    @DisplayName("getCurrentProject() should GET /project/current")
    void getCurrentProject_returnsCurrentProject() throws Exception {
        Project project = Project.builder().path("/workspaces/current").git(false).build();

        wireMock.stubFor(get(urlEqualTo("/project/current"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(project))));

        Project result = client.getCurrentProject();

        assertThat(result.getPath()).isEqualTo("/workspaces/current");
    }
}
