package org.triplea.game.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.relay.GameRelayEnvelope;
import org.triplea.game.server.relay.RelayControl.BootPayload;
import org.triplea.game.server.relay.RelayControl.JoinPayload;
import org.triplea.game.server.relay.RelayControl.WelcomePayload;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

/**
 * Exercises the multi-game relay against a real relay server with real websocket clients. Each test
 * uses its own game-ids so tests sharing the one static relay do not interfere.
 */
class GameRelayServerTest {
  // Use a random port between 6000 and 7000. It takes time to shut down the relay
  // server, if we re-run this test quickly we'll have issues starting the server
  // up on the same port.
  private static final int port = 6000 + ((int) (Math.random() * 1000));
  private static final URI SERVER_URI = URI.create("ws://localhost:" + port);
  private static final GameRelayServer gameRelayServer = new GameRelayServer(port);
  private static final Gson GSON = new Gson();
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final List<RelayTestClient> clients = new ArrayList<>();

  @BeforeAll
  static void startServer() {
    gameRelayServer.start();
  }

  @AfterAll
  static void stopServer() {
    gameRelayServer.stop();
  }

  @AfterEach
  void closeClients() {
    clients.forEach(client -> client.client.close());
    clients.clear();
  }

  private RelayTestClient joinGame(final String gameId, final String name) {
    final RelayTestClient client = new RelayTestClient(gameId, name);
    clients.add(client);
    return client;
  }

  @Test
  @DisplayName("A broadcast is delivered only within its game, never across game-ids")
  void broadcastStaysWithinGame() {
    final RelayTestClient gameAHost = joinGame("gameA", "a-host");
    final RelayTestClient gameAMember = joinGame("gameA", "a-member");
    final RelayTestClient gameBHost = joinGame("gameB", "b-host");
    final RelayTestClient gameBMember = joinGame("gameB", "b-member");

    gameAHost.broadcastGame("hello-A");
    gameBHost.broadcastGame("hello-B");

    gameAMember.awaitGameMessages(1);
    gameBMember.awaitGameMessages(1);

    // Each game's message reaches only that game's other member.
    assertThat(gameAMember.gamePayloads(), contains("hello-A"));
    assertThat(gameBMember.gamePayloads(), contains("hello-B"));
    // The relay never crosses game-ids.
    assertThat(gameBMember.gamePayloads(), is(List.of("hello-B")));
    assertThat(gameAMember.gamePayloads(), is(List.of("hello-A")));
    // The sender never receives its own broadcast.
    assertThat(gameAHost.gamePayloads(), is(empty()));
    assertThat(gameBHost.gamePayloads(), is(empty()));
  }

  @Test
  @DisplayName("A 'to'-addressed message reaches only the addressed node")
  void directMessageReachesOnlyTarget() {
    final RelayTestClient host = joinGame("directGame", "host");
    final RelayTestClient target = joinGame("directGame", "target");
    final RelayTestClient bystander = joinGame("directGame", "bystander");

    host.sendGameTo(target.nodeId, "for-target");

    target.awaitGameMessages(1);
    assertThat(target.gamePayloads(), contains("for-target"));
    assertThat(bystander.gamePayloads(), is(empty()));
    assertThat(host.gamePayloads(), is(empty()));
  }

  @Test
  @DisplayName("First joiner of a game is host; a later joiner is not")
  void firstJoinerIsHost() {
    final RelayTestClient first = joinGame("hostGame", "first");
    final RelayTestClient second = joinGame("hostGame", "second");

    assertThat(first.host, is(true));
    assertThat(second.host, is(false));
  }

  @Test
  @DisplayName("BOOT from the host removes the target; the target stops receiving game traffic")
  void hostCanBoot() {
    final RelayTestClient host = joinGame("bootGame", "host");
    final RelayTestClient target = joinGame("bootGame", "target");
    // Host sees the member join before booting it.
    host.awaitMessages(GameRelayEnvelope.TYPE_MEMBER_JOINED, 1);

    host.boot(target.nodeId, false);

    Awaitility.await().atMost(TIMEOUT).until(() -> gameRelayServer.memberCount("bootGame") == 1);
    host.awaitMessages(GameRelayEnvelope.TYPE_MEMBER_LEFT, 1);

    // A subsequent broadcast never reaches the booted node.
    host.broadcastGame("after-boot");
    assertThat(target.gamePayloads(), is(empty()));
  }

  @Test
  @DisplayName("BOOT from a non-host is rejected; the target stays connected")
  void nonHostCannotBoot() {
    final RelayTestClient host = joinGame("rejectGame", "host");
    final RelayTestClient nonHost = joinGame("rejectGame", "non-host");
    nonHost.awaitMessages(GameRelayEnvelope.TYPE_WELCOME, 1);

    nonHost.boot(host.nodeId, false);

    // The host is not removed, and remains reachable: a broadcast from the non-host reaches it.
    nonHost.broadcastGame("still-here");
    host.awaitGameMessages(1);
    assertThat(gameRelayServer.memberCount("rejectGame"), is(2));
    assertThat(host.gamePayloads(), contains("still-here"));
  }

  /** A test harness client that connects, joins a game, and records received relay envelopes. */
  private static final class RelayTestClient {
    private final GenericWebSocketClient client;
    private final List<GameRelayEnvelope> received = new CopyOnWriteArrayList<>();
    private final String gameId;
    private String nodeId;
    private boolean host;

    private RelayTestClient(final String gameId, final String name) {
      this.gameId = gameId;
      client = new GenericWebSocketClient(SERVER_URI, Map.of());
      client.addListener(GameRelayEnvelope.TYPE, received::add);
      client.connect();
      join(name);
    }

    private void join(final String name) {
      client.sendMessage(
          new GameRelayEnvelope(
              gameId,
              null,
              null,
              null,
              GameRelayEnvelope.TYPE_JOIN,
              GSON.toJson(new JoinPayload(name))));
      final GameRelayEnvelope welcome = awaitMessages(GameRelayEnvelope.TYPE_WELCOME, 1).get(0);
      final WelcomePayload welcomePayload = GSON.fromJson(welcome.payload(), WelcomePayload.class);
      nodeId = welcomePayload.nodeId();
      host = welcomePayload.host();
    }

    private void broadcastGame(final String payload) {
      client.sendMessage(
          new GameRelayEnvelope(gameId, null, nodeId, null, GameRelayEnvelope.TYPE_GAME, payload));
    }

    private void sendGameTo(final String toNodeId, final String payload) {
      client.sendMessage(
          new GameRelayEnvelope(
              gameId, toNodeId, nodeId, null, GameRelayEnvelope.TYPE_GAME, payload));
    }

    private void boot(final String targetNodeId, final boolean ban) {
      client.sendMessage(
          new GameRelayEnvelope(
              gameId,
              null,
              nodeId,
              null,
              GameRelayEnvelope.TYPE_BOOT,
              GSON.toJson(new BootPayload(targetNodeId, ban))));
    }

    private List<GameRelayEnvelope> messagesOfType(final String type) {
      return received.stream().filter(envelope -> type.equals(envelope.type())).toList();
    }

    private List<String> gamePayloads() {
      return messagesOfType(GameRelayEnvelope.TYPE_GAME).stream()
          .map(GameRelayEnvelope::payload)
          .toList();
    }

    private List<GameRelayEnvelope> awaitMessages(final String type, final int count) {
      Awaitility.await().atMost(TIMEOUT).until(() -> messagesOfType(type).size() >= count);
      return messagesOfType(type);
    }

    private void awaitGameMessages(final int count) {
      awaitMessages(GameRelayEnvelope.TYPE_GAME, count);
    }
  }
}
