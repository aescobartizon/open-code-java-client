package com.opencode.client.unit;

import com.opencode.client.OpenCodeClientOperations;
import com.opencode.client.chat.ChatClient;
import com.opencode.client.chat.ChatClientFactory;
import com.opencode.client.chat.ChatSession;
import com.opencode.client.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatClient} and {@link ChatClientFactory}.
 *
 * <p>Uses Mockito to mock {@link OpenCodeClientOperations} and a JUnit 5 {@link TempDir}
 * as the sessions root so tests do not touch the real filesystem outside of a temp directory.
 */
@DisplayName("ChatClient / ChatClientFactory — Unit Tests")
class ChatClientFactoryTest {

    // -----------------------------------------------------------------------
    // Shared fixtures
    // -----------------------------------------------------------------------

    @TempDir
    Path tempDir;

    private OpenCodeClientOperations api;
    private ChatClientFactory factory;

    @BeforeEach
    void setUp() {
        api = mock(OpenCodeClientOperations.class);

        // Default: createSession returns a valid server session
        when(api.createSession(any())).thenAnswer(inv -> {
            CreateSessionRequest req = inv.getArgument(0);
            String title = req != null ? req.getTitle() : null;
            return Session.builder()
                    .id("srv-sess-" + System.nanoTime())
                    .title(title)
                    .created(Instant.now().toEpochMilli())
                    .build();
        });

        factory = new ChatClientFactory(api, tempDir);
    }

    // -----------------------------------------------------------------------
    // ChatClientFactory — newClient()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("newClient() — generates a non-blank clientId and creates server session")
    void newClient_generatesClientIdAndCreatesSession() {
        ChatClient client = factory.newClient();
        try {
            assertThat(client.getClientId()).isNotBlank();
            assertThat(client.getSession().getSessionId()).isNotBlank();
            verify(api, times(1)).createSession(any());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("newClient(id) — uses provided clientId")
    void newClient_withExplicitId_usesProvidedId() {
        ChatClient client = factory.newClient("my-client-001");
        try {
            assertThat(client.getClientId()).isEqualTo("my-client-001");
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("newClient() — session.json is persisted to disk")
    void newClient_persistsSessionFileToDisk() throws Exception {
        ChatClient client = factory.newClient("persist-test");
        try {
            Path sessionFile = tempDir.resolve("persist-test")
                    .resolve(ChatClientFactory.SESSION_FILENAME);
            assertThat(sessionFile).exists();

            String json = Files.readString(sessionFile);
            assertThat(json).contains("\"clientId\"").contains("persist-test");
            assertThat(json).contains("\"sessionId\"");
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("newClient(id) — blank id throws IllegalArgumentException")
    void newClient_blankId_throwsIllegalArgument() {
        assertThatThrownBy(() -> factory.newClient("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // ChatClientFactory — getClient()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getClient() — returns same instance on second call (cache hit)")
    void getClient_cacheHit_returnsSameInstance() {
        ChatClient first = factory.getClient("cache-test");
        ChatClient second = factory.getClient("cache-test");
        try {
            assertThat(second).isSameAs(first);
            // createSession called only once despite two getClient calls
            verify(api, times(1)).createSession(any());
        } finally {
            first.close();
        }
    }

    @Test
    @DisplayName("getClient() — loads from disk when not in cache")
    void getClient_loadFromDisk_whenNotInCache() throws Exception {
        // Step 1: create via newClient, which persists to disk
        ChatClient original = factory.newClient("disk-load-test");
        String originalSessionId = original.getSession().getSessionId();
        original.close();

        // Step 2: new factory (empty cache) should load from disk
        ChatClientFactory factory2 = new ChatClientFactory(api, tempDir);
        ChatClient loaded = factory2.getClient("disk-load-test");
        try {
            assertThat(loaded.getClientId()).isEqualTo("disk-load-test");
            assertThat(loaded.getSession().getSessionId()).isEqualTo(originalSessionId);
            // createSession called only once (for the original newClient, not for the reload)
            verify(api, times(1)).createSession(any());
        } finally {
            loaded.close();
        }
    }

    @Test
    @DisplayName("getClient() — creates new session when no file exists for clientId")
    void getClient_noFile_createsNewSession() {
        ChatClient client = factory.getClient("brand-new-id");
        try {
            assertThat(client.getClientId()).isEqualTo("brand-new-id");
            verify(api, times(1)).createSession(any());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("getClient() — blank id throws IllegalArgumentException")
    void getClient_blankId_throwsIllegalArgument() {
        assertThatThrownBy(() -> factory.getClient(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // ChatClientFactory — listClientIds()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("listClientIds() — returns ids of persisted sessions")
    void listClientIds_returnsPersistedIds() {
        ChatClient c1 = factory.newClient("alpha");
        ChatClient c2 = factory.newClient("beta");
        ChatClient c3 = factory.newClient("gamma");
        try {
            String[] ids = factory.listClientIds();
            assertThat(ids).containsExactlyInAnyOrder("alpha", "beta", "gamma");
        } finally {
            c1.close(); c2.close(); c3.close();
        }
    }

    @Test
    @DisplayName("listClientIds() — empty when no sessions have been created")
    void listClientIds_emptyWhenNoSessions() {
        assertThat(factory.listClientIds()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // ChatClient — send()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("send() — delegates to api.sendMessage and returns reply")
    void send_delegatesToApi() {
        MessageWithParts mockReply = MessageWithParts.builder()
                .info(Message.builder().id("msg-1").sessionId("srv-1").role("assistant").build())
                .parts(List.of(Part.builder().type("text").text("Hello!").build()))
                .build();
        when(api.sendMessage(anyString(), any())).thenReturn(mockReply);

        ChatClient client = factory.newClient("send-test");
        try {
            MessageWithParts reply = client.send("Hi there");
            assertThat(reply).isNotNull();
            assertThat(reply.getInfo().getRole()).isEqualTo("assistant");
            verify(api, times(1)).sendMessage(anyString(), any());
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("send() — blank text throws IllegalArgumentException")
    void send_blankText_throwsIllegalArgument() {
        ChatClient client = factory.newClient("send-blank-test");
        try {
            assertThatThrownBy(() -> client.send("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("send() — updates lastActivityAt after successful send")
    void send_updatesLastActivityAt() throws Exception {
        MessageWithParts mockReply = MessageWithParts.builder()
                .info(Message.builder().id("msg-x").sessionId("srv-x").role("assistant").build())
                .parts(List.of())
                .build();
        when(api.sendMessage(anyString(), any())).thenReturn(mockReply);

        ChatClient client = factory.newClient("activity-test");
        Instant before = client.getSession().getLastActivityAt();
        Thread.sleep(5); // ensure time advances
        client.send("ping");

        Instant after = client.getSession().getLastActivityAt();
        assertThat(after).isAfter(before);
        client.close();
    }

    // -----------------------------------------------------------------------
    // ChatClient — sendAsync()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("sendAsync() — calls api.sendMessageAsync and invokes callback when reply arrives")
    void sendAsync_callbackInvokedWhenReplyAppears() throws InterruptedException {
        // Simulate: after async send, the message list grows with an assistant reply
        String sessionId = "async-srv-sess";
        when(api.createSession(any())).thenReturn(
                Session.builder().id(sessionId).title("async-test").build());

        MessageWithParts userMsg = MessageWithParts.builder()
                .info(Message.builder().id("u1").sessionId(sessionId).role("user").build())
                .parts(List.of())
                .build();
        MessageWithParts assistantMsg = MessageWithParts.builder()
                .info(Message.builder().id("a1").sessionId(sessionId).role("assistant").build())
                .parts(List.of(Part.builder().type("text").text("42").build()))
                .build();

        // Before async: empty list; after async: user + assistant messages appear
        when(api.listMessages(sessionId))
                .thenReturn(List.of())           // baseline call
                .thenReturn(List.of(userMsg, assistantMsg)); // poll call

        doNothing().when(api).sendMessageAsync(anyString(), any());

        ChatClient client = factory.newClient("async-test");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<MessageWithParts> captured = new AtomicReference<>();

        client.sendAsync("What is 6×7?", msg -> {
            captured.set(msg);
            latch.countDown();
        });

        boolean fired = latch.await(10, TimeUnit.SECONDS);
        assertThat(fired).as("callback should have been invoked within 10s").isTrue();
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getInfo().getRole()).isEqualTo("assistant");

        client.close();
    }

    @Test
    @DisplayName("sendAsync() — blank text throws IllegalArgumentException before sending")
    void sendAsync_blankText_throwsIllegalArgument() {
        ChatClient client = factory.newClient("async-blank-test");
        try {
            assertThatThrownBy(() -> client.sendAsync(null, msg -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            client.close();
        }
    }

    // -----------------------------------------------------------------------
    // ChatClient — listMessages()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("listMessages() — delegates to api.listMessages")
    void listMessages_delegatesToApi() {
        String sessionId = "list-srv-sess";
        when(api.createSession(any())).thenReturn(
                Session.builder().id(sessionId).title("list-test").build());
        when(api.listMessages(sessionId)).thenReturn(List.of(
                MessageWithParts.builder()
                        .info(Message.builder().id("m1").sessionId(sessionId).role("user").build())
                        .parts(List.of())
                        .build()
        ));

        ChatClient client = factory.newClient("list-test");
        try {
            List<MessageWithParts> msgs = client.listMessages();
            assertThat(msgs).hasSize(1);
            assertThat(msgs.get(0).getInfo().getRole()).isEqualTo("user");
        } finally {
            client.close();
        }
    }

    // -----------------------------------------------------------------------
    // ChatSession — JSON round-trip
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ChatSession — persisted and loaded JSON round-trip preserves all fields")
    void chatSession_jsonRoundTrip() throws Exception {
        ChatClient client = factory.newClient("round-trip-test");
        String originalSessionId = client.getSession().getSessionId();
        Instant originalCreatedAt = client.getSession().getCreatedAt();
        client.close();

        // Reload via a new factory (forces disk read)
        ChatClientFactory factory2 = new ChatClientFactory(api, tempDir);
        ChatClient reloaded = factory2.getClient("round-trip-test");
        try {
            assertThat(reloaded.getSession().getClientId()).isEqualTo("round-trip-test");
            assertThat(reloaded.getSession().getSessionId()).isEqualTo(originalSessionId);
            assertThat(reloaded.getSession().getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(reloaded.getSession().getTitle()).isEqualTo("chat-round-trip-test");
        } finally {
            reloaded.close();
        }
    }

    // -----------------------------------------------------------------------
    // ChatClientFactory — getSessionsRoot()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getSessionsRoot() — returns the directory supplied at construction")
    void getSessionsRoot_returnsConfiguredDirectory() {
        assertThat(factory.getSessionsRoot()).isEqualTo(tempDir);
    }
}
