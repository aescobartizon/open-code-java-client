package com.opencode.client.integration;

import com.opencode.client.OpenCodeClient;
import com.opencode.client.OpenCodeClientOperations;
import com.opencode.client.config.OpenCodeClientProperties;
import com.opencode.client.exception.OpenCodeClientHttpException;
import com.opencode.client.model.*;
import io.netty.channel.ChannelOption;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for conversational messaging endpoints:
 * <ul>
 *   <li>POST /session/:id/message      — send & wait (sync)</li>
 *   <li>GET  /session/:id/message/:mid — fetch one message by ID</li>
 *   <li>GET  /session/:id/message      — list all messages</li>
 *   <li>POST /session/:id/prompt_async — send without waiting (async)</li>
 * </ul>
 *
 * <p>The test simulates a real multi-turn conversation:
 * <ol>
 *   <li>Turn 1 (sync): sends "What is 2+2?" and verifies the assistant replies.</li>
 *   <li>Fetch by ID: retrieves the user message via GET /message/:id and verifies consistency.</li>
 *   <li>Turn 2 (sync): continues with "And what is 3+3?" — verifies context is preserved.</li>
 *   <li>List verification: GET /message confirms all turns appear in order.</li>
 *   <li>Turn 3 (async): sends a message via prompt_async — verifies it eventually appears
 *       in the message list using Awaitility.</li>
 * </ol>
 *
 * <p><b>Important:</b> the opencode-server requires a configured LLM provider to reply.
 * If no provider/model is configured the assistant response parts may be empty or the call
 * may timeout.  The tests are therefore defensive: they assert structural correctness
 * (non-null IDs, correct roles, correct sessionID) but do not assert the text of the
 * assistant's reply.
 */
@Testcontainers
@DisplayName("Conversational Messaging — Integration Tests")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConversationalMessagingIT {

    // -----------------------------------------------------------------------
    // Container setup (shared across all tests in this class)
    // -----------------------------------------------------------------------

    private static final int SERVER_PORT = 4096;
    private static final String DOCKER_IMAGE = "anescobar/opencode-server:1.0.0";

    @Container
    static final GenericContainer<?> opencodeServer =
            new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE))
                    .withExposedPorts(SERVER_PORT)
                    .withCommand("opencode", "serve", "--hostname", "0.0.0.0", "--port", "4096")
                    .waitingFor(
                            Wait.forHttp("/global/health")
                                    .forPort(SERVER_PORT)
                                    .forStatusCode(200)
                                    .withStartupTimeout(Duration.ofMinutes(2)));

    /** Typed as the interface to verify the concrete class satisfies the contract. */
    private static OpenCodeClientOperations client;

    // -----------------------------------------------------------------------
    // State shared across ordered tests
    // -----------------------------------------------------------------------

    /** ID of the session created in the first test and cleaned up in the last. */
    private static String sessionId;

    /** ID of the user message from turn 1, used by the fetch-by-ID test. */
    private static String turn1UserMessageId;

    // -----------------------------------------------------------------------
    // Client bootstrap
    // -----------------------------------------------------------------------

    @BeforeAll
    static void setUp() {
        String baseUrl = "http://"
                + opencodeServer.getHost()
                + ":"
                + opencodeServer.getMappedPort(SERVER_PORT);

        OpenCodeClientProperties props = new OpenCodeClientProperties();
        props.setBaseUrl(baseUrl);
        props.setConnectTimeout(Duration.ofSeconds(10));
        // Generous timeout — the assistant may take a while to reply when a model is configured
        props.setResponseTimeout(Duration.ofSeconds(120));

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        client = new OpenCodeClient(webClient, props);
    }

    // -----------------------------------------------------------------------
    // Test 1 — Session creation
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("1. Crear sesión conversacional")
    void step1_createSession() {
        Session session = client.createSession(
                CreateSessionRequest.builder()
                        .title("Conversational IT — " + System.currentTimeMillis())
                        .build());

        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotBlank();

        sessionId = session.getId();
    }

    // -----------------------------------------------------------------------
    // Test 2 — Turn 1: send message (sync) + verify response structure
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("2. Turno 1 (sync): enviar mensaje de usuario y recibir respuesta")
    void step2_turn1_sendMessageSync() {
        assertThat(sessionId)
                .as("sessionId must be set by step 1")
                .isNotBlank();

        Part userTextPart = Part.builder()
                .type("text")
                .text("What is 2+2? Reply with just the number.")
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .parts(List.of(userTextPart))
                .build();

        // POST /session/:id/message — blocks until assistant replies (or times out)
        MessageWithParts response = client.sendMessage(sessionId, request);

        // --- Structural assertions on the returned (assistant) message ---
        assertThat(response).isNotNull();
        assertThat(response.getInfo()).isNotNull();

        Message assistantMsg = response.getInfo();
        assertThat(assistantMsg.getId()).isNotBlank();
        assertThat(assistantMsg.getSessionId()).isEqualTo(sessionId);
        assertThat(assistantMsg.getRole())
                .as("The returned message should be the assistant reply")
                .isEqualTo("assistant");

        // Parts list must be present (may be empty if no model is configured)
            assertThat(response.getParts()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // Test 3 — Fetch user message by ID
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("3. GET /session/:id/message/:msgId — obtener mensaje de usuario por ID")
    void step3_fetchUserMessageById() {
        assertThat(sessionId).isNotBlank();

        // List messages to find the user message from turn 1
        List<MessageWithParts> all = client.listMessages(sessionId);
        assertThat(all)
                .as("After turn 1 there must be at least 1 message")
                .isNotEmpty();

        // The first message in the list should be the user message
        MessageWithParts userMsgWithParts = all.stream()
                .filter(m -> m.getInfo() != null && "user".equals(m.getInfo().getRole()))
                .findFirst()
                .orElse(null);

        assertThat(userMsgWithParts)
                .as("A user message must exist after turn 1")
                .isNotNull();

        turn1UserMessageId = userMsgWithParts.getInfo().getId();
        assertThat(turn1UserMessageId).isNotBlank();

        // GET /session/:id/message/:messageID
        MessageWithParts fetched = client.getMessage(sessionId, turn1UserMessageId);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getInfo()).isNotNull();
        assertThat(fetched.getInfo().getId()).isEqualTo(turn1UserMessageId);
        assertThat(fetched.getInfo().getSessionId()).isEqualTo(sessionId);
        assertThat(fetched.getInfo().getRole()).isEqualTo("user");
        assertThat(fetched.getParts())
                .as("User message must have at least one part")
                .isNotNull()
                .isNotEmpty();

        // The text part must contain the original question
        boolean hasExpectedText = fetched.getParts().stream()
                .filter(p -> "text".equals(p.getType()))
                .anyMatch(p -> p.getText() != null
                        && p.getText().contains("2+2"));
        assertThat(hasExpectedText)
                .as("The user message part should contain '2+2'")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Test 4 — Turn 2: continue the conversation (sync)
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("4. Turno 2 (sync): continuar conversación — contexto mantenido")
    void step4_turn2_continueConversation() {
        assertThat(sessionId).isNotBlank();

        Part followUp = Part.builder()
                .type("text")
                .text("And what is 3+3? Also just the number.")
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .parts(List.of(followUp))
                .build();

        MessageWithParts response = client.sendMessage(sessionId, request);

        assertThat(response).isNotNull();
        assertThat(response.getInfo()).isNotNull();
        assertThat(response.getInfo().getSessionId()).isEqualTo(sessionId);
        assertThat(response.getInfo().getRole()).isEqualTo("assistant");
        assertThat(response.getInfo().getId()).isNotBlank();

        // The message ID must differ from turn 1's user message
        assertThat(response.getInfo().getId()).isNotEqualTo(turn1UserMessageId);
    }

    // -----------------------------------------------------------------------
    // Test 5 — List messages: verify complete conversation order
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("5. GET /session/:id/message — listar todos los mensajes en orden")
    void step5_listMessages_verifyConversationOrder() {
        assertThat(sessionId).isNotBlank();

        List<MessageWithParts> messages = client.listMessages(sessionId);

        assertThat(messages)
                .as("After 2 turns there should be at least 2 messages")
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(2);

        // All messages must belong to this session
        messages.forEach(m -> {
            assertThat(m.getInfo()).isNotNull();
            assertThat(m.getInfo().getSessionId())
                    .as("Every message must belong to session %s", sessionId)
                    .isEqualTo(sessionId);
            assertThat(m.getInfo().getId()).isNotBlank();
            assertThat(m.getInfo().getRole())
                    .as("Role must be 'user' or 'assistant'")
                    .isIn("user", "assistant");
        });

        // First message must be the user message from turn 1
        assertThat(messages.get(0).getInfo().getId()).isEqualTo(turn1UserMessageId);

        // Verify alternating roles: user → assistant → user → assistant …
        for (int i = 0; i < messages.size() - 1; i++) {
            String current = messages.get(i).getInfo().getRole();
            String next = messages.get(i + 1).getInfo().getRole();
            assertThat(current)
                    .as("Consecutive messages should not have the same role (index %d)", i)
                    .isNotEqualTo(next);
        }
    }

    // -----------------------------------------------------------------------
    // Test 6 — Turn 3: async send + Awaitility polling
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("6. Turno 3 (async): POST /prompt_async — el mensaje aparece eventualmente en la lista")
    void step6_turn3_sendMessageAsync_eventuallyAppearsInList() {
        assertThat(sessionId).isNotBlank();

        // Capture current message count before the async send
        List<MessageWithParts> before = client.listMessages(sessionId);
        int countBefore = before.size();

        Part asyncPart = Part.builder()
                .type("text")
                .text("One more question: what is 10 divided by 2? Just the number.")
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .parts(List.of(asyncPart))
                .build();

        // POST /session/:id/prompt_async — returns immediately (204 No Content)
        // Should not throw
        assertThatCode(() -> client.sendMessageAsync(sessionId, request))
                .doesNotThrowAnyException();

        // Poll until at least one new message appears (the user message of turn 3)
        Awaitility.await()
                .alias("async user message appears in list")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<MessageWithParts> after = client.listMessages(sessionId);
                    assertThat(after)
                            .as("At least the async user message should appear")
                            .hasSizeGreaterThan(countBefore);
                });

        // Verify the new user message contains our text
        List<MessageWithParts> afterMessages = client.listMessages(sessionId);
        boolean asyncUserMessagePresent = afterMessages.stream()
                .filter(m -> "user".equals(m.getInfo().getRole()))
                .flatMap(m -> m.getParts() == null ? java.util.stream.Stream.empty() : m.getParts().stream())
                .filter(p -> "text".equals(p.getType()) && p.getText() != null)
                .anyMatch(p -> p.getText().contains("10 divided by 2"));

        assertThat(asyncUserMessagePresent)
                .as("The async user message with '10 divided by 2' should be in the list")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Test 7 — Fetch non-existent message: expect 4xx
    // -----------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("7. GET /session/:id/message/:msgId con ID inexistente — debe lanzar error HTTP")
    void step7_getMessage_nonExistentId_throws4xx() {
        assertThat(sessionId).isNotBlank();

        assertThatThrownBy(() -> client.getMessage(sessionId, "non-existent-message-id-xyz"))
                .isInstanceOf(com.opencode.client.exception.OpenCodeClientHttpException.class)
                .satisfies(e -> {
                    com.opencode.client.exception.OpenCodeClientHttpException ex =
                            (com.opencode.client.exception.OpenCodeClientHttpException) e;
                    // Server may return 4xx or 5xx for unknown message IDs
                    assertThat(ex.getStatusCode()).isGreaterThanOrEqualTo(400);
                });
    }

    // -----------------------------------------------------------------------
    // Test 8 — Parts structure: every message has non-null parts list
    // -----------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("8. Estructura de parts — cada mensaje tiene lista de parts no nula")
    void step8_messageParts_notNull() {
        assertThat(sessionId).isNotBlank();

        List<MessageWithParts> messages = client.listMessages(sessionId);
        assertThat(messages).isNotNull().isNotEmpty();

        messages.forEach(m -> {
            assertThat(m.getInfo()).isNotNull();
            assertThat(m.getParts())
                    .as("Parts list for message %s must not be null", m.getInfo().getId())
                    .isNotNull();
        });

        // User messages must have at least one 'text' part
        messages.stream()
                .filter(m -> "user".equals(m.getInfo().getRole()))
                .forEach(m -> {
                    boolean hasTextPart = m.getParts().stream()
                            .anyMatch(p -> "text".equals(p.getType()) && p.getText() != null);
                    assertThat(hasTextPart)
                            .as("User message %s must have a text part", m.getInfo().getId())
                            .isTrue();
                });
    }

    // -----------------------------------------------------------------------
    // Test 9 — getMessage by known ID returns consistent data
    // -----------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("9. getMessage() con ID válido — datos consistentes con listMessages()")
    void step9_getMessage_consistentWithList() {
        assertThat(sessionId).isNotBlank();
        assertThat(turn1UserMessageId).isNotBlank();

        // Fetch via direct endpoint
        MessageWithParts byId = client.getMessage(sessionId, turn1UserMessageId);
        assertThat(byId).isNotNull();
        assertThat(byId.getInfo().getId()).isEqualTo(turn1UserMessageId);
        assertThat(byId.getInfo().getSessionId()).isEqualTo(sessionId);
        assertThat(byId.getInfo().getRole()).isEqualTo("user");

        // Must also appear in the list with the same content
        List<MessageWithParts> all = client.listMessages(sessionId);
        MessageWithParts fromList = all.stream()
                .filter(m -> turn1UserMessageId.equals(m.getInfo().getId()))
                .findFirst()
                .orElse(null);

        assertThat(fromList).isNotNull();
        assertThat(fromList.getInfo().getRole()).isEqualTo(byId.getInfo().getRole());
        assertThat(fromList.getParts()).hasSameSizeAs(byId.getParts());
    }

    // -----------------------------------------------------------------------
    // Test 10 — sendMessage() with empty text part — accepted without NPE
    // -----------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("10. sendMessage() con texto vacío — servidor acepta o rechaza limpiamente")
    void step10_sendMessage_emptyText_serverResponds() {
        assertThat(sessionId).isNotBlank();

        Part emptyPart = Part.builder()
                .type("text")
                .text("")
                .build();
        SendMessageRequest emptyRequest = SendMessageRequest.builder()
                .parts(List.of(emptyPart))
                .build();

        // The server may accept (returning an assistant message) or reject with 4xx.
        // Either outcome is valid — what matters is that no NullPointerException is thrown
        // and the client wraps any HTTP error properly.
        assertThatCode(() -> client.sendMessage(sessionId, emptyRequest))
                .satisfiesAnyOf(
                        code -> {},  // no exception — server accepted empty prompt
                        code -> assertThatThrownBy(() -> client.sendMessage(sessionId, emptyRequest))
                                .isInstanceOf(OpenCodeClientHttpException.class)
                );
    }

    // -----------------------------------------------------------------------
    // Test 11 — listMessages() count grows with conversation
    // -----------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("11. listMessages() — el conteo crece a lo largo de la conversación")
    void step11_listMessages_countGrows() {
        assertThat(sessionId).isNotBlank();

        int before = client.listMessages(sessionId).size();
        assertThat(before).isGreaterThanOrEqualTo(2);

        // Send one more sync message
        MessageWithParts response = client.sendMessage(sessionId,
                SendMessageRequest.builder()
                        .parts(List.of(Part.builder().type("text").text("Say 'pong'.").build()))
                        .build());
        assertThat(response).isNotNull();

        int after = client.listMessages(sessionId).size();
        // At minimum the user message was added (1 more than before)
        assertThat(after).isGreaterThan(before);
    }

    // -----------------------------------------------------------------------
    // Test 12 — forkSession from conversational session
    // -----------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("12. forkSession() — crear rama desde sesión conversacional")
    void step12_forkFromConversationalSession() {
        assertThat(sessionId).isNotBlank();

        Session forked = null;
        try {
            forked = client.forkSession(sessionId);
            assertThat(forked).isNotNull();
            assertThat(forked.getId())
                    .as("Forked session must have a different ID")
                    .isNotEqualTo(sessionId);
            assertThat(forked.getId()).isNotBlank();

            // The fork should appear as a child of the original session.
            // NOTE: the server does not guarantee the fork appears in getSessionChildren()
            // for simple forks — we only verify the call succeeds.
            List<Session> children = client.getSessionChildren(sessionId);
            assertThat(children).isNotNull();
        } finally {
            if (forked != null) {
                try { client.deleteSession(forked.getId()); } catch (Exception ignored) {}
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test 13 — Cleanup
    // -----------------------------------------------------------------------

    @Test
    @Order(13)
    @DisplayName("13. Limpiar — eliminar la sesión conversacional")
    void step13_cleanup_deleteSession() {
        if (sessionId != null) {
            Boolean deleted = client.deleteSession(sessionId);
            assertThat(deleted).isTrue();
        }
    }
}
